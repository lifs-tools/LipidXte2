//
var fsext = require('fs-extra')       //File System - for file manipulation

let exec = require('child_process').execSync
exports.default = {
  processRawfiles: function (inputFolder) {
    console.log('rawfile process called')
    try {
      exec('python3 src/peakStrainer.py ' + inputFolder, {
        cwd: '/app/LipidXplorer'
      }, function (error, stdout, stderr) {
        console.log('stdout: ', stdout)
        console.log('stderr: ', stderr)
        if (error !== null) {
          console.log('exec error: ', error)
        }
      })
    } catch (e) {
      console.log(e)
      return false
    }
    return true
  },
  reorder: function (inputFolder) {
    try {
      exec('python3 src/reorder.py ' + inputFolder, {
        cwd: '/app/LipidXplorer'
      }, function (error, stdout, stderr) {
        console.log('stdout: ', stdout)
        console.log('stderr: ', stderr)
        if (error !== null) {
          console.log('exec error: ', error)
        }
      })
    } catch (e) {
      // console.log(e)
      return false
    }
    return true
  },
  lipidXplorer: function (inputFolder, className) {
    try {
      exec('python3 src/lipidXplorer2Lipidx.py ' + className + ' ' + inputFolder, {
        cwd: '/app/LipidXplorer'
      }, function (error, stdout, stderr) {
        console.log('stdout: ', stdout)
        console.log('stderr: ', stderr)
        if (error !== null) {
          console.log('exec error: ', error)
        }
      })
    } catch (e) {
      // console.log(e)
      return false
    }
    return true
  },
  machinePerformance: function (mergedOut, inputFolder) {
    console.log('Machine performance check')
    var Xvfb = require('xvfb');
    var xvfb = new Xvfb();
    xvfb.startSync();
    console.log('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=valid --merged-file=' + mergedOut + ' --output-path=' + inputFolder)
    try {
      exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=valid --merged-file=' + mergedOut + ' --output-path=' + inputFolder + ' > log.txt', {
        cwd: '/app'
      }, function (error, stdout, stderr) {
        console.log('stdout: ', stdout)
        console.log('stderr: ', stderr)
        if (error !== null) {
          console.log('exec error: ', error)
        }
      })
    } catch (e) {
      console.log(e)
      return false
    } finally {
      xvfb.stopSync();
    }
    return true
  },
  lipidXte: function (baseDir, mergedOut, inputFolder, group1, group2, group3, quantOption = 'Quantity', outputOption = 'All', options = 'RemoveRef SummarizeNCE') {

    try {
      var standard_list = inputFolder + '/standard_list.csv'
      // checking the standard_list.csv
      var Xvfb = require('xvfb');
      var xvfb = new Xvfb();

      if (fsext.existsSync(standard_list)) {
        console.log('User given standard_list.csv used')

        xvfb.startSync();

        exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=' + standard_list + ' --merged-file=' + mergedOut + ' --output-path=' + inputFolder + ' --group1="' + group1 + '" --group2="' + group2 + '" --group3="' + group3 + '" --quant-option=' + quantOption + ' --output-option=' + outputOption + ' ' + options + ' > log.txt', {
          cwd: '/app'
        }, function (error, stdout, stderr) {
          console.log('stdout: ', stdout)
          console.log('stderr: ', stderr)
          if (error !== null) {
            console.log('exec error: ', error)
          }
          xvfb.stopSync();
        })
      } else {
        console.log('System standard_list.csv used')
        xvfb.startSync();
        exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=' + baseDir + '/standard_list.csv --merged-file=' + mergedOut + ' --output-path=' + inputFolder + ' --group1="' + group1 + '" --group2="' + group2 + '" --group3="' + group3 + '" --quant-option=' + quantOption + ' --output-option=' + outputOption + ' ' + options + ' > log.txt', {
          cwd: '/app'
        }, function (error, stdout, stderr) {
          console.log('stdout: ', stdout)
          console.log('stderr: ', stderr)
          if (error !== null) {
            console.log('exec error: ', error)
          }
          xvfb.stopSync();
        })
      }
    } catch (e) {
      // console.log(e)
      return false
    }
    return true
  }
}
