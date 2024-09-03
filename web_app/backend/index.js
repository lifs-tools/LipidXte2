'use strict'

const path = require('path'),
  express = require('express')

const busboy = require('connect-busboy') //middleware for form/file upload
const fsext = require('fs-extra')
const fs = require('fs')
const https = require('https')       //File System - for file manipulation

const app = express(),
  port = process.env.PORT || 443,
  BASE_DIR = path.join(__dirname, '.'),
  DOCS_DIR = path.join(BASE_DIR, 'www'),
  DOWNLOAD_DIR = path.join(BASE_DIR, 'download'),
  SAMPLE_DIR = path.join(BASE_DIR, 'sample')

const sqlite3 = require('sqlite3').verbose()
const db = new sqlite3.Database('db.sqlite')

// Create necessary tables if they do not exist
db.serialize(function () {
  db.run('CREATE TABLE IF NOT EXISTS Batches (ID INTEGER PRIMARY KEY AUTOINCREMENT, Batch TEXT NOT NULL, Title TEXT NOT NULL, Status TEXT)')
  db.run('CREATE TABLE IF NOT EXISTS Tags (ID INTEGER PRIMARY KEY AUTOINCREMENT, Tag TEXT NOT NULL, UNIQUE(Tag))')
  db.run('CREATE TABLE IF NOT EXISTS Batches_Tags (BatchID INTEGER, TagID INTEGER, ' +
    'FOREIGN KEY(BatchID) REFERENCES Batches(ID), FOREIGN KEY(TagID) REFERENCES Tags(ID), UNIQUE(BatchID, TagID))')
  db.run('CREATE TABLE IF NOT EXISTS BatchesFiles (BatchID INTEGER, FilePath TEXT, Status TEXT, ' +
    'FOREIGN KEY(BatchID) REFERENCES Batches(ID), UNIQUE(BatchID, FilePath))')
})

db.close()

app.use('/json', express.static(DOCS_DIR))
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

app.use(express.static('web'))

app.route('/test')
  .get(function (req, res) {
    let folder = req.headers.timestamp.replace(/:/g, '')
    folder = folder.replace(/\./g, '_')
    console.log('Processing ... ' + DOWNLOAD_DIR + path.sep + folder)

    const tags = req.headers.tags.toLowerCase().split(',').map(item => item.trim())

    console.log('Tags: ' + tags)

    // Insert current status
    let db = new sqlite3.Database('db.sqlite')

    db.run('INSERT INTO Batches(Batch, Status) values (?, ?)', [folder, 'Start'], function(err) {
      if (err) {
        return console.error(err.message)
      }
      console.log(this)
      let rowId = this.lastID

      db.serialize(function() {
        // Queries scheduled here will still be serialized.
        let placeholders = tags.map((tag) => '(?)').join(',')
        let sql = 'INSERT OR IGNORE INTO Tags(Tag) VALUES ' + placeholders
        db.run(sql, tags)

        // Insert tags
        let stmt = db.prepare('INSERT OR IGNORE INTO Batches_Tags(BatchID, TagID) VALUES (?, (SELECT ID From Tags WHERE Tag = ?))')

        tags.forEach(tag => {
          stmt.run(rowId, tag)
        })

        stmt.finalize()

        let updateSql = 'UPDATE Batches SET Status = ? WHERE ID = ?'
        db.run(updateSql, ['Raw Files Processing started', rowId])

        db.run(updateSql, ['RawFileProcessed', rowId])

        db.run(updateSql, ['RawFileProcessed', rowId])

        db.run(updateSql, ['Reordered', rowId])

        db.run(updateSql, ['merged.csv', rowId])

        db.run(updateSql, ['LipidXte started', rowId])

        db.run(updateSql, ['LipidXte finished', rowId])
      })

      db.close()
    })

    res.sendStatus(200)
  })

app.route('/process')
  .get(function (req, res) {
    let folder = req.headers.timestamp.replace(/:/g, '')
    folder = folder.replace(/\./g, '_')
    console.log('Processing ... ' + DOWNLOAD_DIR + path.sep + folder)

    const title = req.headers.title

    console.log('Title: ' + title)

    // const tags = req.headers.tags.toLowerCase().split(',').map(item => item.trim())

    // console.log('Tags: ' + tags)

    const className = req.headers.classname

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
    let db = new sqlite3.Database('db.sqlite')

    db.run('INSERT INTO Batches(Batch, Title, Status) values (?, ?, ?)', [folder, title, 'Start'], function(err) {
      if (err) {
        return console.error(err.message)
      }
      //console.log(this)
      let rowId = this.lastID

      db.serialize(function() {
        // Queries scheduled here will still be serialized.
        // let placeholders = tags.map((tag) => '(?)').join(',')
        // let sql = 'INSERT OR IGNORE INTO Tags(Tag) VALUES ' + placeholders
        // db.run(sql, tags)
        //
        // // Insert tags
        // let stmt = db.prepare('INSERT OR IGNORE INTO Batches_Tags(BatchID, TagID) VALUES (?, (SELECT ID From Tags WHERE Tag = ?))')
        //
        // tags.forEach(tag => {
        //   stmt.run(rowId, tag)
        // })
        //
        // stmt.finalize()

        let process = require('./lipidXte').process

        // 1. (Optional) If there are raw files, then convert them to mzXML
        let updateSql = 'UPDATE Batches SET Status = ? WHERE ID = ?'
        db.run(updateSql, ['Raw Files Processing started', rowId])

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
          db.run(updateSql, ['RawFileProcessed', rowId])
        } else {
          db.run(updateSql, ['RawFileProcessed(Failed)', rowId])
        }

        // 1.2 Reorder
        if(success && lawFileProcessed) {
          console.log('Reordering ..')
          success = process.reorder(inputFolder)
          if(success) {
            db.run(updateSql, ['Reordered', rowId])
          } else {
            db.run(updateSql, ['Reordered(Failed)', rowId])
            let err = 'process.reordering failed. Please, contact to authors.'
            console.error(err)
            res.status(500).send(err)
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
          db.run(updateSql, ['merged.csv', rowId])
        } else {
          db.run(updateSql, ['merged.csv(Failed)', rowId])
          let err = 'process.lipidXplorer failed. Please, contact to authors.'
          console.error(err)
          res.status(500).send(err)
          return
        }

        // 4. Machine Performance check
        if (fsext.existsSync(mergedOut) && !fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
          console.log('Machine Performance Check...')
          db.run(updateSql, ['Machine performance check started', rowId])

          success = process.machinePerformance(mergedOut, inputFolder)
        }

        if(success) {
          db.run(updateSql, ['Machine performance check finished', rowId])
        } else {
          db.run(updateSql, ['Machine performance check finished(Failed)', rowId])
          let err = 'process.machinePerformance failed. Please, contact to authors.'
          console.error(err)
          res.status(500).send(err)
          return
        }

        // 5. Verify merged-out.csv for LipidXte process
        if (fsext.existsSync(mergedOut)) {
          console.log('Run LipidXte...')

          db.run(updateSql, ['LipidXte started', rowId])

          // 6. Run LipidXte process
          success = process.lipidXte(BASE_DIR, mergedOut, inputFolder, group1, group2, group3)
          // process.lipidXte(BASE_DIR, mergedOut, inputFolder)

          if(success) {
            db.run(updateSql, ['LipidXte finished', rowId])
          } else {
            db.run(updateSql, ['LipidXte finished(Failed)', rowId])
            let err = 'process.LipidXte failed. Please, contact to authors.'
            console.error(err)
            res.status(500).send(err)
            return
          }

          let outputTsv = 'output_Quantity_All(RemoveRef_SummarizeNCE).tsv'
          if (fsext.existsSync(inputFolder + path.sep + outputTsv)) {
            db.run('INSERT OR REPLACE INTO BatchesFiles(BatchID, FilePath, Status) VALUES (?, ?, ?)', [rowId, outputTsv, 'Done'])
          } else {
            console.error(outputTsv + ' is not generated.')
            db.run('INSERT OR REPLACE INTO BatchesFiles(BatchID, FilePath, Status) VALUES (?, ?, ?)', [rowId, outputTsv, 'Failed'])
          }

          // Show the results
          // Rendering the web interface
          res.status(200).send(outputTsv)
        }
        else
        {
          var err = 'No merged output file is given!'
          db.run(updateSql, ['Errors in LipidXte', rowId])
          console.error(err)
          res.status(500).send(err)
        }
      })

      db.close()
    })
  })

app.route('/upload')
  .post(function (req, res) {
    // console.log(req.headers);
    let folder = req.headers.timestamp.replace(/:/g, '')
    folder = folder.replace(/\./g, '_')
    let fstream
    req.pipe(req.busboy)
    req.busboy.on('file', function (fieldname, file, filename) {
      console.log('Uploading: ' + path.sep + folder + path.sep + filename)

      //Path where image will be uploaded
      // var downloadedFile = DOWNLOAD_DIR + '/' + filename;

      fsext.ensureDirSync(DOWNLOAD_DIR + path.sep + folder)
      fstream = fsext.createWriteStream(DOWNLOAD_DIR + path.sep + folder + path.sep + filename)
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

        res.sendStatus(200)
      })
    })
  })

app.route('/batch')
  .get(function (req, res) {
    let folder = req.headers.timestamp.replace(/:/g, '')
    folder = folder.replace(/\./g, '_')

    console.log('Getting batch ... ' + DOWNLOAD_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    let quantOption = req.headers.quantoption
    let options = req.headers.options
    let outputOption = req.headers.outputoption

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

    if (fsext.existsSync(file)) {
      let inputFolder = DOWNLOAD_DIR + path.sep + folder
      let nce = '[25.0, 30.0, 35.0]'
      // Machine Performance check
      // if (fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
      //
      //   let nce = '[25.0, 30.0, 35.0]'
      //   let data = fsext.readFileSync(inputFolder + path.sep + 'machine_performance.tsv')
      //   nce = data.toString('utf8').slice(0, data.indexOf('\n'))
      //   let checkValue = nce.slice(nce.lastIndexOf('\t') + 1)
      //   if (checkValue === '-Infinity') {
      //       nce = '[25.0, 30.0, 35.0]'
      //   } else {
      //       nce = nce.slice(nce.indexOf('\t') + 1, nce.lastIndexOf('\t'))
      //   }
      //   // console.log(nce)
      // }

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
      let mergedOut = DOWNLOAD_DIR + path.sep + folder + path.sep + 'merged.csv'
      let inputFolder = DOWNLOAD_DIR + path.sep + folder

      let nce = '[25.0, 30.0, 35.0]'
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

      let db = new sqlite3.Database('db.sqlite')

      if (fsext.existsSync(file)) {
        db.run('INSERT OR REPLACE INTO BatchesFiles(BatchID, FilePath, Status) VALUES ((SELECT ID From Batches WHERE Batch = ?), ?, ?)', [folder, outputTsv, 'Done'])

        fsext.readFile(file, (err, data) => {
          if (err) throw err
          let result = []
          result.push(nce)
          result.push(data.toString())
          res.status(200).send(result.join('\n'))
        })
      } else {
        console.error(outputTsv + ' is not generated.')
        db.run('INSERT OR REPLACE INTO BatchesFiles(BatchID, FilePath, Status) VALUES ((SELECT ID From Batches WHERE Batch = ?), ?, ?)', [folder, outputTsv, 'Failed'])

        res.sendStatus(500)
      }

      db.close()
    }
  })

app.route('/download')
  .get(function (req, res) {
    let folder = req.headers.timestamp.replace(/:/g, '')
    folder = folder.replace(/\./g, '_')

    console.log('Getting batch for downloading... ' + DOWNLOAD_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    let quantOption = req.headers.quantoption
    let options = req.headers.options
    let outputOption = req.headers.outputoption

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
        res.end(new Buffer(data, 'binary'))
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

      let db = new sqlite3.Database('db.sqlite')

      db.serialize(function() {
        let dic = {}

        // db.each('SELECT Batches.Batch as Batch, Tags.Tag as Tag FROM Batches, Tags, Batches_Tags WHERE Batches.ID = Batches_Tags.BatchID AND Batches_Tags.TagID = Tags.ID', function (err, row) {
        //   if (err) {
        //     console.error('Error')
        //   } else {
        //     const batch = row.Batch
        //
        //     if (!dic[batch]) {
        //       dic[batch] = []
        //     }
        //     dic[batch].push(row.Tag.trim())
        //   }
        // })

        db.each('SELECT Batch, Title FROM Batches where Status = \'LipidXte finished\'', function (err, row) {
          if (err) {
            console.error('Error')
          } else {
            const batch = row.Batch

            dic[batch] = row.Title
          }
        })

        db.close((err) => {
          if (err) {
            res.status(500).send(err.message)
            return console.error(err.message)
          }
          // console.log(dic)
          // console.log(result)
          let data = {}
          Object.keys(dic).map(function(key, index) {
            data[result[key]] = dic[key]
          })

          console.log(data)
          res.status(200).send(data)
        })
      })
    } else {
      console.error(DOWNLOAD_DIR + ' does not exist. Try to run LipidXte to produce it...')

      res.sendStatus(500)
    }
  })

// Fetching all the classes
app.route('/classes')
  .get(function(req, res) {
    let db = new sqlite3.Database('LipidXteSqlite.db')

    db.serialize(function() {
      let data = []

      db.each('SELECT DISTINCT GRP from MASTER', function (err, row) {
        if (err) {
          console.error('Error')
        } else {
          data.push(row.GRP)
        }
      })

      db.close((err) => {
        if (err) {
          res.status(500).send(err.message)
          return console.error(err.message)
        }
        // console.log(data)
        res.status(200).send(data)
      })
    })
  })

// Fetching all the fractions
// select case when fa_iso = 0 then fa_c || ':' || fa_db else fa_c || ':' || fa_db || ' (' || fa_iso || 'z)' end from fa;
app.route('/fractions')
  .get(function(req, res) {
    let db = new sqlite3.Database('LipidXteSqlite.db')

    db.serialize(function() {
      let data = []

      db.each('SELECT id, mz, CASE WHEN fa_iso = 0 THEN fa_c || \':\' || fa_db ELSE fa_c || \':\' || fa_db || \' (\' || fa_iso || \'z)\' END name FROM fa', function (err, row) {
        if (err) {
          console.error('Error')
        } else {
          data.push(row)
        }
      })

      db.close((err) => {
        if (err) {
          res.status(500).send(err.message)
          return console.error(err.message)
        }
        // console.log(data)
        res.status(200).send(data)
      })
    })
  })

// Fetching SN1's curve
app.route('/sn1/:class/:id')
  .get(function(req, res) {
    const clazz = req.params.class
    const id = req.params.id

    let db = new sqlite3.Database('LipidXteSqlite.db')

    db.serialize(function() {
      let data = []

      db.each(`SELECT CE, INT, CF, CO2INT FROM detail WHERE SN1=1 and REF=${id} AND GRP='${clazz}'`, function (err, row) {
        if (err) {
          console.error('Error')
        } else {
          data.push(row)
        }
      })

      db.close((err) => {
        if (err) {
          res.status(500).send(err.message)
          return console.error(err.message)
        }
        // console.log(data)
        res.status(200).send(data)
      })
    })
  })

// Fetching SN2's curve
app.route('/sn2/:class/:id')
  .get(function(req, res) {
    const clazz = req.params.class
    const id = req.params.id

    let db = new sqlite3.Database('LipidXteSqlite.db')

    db.serialize(function() {
      let data = []

      db.each(`SELECT CE, INT, CF, CO2INT FROM detail WHERE SN2=1 and REF=${id} AND GRP='${clazz}'`, function (err, row) {
        if (err) {
          console.error('Error')
        } else {
          data.push(row)
        }
      })

      db.close((err) => {
        if (err) {
          res.status(500).send(err.message)
          return console.error(err.message)
        }
        // console.log(data)
        res.status(200).send(data)
      })
    })
  })

// Fetching SYM's curve
app.route('/sym/:class/:id')
  .get(function(req, res) {
    const clazz = req.params.class
    const id = req.params.id

    let db = new sqlite3.Database('LipidXteSqlite.db')

    db.serialize(function() {
      let data = []

      db.each(`SELECT CE, INT, CF, CO2INT FROM detail WHERE SYM=1 and REF=${id} AND GRP='${clazz}'`, function (err, row) {
        if (err) {
          console.error('Error')
        } else {
          data.push(row)
        }
      })

      db.close((err) => {
        if (err) {
          res.status(500).send(err.message)
          return console.error(err.message)
        }
        // console.log(data)
        res.status(200).send(data)
      })
    })
  })

app.route('/ultimate')
  .get(function (req, res) {
    let folder = req.headers.timestamp

    console.log('Getting batch ... ' + SAMPLE_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    let quantOption = req.headers.quantoption
    let options = req.headers.options
    let outputOption = req.headers.outputoption

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
      let mergedOut = SAMPLE_DIR + path.sep + folder + path.sep + 'merged.csv'
      let inputFolder = SAMPLE_DIR + path.sep + folder

      let nce = '[25.0, 30.0, 35.0]'
      // Machine Performance check
      if (fsext.existsSync(mergedOut) && !fsext.existsSync(inputFolder + path.sep + 'machine_performance.tsv')) {
        console.log('Machine Performance Check...')
        process.machinePerformance(mergedOut, inputFolder)
      }

      let data = fsext.readFileSync(inputFolder + path.sep + 'machine_performance.tsv')
      const line = data.toString('utf8').slice(0, data.indexOf('\n'))
      const nceString = line.slice(line.indexOf('\t') + 1, line.lastIndexOf('\t'))
      if (nceString !== 'null') {
        nce = nceString
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
    let folder = req.headers.timestamp.replace(/:/g, '')
    folder = folder.replace(/\./g, '_')

    console.log('Getting batch for downloading... ' + SAMPLE_DIR + path.sep + folder)

    // batch id check

    // parsing all the parameters
    let quantOption = req.headers.quantoption
    let options = req.headers.options
    let outputOption = req.headers.outputoption

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
        res.end(new Buffer(data, 'binary'))
      })
    } else {
      console.error(file + ' does not exist. Try to run LipidXte to produce it...')

      res.sendStatus(500)
    }
  })

// Check the access permission
app.route('/checkPass/:pass')
  .get(async function(req, res) {
    const pass = req.params.pass

    const bcrypt = require('bcrypt')
    const result = await bcrypt.compare(pass, '$2b$10$GtwrFMoKrcmbKK/dC/FcOOrkkRoT36cPY0LvMqfvEzOUL5y9MUZUW')

    if (result) {
      res.status(200).send([ true, '' ])
    } else {
      res.status(500).send([ false, 'No Access'])
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
      key: fs.readFileSync('/etc/pki/tls/private/lipidxte.key.2023'),
      cert: fs.readFileSync( '/etc/pki/tls/certs/lipidxte.pem.2024' )
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
