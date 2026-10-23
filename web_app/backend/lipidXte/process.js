// Wrappers around the LipidXplorer scripts and the LipidXte jar.
//
// Every tool is started with an argument vector (execFileSync, no shell), so
// values that originate in request headers (class name, group names, options)
// reach the program as single arguments and can never turn into commands.
const fs = require('fs')
const path = require('path')
const fsext = require('fs-extra')
const { execFileSync } = require('child_process')

const APP_DIR = path.join(__dirname, '..')              // /app inside the image
const LIPIDXPLORER_DIR = path.join(APP_DIR, 'LipidXplorer')
const JAR = 'LipidXte-1.0-SNAPSHOT-jfx.jar'

function runPython (script, args) {
  execFileSync('python3', [script, ...args], { cwd: LIPIDXPLORER_DIR })
}

// Runs the jar under a virtual X server; stdout goes to log.txt next to the jar.
function runJar (args) {
  const Xvfb = require('xvfb')
  const xvfb = new Xvfb()
  xvfb.startSync()
  const log = fs.openSync(path.join(APP_DIR, 'log.txt'), 'w')
  try {
    execFileSync('java', ['-jar', JAR, ...args], { cwd: APP_DIR, stdio: ['ignore', log, 'inherit'] })
  } finally {
    fs.closeSync(log)
    xvfb.stopSync()
  }
}

exports.default = {
  processRawfiles: function (inputFolder) {
    console.log('rawfile process called')
    try {
      runPython('src/peakStrainer.py', [inputFolder])
    } catch (e) {
      console.log(e)
      // sometimes RAW file conversion suddenly stopped after generating mzXML file
      // So the return value is changed to true
      return true
    }
    return true
  },
  reorder: function (inputFolder) {
    try {
      runPython('src/reorder.py', [inputFolder])
    } catch (e) {
      return false
    }
    return true
  },
  lipidXplorer: function (inputFolder, className) {
    try {
      runPython('src/lipidXplorer2Lipidx.py', [className, inputFolder])
    } catch (e) {
      return false
    }
    return true
  },
  machinePerformance: function (mergedOut, inputFolder) {
    console.log('Machine performance check')
    try {
      runJar(['--op=valid', '--merged-file=' + mergedOut, '--output-path=' + inputFolder])
    } catch (e) {
      console.log(e)
      return false
    }
    return true
  },
  lipidXte: function (baseDir, mergedOut, inputFolder, group1, group2, group3, quantOption = 'Quantity', outputOption = 'All', options = 'RemoveRef SummarizeNCE') {
    try {
      const userList = path.join(inputFolder, 'standard_list.csv')
      let standardList
      if (fsext.existsSync(userList)) {
        console.log('User given standard_list.csv used')
        standardList = userList
      } else {
        console.log('System standard_list.csv used')
        standardList = path.join(baseDir, 'standard_list.csv')
      }
      runJar([
        '--op=quant',
        '--standard-list=' + standardList,
        '--merged-file=' + mergedOut,
        '--output-path=' + inputFolder,
        '--group1=' + group1,
        '--group2=' + group2,
        '--group3=' + group3,
        '--quant-option=' + quantOption,
        '--output-option=' + outputOption,
        ...String(options).split(/\s+/).filter(Boolean)
      ])
    } catch (e) {
      return false
    }
    return true
  }
}
