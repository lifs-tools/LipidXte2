// Regression tests for the two SQLite databases the server reads and writes:
// LipidXteSqlite.db (reference curves, read-only) and download/db.sqlite
// (batch status). The expected values come from the shipped databases.
const path = require('path')
const fs = require('fs')
const supertest = require('supertest')
const { DatabaseSync } = require('node:sqlite')
const app = require('../index.js')

const request = supertest(app)
const DOWNLOAD_DIR = path.join(__dirname, '..', 'download')
const STATUS_DB = path.join(DOWNLOAD_DIR, 'db.sqlite')

describe('reference database', () => {
  it('GET /classes lists the lipid classes', async () => {
    const res = await request.get('/classes')
    expect(res.status).toBe(200)
    expect(res.type).toContain('json')
    expect(res.body).toEqual(['PA', 'PC', 'PCO', 'PCO-FANL', 'PCO-M-60', 'PCO-PR', 'PE', 'PEO', 'PG', 'PI', 'PS'])
  })

  it('GET /fractions lists the fatty-acid fractions', async () => {
    const res = await request.get('/fractions')
    expect(res.status).toBe(200)
    expect(res.body.length).toBe(69)
    expect(res.body.map(r => r.name).slice(0, 8)).toEqual(['11:0', '12:0', '13:0', '14:0', '14:1 (7z)', '15:0', '15:1 (7z)', '16:0'])
    expect(Object.keys(res.body[0])).toEqual(['id', 'mz', 'name'])
  })

  it('GET /sn1, /sn2, /sym return the curves of a reference', async () => {
    const sn1 = await request.get('/sn1/PC/1')
    expect(sn1.status).toBe(200)
    expect(sn1.body.length).toBe(61)
    expect(Object.keys(sn1.body[0])).toEqual(['CE', 'INT', 'CF', 'CO2INT'])
    expect(typeof sn1.body[0].CE).toBe('number')
    const sn2 = await request.get('/sn2/PC/1')
    expect(sn2.status).toBe(200)
    expect(sn2.body.length).toBeGreaterThan(0)
    const sym = await request.get('/sym/PC/1')
    expect(sym.status).toBe(200)
    expect(sym.body.length).toBeGreaterThan(0)
  })
})

describe('status database', () => {
  const FOLDER = '2099-12-31T235959_000Z' // sorts last, so it is always among the 10 listed batches
  const STAMP = '2099-12-31T23:59:59.000Z' // how /list renders that folder name

  beforeAll(() => {
    fs.mkdirSync(path.join(DOWNLOAD_DIR, FOLDER), { recursive: true })
    const db = new DatabaseSync(STATUS_DB)
    db.prepare('INSERT INTO Batches(Batch, Title, Status) VALUES (?, ?, ?)').run(FOLDER, 'listed batch', 'LipidXte finished')
    db.prepare('INSERT INTO Batches(Batch, Title, Status) VALUES (?, ?, ?)').run(FOLDER + 'x', 'unfinished batch', 'Start')
    db.close()
  })

  afterAll(() => {
    const db = new DatabaseSync(STATUS_DB)
    db.prepare('DELETE FROM Batches WHERE Batch IN (?, ?)').run(FOLDER, FOLDER + 'x')
    db.close()
    fs.rmSync(path.join(DOWNLOAD_DIR, FOLDER), { recursive: true, force: true })
  })

  it('creates the status tables on start-up', () => {
    const db = new DatabaseSync(STATUS_DB, { readOnly: true })
    const tables = db.prepare("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").all().map(r => r.name)
    db.close()
    expect(tables).toEqual(expect.arrayContaining(['Batches', 'BatchesFiles', 'Batches_Tags', 'Tags']))
  })

  it('GET /list maps finished batches to their titles', async () => {
    const res = await request.get('/list')
    expect(res.status).toBe(200)
    expect(res.body[STAMP]).toBe('listed batch')
    expect(Object.values(res.body)).not.toContain('unfinished batch')
  })
})
