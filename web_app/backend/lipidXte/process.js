//
var fsext = require('fs-extra')       //File System - for file manipulation

let exec = require('child_process').execSync
exports.default = {
  rawfile: function (inputfile) {
    console.log('rawfile process called')
    try {
      // windows server version
      // exec('D:\\Projects\\PeakStrainer\\peakStrainer.py ' + inputfile, function(error, stdout, stderr) {
      // linux + wine version
      exec('/usr/local/bin/wine python D:\\\\Projects\\\\PeakStrainer\\\\peakStrainer.py ' + inputfile, function (error, stdout, stderr) {
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
  reorder: function (inputFolder) {
    try {
      // windows server version
      // exec('D:\\Projects\\PeakStrainer\\reorder.py ' + inputFolder, function(error, stdout, stderr) {
      // linux + wine version
      exec('python /local/moon/drive_d/projects/PeakStrainer/reorder.py ' + inputFolder, function (error, stdout, stderr) {
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
      // windows server version
      // exec('lipidXplorer\\lipidXplorer2Lipidx.py ' + className + ' ' + inputFolder, function (error, stdout, stderr) {
      // linux + wine version
      exec('python lipidXplorer/lipidXplorer2Lipidx.py ' + className + ' ' + inputFolder, function (error, stdout, stderr) {
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

    try {
      exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=valid --merged-file=' + mergedOut + ' --output-path=' + inputFolder + '\\ > log.txt', function (error, stdout, stderr) {
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
  // lipidXte: function (baseDir, mergedOut, inputFolder, quantOption = 'Quantity', outputOption = 'All', options = 'RemoveRef SummarizeNCE') {
  //
  //   var standard_list = inputFolder + '/standard_list.csv'
  //   // checking the standard_list.csv
  //   if (fsext.existsSync(standard_list)) {
  //     console.log('User given standard_list.csv used')
  //
  //     exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=' + standard_list + ' --merged-file=' + mergedOut + ' --output-path=' + inputFolder + '\\ --quant-option=' + quantOption + ' --output-option=' + outputOption + ' ' + options + ' > log.txt', function(error, stdout, stderr) {
  //       console.log('stdout: ', stdout)
  //       console.log('stderr: ', stderr)
  //       if (error !== null) {
  //         console.log('exec error: ', error)
  //       }
  //     })
  //   } else {
  //     console.log('System standard_list.csv used')
  //
  //     exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=' + baseDir + '/standard_list.csv --merged-file=' + mergedOut + ' --output-path=' + inputFolder + '\\ --quant-option=' + quantOption + ' --output-option=' + outputOption + ' ' + options + ' > log.txt', function(error, stdout, stderr) {
  //       console.log('stdout: ', stdout)
  //       console.log('stderr: ', stderr)
  //       if (error !== null) {
  //         console.log('exec error: ', error)
  //       }
  //     })
  //   }
  // }
  lipidXte: function (baseDir, mergedOut, inputFolder, group1, group2, group3, quantOption = 'Quantity', outputOption = 'All', options = 'RemoveRef SummarizeNCE') {

    try {
      var standard_list = inputFolder + '/standard_list.csv'
      // checking the standard_list.csv
      if (fsext.existsSync(standard_list)) {
        console.log('User given standard_list.csv used')

        exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=' + standard_list + ' --merged-file=' + mergedOut + ' --output-path=' + inputFolder + ' --group1="' + group1 + '" --group2="' + group2 + '" --group3="' + group3 + '" --quant-option=' + quantOption + ' --output-option=' + outputOption + ' ' + options + ' > log.txt', function (error, stdout, stderr) {
          console.log('stdout: ', stdout)
          console.log('stderr: ', stderr)
          if (error !== null) {
            console.log('exec error: ', error)
          }
        })
      } else {
        console.log('System standard_list.csv used')

        exec('java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=' + baseDir + '/standard_list.csv --merged-file=' + mergedOut + ' --output-path=' + inputFolder + ' --group1="' + group1 + '" --group2="' + group2 + '" --group3="' + group3 + '" --quant-option=' + quantOption + ' --output-option=' + outputOption + ' ' + options + ' > log.txt', function (error, stdout, stderr) {
          console.log('stdout: ', stdout)
          console.log('stderr: ', stderr)
          if (error !== null) {
            console.log('exec error: ', error)
          }
        })
      }
    } catch (e) {
      // console.log(e)
      return false
    }
    return true
  }
}
