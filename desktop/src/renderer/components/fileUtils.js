export function getFile (oReq) {
  var result = document.createElement('a')
  var contentDisposition = oReq.getResponseHeader('Content-Disposition') || ''
  var filename = contentDisposition.split('filename=')[1]
  filename = filename.replace(/"/g, '')

  var binaryData = []
  binaryData.push(oReq.response)

  result.href = window.URL.createObjectURL(new Blob(binaryData, {type: 'application/zip'}))
  result.target = '_self'
  result.download = filename

  result.click()
}
