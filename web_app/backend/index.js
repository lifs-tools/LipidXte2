'use strict'

const path = require('path'),
  express = require('express')

const busboy = require('connect-busboy') //middleware for form/file upload
const fsext = require('fs-extra')
const fs = require('fs')
const https = require('https')

const app = express(),
  port = process.env.PORT || 443,
  BASE_DIR = path.join(__dirname, '.'),
  DOCS_DIR = path.join(BASE_DIR, 'www'),
  DOWNLOAD_DIR = path.join(BASE_DIR, 'download'),
  SAMPLE_DIR = path.join(BASE_DIR, 'sample')

// --- Request input -----------------------------------------------------------
// Header values end up in file-system paths and on the LipidXte command line,
// so each one is checked against the shape the web UI actually sends before it
// is used. Anything else is answered with 400.
const SAFE_FOLDER = /^[A-Za-z0-9][A-Za-z0-9._-]*$/   // one path segment: never empty, '.', '..' or containing a separator
const SAFE_TOKEN = /^[A-Za-z0-9_-]+$/                 // quantoption, outputoption, classname, curve class
const SAFE_OPTIONS = /^[A-Za-z0-9_]*$/                // options: sorted names joined by '_', may be empty
const SAFE_ID = /^\d+$/                               // reference-curve id

// Batch folder derived from the ISO timestamp header ('2001-02-03T04:05:06.789Z' -> '2001-02-03T040506_789Z').
function batchFolder (req) {
  const stamp = req.headers.timestamp
  if (typeof stamp !== 'string') return null
  const folder = stamp.replace(/:/g, '').replace(/\./g, '_')
  return SAFE_FOLDER.test(folder) ? folder : null
}

// Sample folder named directly by the timestamp header (e.g. 'brain').
function sampleFolder (req) {
  const name = req.headers.timestamp
  return typeof name === 'string' && SAFE_FOLDER.test(name) ? name : null
}

function header (req, name, pattern) {
  const value = req.headers[name]
  return typeof value === 'string' && pattern.test(value) ? value : null
}

// Reads the three result-selection headers; null when any of them is malformed.
function resultOptions (req) {
  const quantOption = header(req, 'quantoption', SAFE_TOKEN)
  const outputOption = header(req, 'outputoption', SAFE_TOKEN)
  const options = header(req, 'options', SAFE_OPTIONS)
  if (quantOption === null || outputOption === null || options === null) return null
  return { quantOption, outputOption, options }
}

// An uploaded file is stored under its own name inside the batch folder, so the
// name must be a bare file name.
function safeFileName (name) {
  if (typeof name !== 'string' || name === '') return null
  const base = path.basename(name)
  if (base !== name || base === '.' || base === '..' || name.includes('\\') || name.includes('\0')) return null
  return base
}

// --- Databases ---------------------------------------------------------------
// Two SQLite files, opened with Node's built-in node:sqlite (synchronous API).
const { DatabaseSync } = require('node:sqlite')
const STATUS_DB = path.join(DOWNLOAD_DIR, 'db.sqlite')        // batch status; created on first start
const REFERENCE_DB = path.join(BASE_DIR, 'LipidXteSqlite.db')   // reference curves; read-only

// A busy timeout lets concurrent processes (pm2 cluster workers, parallel test
// workers) wait for a write lock instead of failing with SQLITE_BUSY at once.
const BUSY_TIMEOUT_MS = 5000

function openStatusDb () {
  return new DatabaseSync(STATUS_DB, { timeout: BUSY_TIMEOUT_MS })
}

function openReferenceDb () {
  return new DatabaseSync(REFERENCE_DB, { readOnly: true, timeout: BUSY_TIMEOUT_MS })
}

// Runs fn(db) on a freshly opened database and always closes it again.
function withDb (open, fn) {
  const db = open()
  try {
    return fn(db)
  } finally {
    db.close()
  }
}

// Answers with the rows of one reference-database query (optionally mapped through pick).
function sendReferenceRows (res, sql, params, pick) {
  let rows
  try {
    rows = withDb(openReferenceDb, db => db.prepare(sql).all(...params))
  } catch (err) {
    console.error(err.message)
    return res.status(500).send(err.message)
  }
  res.status(200).send(pick ? rows.map(pick) : rows)
}

// Create necessary tables if they do not exist
withDb(openStatusDb, db => db.exec(
  'CREATE TABLE IF NOT EXISTS Batches (ID INTEGER PRIMARY KEY AUTOINCREMENT, Batch TEXT NOT NULL, Title TEXT NOT NULL, Status TEXT);' +
  'CREATE TABLE IF NOT EXISTS Tags (ID INTEGER PRIMARY KEY AUTOINCREMENT, Tag TEXT NOT NULL, UNIQUE(Tag));' +
  'CREATE TABLE IF NOT EXISTS Batches_Tags (BatchID INTEGER, TagID INTEGER, ' +
    'FOREIGN KEY(BatchID) REFERENCES Batches(ID), FOREIGN KEY(TagID) REFERENCES Tags(ID), UNIQUE(BatchID, TagID));' +
  'CREATE TABLE IF NOT EXISTS BatchesFiles (BatchID INTEGER, FilePath TEXT, Status TEXT, ' +
    'FOREIGN KEY(BatchID) REFERENCES Batches(ID), UNIQUE(BatchID, FilePath))'
))

app.use(busboy())

// Set the allow cross domain constraints
const allowCrossDomain = function(req, res, next) {
  res.header('Access-Control-Allow-Origin', '*')
  // res.header('Access-Control-Allow-Credentials', 'true');
  res.header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, PATCH, DELETE')
  res.header('Access-Control-Allow-Headers', 'Content-Type, Cache-Control, X-Requested-With, LipidXTe-Header, timestamp, title, files, options, quantoption, outputoption, classname, group1, group2, group3')

  // intercept OPTIONS method
  if (req.method === 'OPTIONS') {
    res.sendStatus(200)
  }
  else {
    next()
  }
}
app.use(allowCrossDomain)

app.use('/json', express.static(DOCS_DIR))
app.use(express.static('web'))

app.route('/test')
  .get(function (req, res) {
    const folder = batchFolder(req)
    if (!folder) return res.status(400).send('Invalid timestamp header')
    console.log('Processing ... ' + DOWNLOAD_DIR + path.sep + folder)

    if (typeof req.headers.tags !== 'string') return res.status(400).send('Invalid tags header')
    const tags = req.headers.tags.toLowerCase().split(',').map(item => item.trim())

    console.log('Tags: ' + tags)

    // Insert current status
    try {
      withDb(openStatusDb, db => {
        const rowId = db.prepare('INSERT INTO Batches(Batch, Status) values (?, ?)').run(folder, 'Start').lastInsertRowid
        console.log('Batch row', rowId)

        const placeholders = tags.map(() => '(?)').join(',')
        db.prepare('INSERT OR IGNORE INTO Tags(Tag) VALUES ' + placeholders).run(...tags)

        // Insert tags
        const link = db.prepare('INSERT OR IGNORE INTO Batches_Tags(BatchID, TagID) VALUES (?, (SELECT ID From Tags WHERE Tag = ?))')
        tags.forEach(tag => link.run(rowId, tag))

        const setStatus = db.prepare('UPDATE Batches SET Status = ? WHERE ID = ?')
        for (const status of ['Raw Files Processing started', 'RawFileProcessed', 'RawFileProcessed', 'Reordered', 'merged.csv', 'LipidXte started', 'LipidXte finished']) {
          setStatus.run(status, rowId)
        }
      })
    } catch (err) {
      console.error(err.message)
    }

    res.sendStatus(200)
  })

app.route('/process')
  .get(function (req, res) {
    const folder = batchFolder(req)
    if (!folder) return res.status(400).send('Invalid timestamp header')
    console.log('Processing ... ' + DOWNLOAD_DIR + path.sep + folder)

    const title = req.headers.title
    if (typeof title !== 'string') return res.status(400).send('Invalid title header')

    console.log('Title: ' + title)

    // const tags = req.headers.tags.toLowerCase().split(',').map(item => item.trim())

    // console.log('Tags: ' + tags)

    const className = header(req, 'classname', SAFE_TOKEN)
    if (className === null) return res.status(400).send('Invalid classname header')

    console.log('Class: ' + className)

    const group1 = req.headers.group1
    const group2 = req.headers.group2
    const group3 = req.headers.group3

    console.log('Group-1:', group1)
    console.log('Group-2:', group2)
    console.log('Group-3:', group3)

    const groups = {group1, group2, group3}
    let groupFile = DOWNLOAD_DIR + path.sep + folder + path.sep + 'groups.json'

    fsext.writeFile(groupFile, JSON.stringify(groups), 'utf8', function (err) {
      if (err) {
        return console.error(err)
      }
    })

    // Insert current status
    const db = openStatusDb()
    let rowId
    try {
      rowId = db.prepare('INSERT INTO Batches(Batch, Title, Status) values (?, ?, ?)').run(folder, title, 'Start').lastInsertRowid
    } catch (err) {
      db.close()
      console.error(err.message)
      return res.status(500).send(err.message)
    }
    const setStatus = db.prepare('UPDATE Batches SET Status = ? WHERE ID = ?')
    const setFile = db.prepare('INSERT OR REPLACE INTO BatchesFiles(BatchID, FilePath, Status) VALUES (?, ?, ?)')

    try {
        let process = require('./lipidXte').process

        // 1. (Optional) If there are raw files, then convert them to mzXML
        setStatus.run('Raw Files Processing started', rowId)

        let lawFileProcessed = false
        let inputFolder = DOWNLOAD_DIR + path.sep + folder
        let success = true
        let rawFiles = []
        fsext.readdirSync(DOWNLOAD_DIR + path.sep + folder).forEach( function (file)
        {
          if (file.endsWith('.raw') || file.endsWith('.RAW')) {
            rawFiles.push(file)
          }
        })

        if (rawFiles.length > 0) {
          console.log('RAW file detected. Run peakStainer')
          success = process.processRawfiles(inputFolder + path.sep)
          lawFileProcessed = true
        }

        if(success) {
          setStatus.run('RawFileProcessed', rowId)
        } else {
          setStatus.run('RawFileProcessed(Failed)', rowId)
        }

        // 1.2 Reorder
        if(success && lawFileProcessed) {
          console.log('Reordering ..')
          success = process.reorder(inputFolder)
          if(success) {
            setStatus.run('Reordered', rowId)
          } else {
            setStatus.run('Reordered(Failed)', rowId)
            let err = 'process.reordering failed. Please, contact to authors.'
            console.error(err)
            res.status(500).send(err)
            return
          }
        }

        // 2. Run lipidXplorer batch process
        // D:\Projects\LipidXteServer\lipidXplorer\lipidXplorer2Lipidx.py
        let mergedOut = DOWNLOAD_DIR + path.sep + folder + path.sep + 'merged.csv'
        if (!fsext.existsSync(mergedOut)) {
          success = process.lipidXplorer(inputFolder, className)
        } else {
          console.log('merged.csv is provided.')
        }

        if(success) {
          setStatus.run('merged.csv', rowId)
        } else {
          setStatus.run('merged.csv(Failed)', rowId)
          let err = 'process.lipidXplorer failed. Please, contact to authors.'
          console.error(err)
          res.status(500).send(err)
          return
        }

        // 4. Machine Performance check
        // if (fsext.existsSync(mergedOut) && !fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
        //   // console.log('Machine Performance Check...')
        //   setStatus.run('Machine performance check started', rowId)
        //
        //   success = process.machinePerformance(mergedOut, inputFolder)
        // }
        //
        // if(success) {
        //   setStatus.run('Machine performance check finished', rowId)
        // } else {
        //   setStatus.run('Machine performance check finished(Failed)', rowId)
        //   let err = 'process.machinePerformance failed. Please, contact to authors.'
        //   console.error(err)
        //   res.status(500).send(err)
        //   return
        // }

        // 4.1 Class matching test
        if (fsext.existsSync(mergedOut)) {
          // console.log('Machine Performance Check...')
          setStatus.run('Lipid Class check started', rowId)


          let data = fsext.readFileSync(mergedOut, 'utf-8')

          // Checking class in the merged file
          // data = data.split('\n').filter(c => c.startsWith('###')).map(line => line.split(',')[1])
          // data = data.filter(c => c.endsWith('FAS')).map(line => line.replaceAll('FAS', ''))
          // let clazz = data && data.length > 0 ? data[0] : ''

          // Checking the number of items
          data = data.split('\n')

          success = (data.length > 100)
        }

        if (success) {
          setStatus.run('Lipid Class check finished', rowId)
        } else {
          setStatus.run('Lipid Class check finished(Failed)', rowId)
          let err = 'process.lipidClassCheck failed. Please, contact to authors.'
          console.error(err)
          res.status(500).send(err)
          return
        }

        // 5. Verify merged-out.csv for LipidXte process
        if (fsext.existsSync(mergedOut)) {
          console.log('Run LipidXte...')

          setStatus.run('LipidXte started', rowId)

          // 6. Run LipidXte process
          success = process.lipidXte(BASE_DIR, mergedOut, inputFolder, group1, group2, group3)
          // process.lipidXte(BASE_DIR, mergedOut, inputFolder)

          if(success) {
            setStatus.run('LipidXte finished', rowId)
          } else {
            setStatus.run('LipidXte finished(Failed)', rowId)
            let err = 'process.LipidXte failed. Please, contact to authors.'
            console.error(err)
            res.status(500).send(err)
            return
          }

          let outputTsv = 'output_Quantity_All(RemoveRef_SummarizeNCE).tsv'
          if (fsext.existsSync(inputFolder + path.sep + outputTsv)) {
            setFile.run(rowId, outputTsv, 'Done')
          } else {
            console.error(outputTsv + ' is not generated.')
            setFile.run(rowId, outputTsv, 'Failed')
          }

          // Show the results
          // Rendering the web interface
          res.status(200).send(outputTsv)
        }
        else
        {
          var err = 'No merged output file is given!'
          setStatus.run('Errors in LipidXte', rowId)
          console.error(err)
          res.status(500).send(err)
        }
    } finally {
      db.close()
    }
  })

app.route('/upload')
  .post(function (req, res) {
    // console.log(req.headers);
    const folder = batchFolder(req)
    if (!folder) return res.status(400).send('Invalid timestamp header')
    let fstream
    req.pipe(req.busboy)
    req.busboy.on('file', function (fieldname, file, fileObject) {
      const filename = safeFileName(fileObject.filename)
      if (!filename) {
        file.resume() // discard the stream
        if (!res.headersSent) res.status(400).send('Invalid file name')
        return
      }
      console.log('Uploading: ' + path.sep + folder + path.sep + filename)

      const batchDir = path.join(DOWNLOAD_DIR, folder)
      fsext.ensureDirSync(batchDir)
      fstream = fsext.createWriteStream(path.join(batchDir, filename))
      file.pipe(fstream)
      fstream.on('close', function () {
        console.log('Upload Finished of ' + path.sep + folder + path.sep + filename)

        // var exec = require('child_process').exec;
        // exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=' + BASE_DIR + '/standard_list.csv --merged-file=' + downloadedFile + ' --output-file=' + DOWNLOAD_DIR + '/PC_-output.tsv > t', function(error, stdout, stderr) {
        //   console.log('stdout: ', stdout);
        //   console.log('stderr: ', stderr);
        //   if (error !== null) {
        //     console.log('exec error: ', error);
        //   }
        // });

        if (!res.headersSent) res.sendStatus(200)
      })
    })
  })

app.route('/batch')
  .get(function (req, res) {
    const folder = batchFolder(req)
    if (!folder) return res.status(400).send('Invalid timestamp header')

    console.log('Getting batch ... ' + DOWNLOAD_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    const selected = resultOptions(req)
    if (!selected) return res.status(400).send('Invalid quantoption, outputoption or options header')
    const { quantOption, outputOption, options } = selected

    console.log(quantOption)
    console.log(options)
    console.log(outputOption)

    let groupFile = DOWNLOAD_DIR + path.sep + folder + path.sep + 'groups.json'

    const contents = fsext.readFileSync(groupFile, 'utf8')
    let groups = JSON.parse(contents)

    // 'RemoveRef SummarizeNCE GroupOnly'
    let outputTsv = 'output_' + quantOption + '_' + outputOption + '(' + options + ').tsv'

    // if(options !== '') {
    //   outputTsv = 'output_' + quantOption + '_' + outputOption + '(RemoveRef_SummarizeNCE_NoCorrection).tsv'
    //   options = 'RemoveRef_SummarizeNCE_NoCorrection'
    // }
    // else {
    //   options = 'RemoveRef_SummarizeNCE'
    // }

    let file = DOWNLOAD_DIR + path.sep + folder + path.sep + outputTsv
    let mergedOut = DOWNLOAD_DIR + path.sep + folder + path.sep + 'merged.csv'

    if (fsext.existsSync(file)) {
      let inputFolder = DOWNLOAD_DIR + path.sep + folder
      let nce = '[25.0, 30.0, 35.0]'

      // Machine Performance check
      if (fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
        let data = fsext.readFileSync(inputFolder + path.sep + 'machine_performance.tsv')
        nce = data.toString('utf8').slice(0, data.indexOf('\n'))
        let checkValue = nce.slice(nce.lastIndexOf('\t') + 1)
        if (checkValue === '-Infinity') {
            nce = '[25.0, 30.0, 35.0]'
        } else {
            nce = nce.slice(nce.indexOf('\t') + 1, nce.lastIndexOf('\t'))
        }
        // console.log(nce)
      } else {
        if (fsext.existsSync(mergedOut)) {
          let data = fsext.readFileSync(mergedOut, 'utf-8')

          nce = data.split('\n').filter(c => c.startsWith('#NCE')).map(line => parseFloat(line.split(',')[1]))
          nce = JSON.stringify(nce).replaceAll(',', ', ')
        }
      }

      fsext.readFile(file, (err, data) => {
        if (err) throw err
        let result = []
        result.push(nce)
        result.push(data.toString())
        res.status(200).send(result.join('\n'))
      })
    } else {
      console.error(file + ' does not exist. Try to run LipidXte to produce it...')

      // try to run LipidXte
      let process = require('./lipidXte').process
      let inputFolder = DOWNLOAD_DIR + path.sep + folder

      let nce = '[25.0, 30.0, 35.0]'
      // Extract NCE from merged.csv

      if (fsext.existsSync(mergedOut)) {
        let data = fsext.readFileSync(mergedOut, 'utf-8')

        nce = data.split('\n').filter(c => c.startsWith('#NCE')).map(line => parseFloat(line.split(',')[1]))
        nce = JSON.stringify(nce).replaceAll(',', ', ')
      }

      // Machine Performance check
      // if (fsext.existsSync(mergedOut) && !fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
      //   console.log('Machine Performance Check...')
      //   process.machinePerformance(mergedOut, inputFolder)
      // }
      //
      // let data = fsext.readFileSync(inputFolder + path.sep + 'machine_performance.tsv')
      // nce = data.toString('utf8').slice(0, data.indexOf('\n'))
      // nce = nce.slice(nce.indexOf('\t') + 1, nce.lastIndexOf('\t'))
      // console.log(nce)

      // 4. Run LipidXte process
      process.lipidXte(BASE_DIR, mergedOut, inputFolder, groups.group1, groups.group2, groups.group3, quantOption, outputOption, options.replace(/_/g, ' '))
      // process.lipidXte(BASE_DIR, mergedOut, inputFolder, quantOption, outputOption, options.replace(/_/g, ' '))

      const generated = fsext.existsSync(file)
      withDb(openStatusDb, db => db
        .prepare('INSERT OR REPLACE INTO BatchesFiles(BatchID, FilePath, Status) VALUES ((SELECT ID From Batches WHERE Batch = ?), ?, ?)')
        .run(folder, outputTsv, generated ? 'Done' : 'Failed'))

      if (generated) {
        fsext.readFile(file, (err, data) => {
          if (err) throw err
          let result = []
          result.push(nce)
          result.push(data.toString())
          res.status(200).send(result.join('\n'))
        })
      } else {
        console.error(outputTsv + ' is not generated.')
        res.sendStatus(500)
      }
    }
  })

app.route('/download')
  .get(function (req, res) {
    const folder = batchFolder(req)
    if (!folder) return res.status(400).send('Invalid timestamp header')

    console.log('Getting batch for downloading... ' + DOWNLOAD_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    const selected = resultOptions(req)
    if (!selected) return res.status(400).send('Invalid quantoption, outputoption or options header')
    const { quantOption, outputOption, options } = selected

    // 'RemoveRef SummarizeNCE GroupOnly'
    let outputTsv = 'output_' + quantOption + '_' + outputOption + '(' + options + ').tsv'

    let file = DOWNLOAD_DIR + path.sep + folder + path.sep + outputTsv

    if (fsext.existsSync(file)) {
      fsext.readFile(file, 'utf-8', (err, data) => {
        if (err) throw err

        data = data.replace(/\t/g, ',')
        outputTsv = outputTsv.replace('.tsv', '.csv')
        res.writeHead(200, {
          'Content-Type': 'text/csv',
          'Content-disposition': 'attachment;filename="' + outputTsv + '"',
          'Content-Length': data.length
        })
        res.end(Buffer.from(data, 'binary'))
      })
    } else {
      console.error(file + ' does not exist. Try to run LipidXte to produce it...')

      res.sendStatus(500)
    }
  })

app.route('/list')
  .get(function (req, res) {
    // let folder = req.headers.timestamp.replace(/:/g, '')
    // folder = folder.replace(/\./g, '_')

    console.log('List latest 10 batches ' + DOWNLOAD_DIR)

    if (fsext.existsSync(DOWNLOAD_DIR)) {
      let files = fsext.readdirSync(DOWNLOAD_DIR)
      files = files.filter(file => file.startsWith('20')).sort()

      // console.log(files)

      const fiveRecentFiles = files.slice(-10)

      const result = {}

      fiveRecentFiles.forEach(c => {
        let folder = c.replace(/_/g, '.')
        let colon = ':'
        let position = 13
        folder = [folder.slice(0, position), colon, folder.slice(position)].join('')
        position = 16
        folder = [folder.slice(0, position), colon, folder.slice(position)].join('')
        result[c] = folder
      })
      // console.log(result)

      let rows
      try {
        rows = withDb(openStatusDb, db => db.prepare('SELECT Batch, Title FROM Batches WHERE Status = \'LipidXte finished\'').all())
      } catch (err) {
        console.error(err.message)
        return res.status(500).send(err.message)
      }

      const data = {}
      for (const row of rows) {
        data[result[row.Batch]] = row.Title
      }

      console.log(data)
      res.status(200).send(data)
    } else {
      console.error(DOWNLOAD_DIR + ' does not exist. Try to run LipidXte to produce it...')

      res.sendStatus(500)
    }
  })

// Fetching all the classes
app.route('/classes')
  .get(function(req, res) {
    sendReferenceRows(res, 'SELECT DISTINCT GRP from MASTER', [], row => row.GRP)
  })

// Fetching all the fractions
// select case when fa_iso = 0 then fa_c || ':' || fa_db else fa_c || ':' || fa_db || ' (' || fa_iso || 'z)' end from fa;
app.route('/fractions')
  .get(function(req, res) {
    sendReferenceRows(res, 'SELECT id, mz, CASE WHEN fa_iso = 0 THEN fa_c || \':\' || fa_db ELSE fa_c || \':\' || fa_db || \' (\' || fa_iso || \'z)\' END name FROM fa', [])
  })

// Fetching SN1's curve
app.route('/sn1/:class/:id')
  .get(function(req, res) {
    const clazz = req.params.class
    const id = req.params.id
    if (!SAFE_TOKEN.test(clazz) || !SAFE_ID.test(id)) return res.status(400).send('Invalid class or id')

    sendReferenceRows(res, 'SELECT CE, INT, CF, CO2INT FROM detail WHERE SN1=1 and REF=? AND GRP=?', [Number(id), clazz])
  })

// Fetching SN2's curve
app.route('/sn2/:class/:id')
  .get(function(req, res) {
    const clazz = req.params.class
    const id = req.params.id
    if (!SAFE_TOKEN.test(clazz) || !SAFE_ID.test(id)) return res.status(400).send('Invalid class or id')

    sendReferenceRows(res, 'SELECT CE, INT, CF, CO2INT FROM detail WHERE SN2=1 and REF=? AND GRP=?', [Number(id), clazz])
  })

// Fetching SYM's curve
app.route('/sym/:class/:id')
  .get(function(req, res) {
    const clazz = req.params.class
    const id = req.params.id
    if (!SAFE_TOKEN.test(clazz) || !SAFE_ID.test(id)) return res.status(400).send('Invalid class or id')

    sendReferenceRows(res, 'SELECT CE, INT, CF, CO2INT FROM detail WHERE SYM=1 and REF=? AND GRP=?', [Number(id), clazz])
  })

app.route('/ultimate')
  .get(function (req, res) {
    const folder = sampleFolder(req)
    if (!folder) return res.status(400).send('Invalid timestamp header')

    console.log('Getting batch ... ' + SAMPLE_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    const selected = resultOptions(req)
    if (!selected) return res.status(400).send('Invalid quantoption, outputoption or options header')
    const { quantOption, outputOption, options } = selected

    console.log(quantOption)
    console.log(options)
    console.log(outputOption)

    let groupFile = SAMPLE_DIR + path.sep + folder + path.sep + 'groups.json'

    const contents = fsext.readFileSync(groupFile, 'utf8')
    let groups = JSON.parse(contents)

    // 'RemoveRef SummarizeNCE GroupOnly'
    let outputTsv = 'output_' + quantOption + '_' + outputOption + '(' + options + ').tsv'
    let mergedOut = SAMPLE_DIR + path.sep + folder + path.sep + 'merged.csv'
    let file = SAMPLE_DIR + path.sep + folder + path.sep + outputTsv
    let standard_list_file = SAMPLE_DIR + path.sep + folder + path.sep + 'standard_list.csv'
    const standard_list_exists = fsext.existsSync(standard_list_file)
    console.log('Standard list file exists: ', standard_list_exists)

    if (fsext.existsSync(file)) {
      let inputFolder = SAMPLE_DIR + path.sep + folder
      let nce = '[25.0, 30.0, 35.0]'
      // Machine Performance check
      if (fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
        let data = fsext.readFileSync(inputFolder + path.sep + 'machine_performance.tsv')
        nce = data.toString('utf8').slice(0, data.indexOf('\n'))
        let checkValue = nce.slice(nce.lastIndexOf('\t') + 1)
        if (checkValue === '-Infinity' || checkValue === 'null') {
          nce = '[25.0, 30.0, 35.0]'
        } else {
          nce = nce.slice(nce.indexOf('\t') + 1, nce.lastIndexOf('\t'))
        }
        // console.log(nce)
      } else {
        if (fsext.existsSync(mergedOut)) {
          let data = fsext.readFileSync(mergedOut, 'utf-8')

          nce = data.split('\n').filter(c => c.startsWith('#NCE')).map(line => parseFloat(line.split(',')[1]))
          nce = JSON.stringify(nce).replaceAll(',', ', ')
        }
      }

      fsext.readFile(file, (err, data) => {
        if (err) throw err
        let result = []
        result.push(nce)
        result.push(data.toString())
        result.push(standard_list_exists)
        res.status(200).json(result)
      })
    } else {
      console.error(file + ' does not exist. Try to run LipidXte to produce it...')

      // try to run LipidXte
      let process = require('./lipidXte').process
      let inputFolder = SAMPLE_DIR + path.sep + folder

      let nce = '[25.0, 30.0, 35.0]'
      // Machine Performance check
      // if (fsext.existsSync(mergedOut) && !fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
      //   console.log('Machine Performance Check...')
      //   process.machinePerformance(mergedOut, inputFolder)
      // }
      //
      // let data = fsext.readFileSync(inputFolder + path.sep + 'machine_performance.tsv')
      // const line = data.toString('utf8').slice(0, data.indexOf('\n'))
      // const nceString = line.slice(line.indexOf('\t') + 1, line.lastIndexOf('\t'))
      // if (nceString !== 'null') {
      //   nce = nceString
      // }
      if (fsext.existsSync(mergedOut)) {
        let data = fsext.readFileSync(mergedOut, 'utf-8')

        nce = data.split('\n').filter(c => c.startsWith('#NCE')).map(line => parseFloat(line.split(',')[1]))
        nce = JSON.stringify(nce).replaceAll(',', ', ')
      }

      console.log(nce)

      // 4. Run LipidXte process
      process.lipidXte(BASE_DIR, mergedOut, inputFolder, groups.group1, groups.group2, groups.group3, quantOption, outputOption, options.replace(/_/g, ' '))
      // process.lipidXte(BASE_DIR, mergedOut, inputFolder, quantOption, outputOption, options.replace(/_/g, ' '))

      if (fsext.existsSync(file)) {
        fsext.readFile(file, (err, data) => {
          if (err) throw err
          let result = []
          result.push(nce)
          result.push(data.toString())
          result.push(standard_list_exists)
          res.status(200).json(result)
        })
      } else {
        console.error(outputTsv + ' is not generated.')

        res.sendStatus(500)
      }
    }
  })

// Download ultimate data
app.route('/download-ultimate')
  .get(function (req, res) {
    const folder = batchFolder(req)
    if (!folder) return res.status(400).send('Invalid timestamp header')

    console.log('Getting batch for downloading... ' + SAMPLE_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    const selected = resultOptions(req)
    if (!selected) return res.status(400).send('Invalid quantoption, outputoption or options header')
    const { quantOption, outputOption, options } = selected

    // 'RemoveRef SummarizeNCE GroupOnly'
    let outputTsv = 'output_' + quantOption + '_' + outputOption + '(' + options + ').tsv'

    let file = SAMPLE_DIR + path.sep + folder + path.sep + outputTsv

    if (fsext.existsSync(file)) {
      fsext.readFile(file, 'utf-8', (err, data) => {
        if (err) throw err

        data = data.replace(/\t/g, ',')
        outputTsv = outputTsv.replace('.tsv', '.csv')
        res.writeHead(200, {
          'Content-Type': 'text/csv',
          'Content-disposition': 'attachment;filename="' + outputTsv + '"',
          'Content-Length': data.length
        })
        res.end(Buffer.from(data, 'binary'))
      })
    } else {
      console.error(file + ' does not exist. Try to run LipidXte to produce it...')

      res.sendStatus(500)
    }
  })

// Start server
if (require.main === module) {
  console.log('Server listening on port %s', port)

  if (process.env.NODE_ENV === 'development') {
    // Run the server in development mode
    app.listen( port )
  } else {
    // Run the server in production mode
    const fs = require('fs')

    let options = {
      key: fs.readFileSync('/app/certs/lipidxte.key'),
      cert: fs.readFileSync('/app/certs/lipidxte.pem')
    }

    let https = require('https')
    https.createServer(options, app).listen(port)

    let app1 = express()

    app1.get('*', function(req, res) {
      return res.redirect('https://' + req.headers.host + req.url)
    }).listen(80)
  }
}

module.exports = app
