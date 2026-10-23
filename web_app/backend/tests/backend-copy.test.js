// web_app/backend/ (the original stand-alone LipidXteServer checkout) carries a
// copy of the server source that ships from docker-build/. This test fails as
// soon as the two copies differ, so the mirror cannot drift again: edit
// docker-build/ and copy the changed files over (or the other way round).
const fs = require('fs')
const path = require('path')

function repoRoot (from) {
  let dir = from
  while (!fs.existsSync(path.join(dir, 'Dockerfile'))) {
    const up = path.dirname(dir)
    if (up === dir) return null
    dir = up
  }
  return dir
}

const ROOT = repoRoot(__dirname)
const SHIPPED = ROOT && path.join(ROOT, 'docker-build')
const MIRROR = ROOT && path.join(ROOT, 'web_app', 'backend')
const bothPresent = ROOT && fs.existsSync(SHIPPED) && fs.existsSync(MIRROR)

const FILES = [
  'index.js',
  'lipidXte/index.js',
  'lipidXte/process.js',
  'package.json',
  '.yarnrc.yml',
  'yarn.lock',
  'standard_list.csv',
  'LipidXte-1.0-SNAPSHOT-jfx.jar',
  'LipidXteSqlite.db'
]

// Inside the Docker image there is no repository checkout, so there is nothing to compare.
const maybe = bothPresent ? describe : describe.skip

maybe('web_app/backend mirrors docker-build', () => {
  const tests = fs.readdirSync(path.join(SHIPPED, 'tests')).filter(f => f.endsWith('.test.js')).map(f => 'tests/' + f)
  const www = fs.readdirSync(path.join(SHIPPED, 'www')).filter(f => f.endsWith('.json')).map(f => 'www/' + f)

  it.each([...FILES, ...tests, ...www])('%s is identical in both directories', (file) => {
    const shipped = fs.readFileSync(path.join(SHIPPED, file))
    const mirrorPath = path.join(MIRROR, file)
    expect(fs.existsSync(mirrorPath)).toBe(true)
    expect(shipped.equals(fs.readFileSync(mirrorPath))).toBe(true)
  })

  it('has no stale test files of its own', () => {
    const extra = fs.readdirSync(path.join(MIRROR, 'tests')).filter(f => f.endsWith('.test.js') || f.endsWith('.js'))
      .filter(f => !fs.existsSync(path.join(SHIPPED, 'tests', f)))
    expect(extra).toEqual([])
  })
})
