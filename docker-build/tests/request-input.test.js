// Request headers and upload names end up in file-system paths and in the
// LipidXte command line. These tests pin down that anything outside the shape
// the web UI sends is rejected with 400 before it reaches either.
const path = require('path')
const fs = require('fs')
const supertest = require('supertest')
const app = require('../index.js')

const request = supertest(app)
const DOWNLOAD_DIR = path.join(__dirname, '..', 'download')
const STAMP = '2001-02-03T04:05:06.789Z' // what LandingPage.vue sends
const FOLDER = '2001-02-03T040506_789Z' // what the server derives from it
const MARKER = 'lipidxte-traversal-marker-' + process.pid + '.txt'

afterAll(() => {
  fs.rmSync(path.join(DOWNLOAD_DIR, FOLDER), { recursive: true, force: true })
  // If a traversal ever succeeds the marker lands three levels above the batch dir.
  fs.rmSync(path.resolve(DOWNLOAD_DIR, FOLDER, '..', '..', '..', MARKER), { force: true })
})

describe('/upload', () => {
  it('stores a file under download/<timestamp folder>/<file name>', async () => {
    const res = await request.post('/upload').set('timestamp', STAMP)
      .attach('file', Buffer.from('hello'), 'sample 1.raw')
    expect(res.status).toBe(200)
    expect(fs.readFileSync(path.join(DOWNLOAD_DIR, FOLDER, 'sample 1.raw'), 'utf8')).toBe('hello')
  })

  it('never writes outside the batch folder when the file name tries to climb', async () => {
    // busboy >= 1.0 already reduces the name to its basename, so the request is
    // either refused or stored flat inside the batch folder; it must never escape.
    const res = await request.post('/upload').set('timestamp', STAMP)
      .attach('file', Buffer.from('x'), '../../../' + MARKER)
    expect([200, 400]).toContain(res.status)
    expect(fs.existsSync(path.resolve(DOWNLOAD_DIR, FOLDER, '..', '..', '..', MARKER))).toBe(false)
    if (res.status === 200) {
      expect(fs.existsSync(path.join(DOWNLOAD_DIR, FOLDER, MARKER))).toBe(true)
    }
  })

  it('rejects a timestamp containing a path separator', async () => {
    const res = await request.post('/upload').set('timestamp', 'a/b')
      .attach('file', Buffer.from('x'), 'f.raw')
    expect(res.status).toBe(400)
    expect(fs.existsSync(path.join(DOWNLOAD_DIR, 'a'))).toBe(false)
  })

  it('rejects a missing timestamp', async () => {
    const res = await request.post('/upload').attach('file', Buffer.from('x'), 'f.raw')
    expect(res.status).toBe(400)
  })
})

describe('header values that reach paths and the command line', () => {
  it('/ultimate rejects a sample folder with ..', async () => {
    const res = await request.get('/ultimate').set('timestamp', '../download')
      .set('quantoption', 'Quantity').set('outputoption', 'All').set('options', 'RemoveRef')
    expect(res.status).toBe(400)
  })

  it('/batch rejects shell metacharacters in options', async () => {
    const res = await request.get('/batch').set('timestamp', STAMP)
      .set('quantoption', 'Quantity').set('outputoption', 'All').set('options', 'RemoveRef; touch /tmp/pwned')
    expect(res.status).toBe(400)
  })

  it('/download rejects a quantoption that is not a plain token', async () => {
    const res = await request.get('/download').set('timestamp', STAMP)
      .set('quantoption', '../x').set('outputoption', 'All').set('options', '')
    expect(res.status).toBe(400)
  })

  it('/process rejects a classname that is not a plain token', async () => {
    const res = await request.get('/process').set('timestamp', STAMP).set('title', 't')
      .set('classname', 'PC $(id)').set('group1', 'a').set('group2', 'b').set('group3', 'c')
    expect(res.status).toBe(400)
  })

  it('/download-ultimate still answers 500 (not found) for a well-formed but unknown batch', async () => {
    const res = await request.get('/download-ultimate').set('timestamp', 'no-such-sample')
      .set('quantoption', 'Quantity').set('outputoption', 'All').set('options', 'RemoveRef')
    expect(res.status).toBe(500)
  })
})

describe('reference-curve routes', () => {
  it('/sn1/:class/:id returns rows for a real class', async () => {
    const res = await request.get('/sn1/PC/1')
    expect(res.status).toBe(200)
    expect(res.body.length).toBeGreaterThan(0)
    expect(Object.keys(res.body[0])).toEqual(['CE', 'INT', 'CF', 'CO2INT'])
  })

  it('/sn1/:class/:id rejects an id or class that is not a plain value', async () => {
    const injectedId = await request.get('/sn1/PC/' + encodeURIComponent('1 OR 1=1'))
    expect(injectedId.status).toBe(400)
    const injectedClass = await request.get('/sn2/' + encodeURIComponent("PC' OR '1'='1") + '/1')
    expect(injectedClass.status).toBe(400)
  })
})

describe('removed routes', () => {
  it('/checkPass/:pass no longer exists (the password used to travel in the URL)', async () => {
    const res = await request.get('/checkPass/anything')
    expect(res.status).toBe(404)
  })
})
