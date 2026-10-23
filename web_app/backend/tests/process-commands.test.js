// The LipidXte / LipidXplorer tools are started with an argument vector, never
// through a shell, so header-derived values such as group names cannot inject
// commands. These tests pin the exact argv each helper builds.
jest.mock('child_process')
jest.mock('xvfb', () => function Xvfb () { return { startSync () {}, stopSync () {} } })

const fs = require('fs')
const path = require('path')
const cp = require('child_process')
const proc = require('../lipidXte/process.js').default

const APP_DIR = path.join(__dirname, '..')

beforeEach(() => {
  jest.clearAllMocks()
  cp.execFileSync.mockReturnValue(Buffer.from(''))
  jest.spyOn(fs, 'openSync').mockReturnValue(42)
  jest.spyOn(fs, 'closeSync').mockImplementation(() => {})
})

afterEach(() => jest.restoreAllMocks())

test('lipidXte: one argv entry per value, options split on whitespace', () => {
  const ok = proc.lipidXte(APP_DIR, '/d/b/merged.csv', '/d/b', 'g1', 'g2 "quoted"; id', 'g3',
    'Quantity', 'All', 'RemoveRef NoCorrection')
  expect(ok).toBe(true)
  expect(cp.execSync).not.toHaveBeenCalled()
  expect(cp.execFileSync).toHaveBeenCalledTimes(1)
  const [cmd, args, opts] = cp.execFileSync.mock.calls[0]
  expect(cmd).toBe('java')
  expect(args).toEqual([
    '-jar', 'LipidXte-1.0-SNAPSHOT-jfx.jar', '--op=quant',
    '--standard-list=' + path.join(APP_DIR, 'standard_list.csv'),
    '--merged-file=/d/b/merged.csv', '--output-path=/d/b',
    '--group1=g1', '--group2=g2 "quoted"; id', '--group3=g3',
    '--quant-option=Quantity', '--output-option=All',
    'RemoveRef', 'NoCorrection'
  ])
  expect(opts.cwd).toBe(APP_DIR)
  expect(opts.shell).toBeUndefined()
})

test('lipidXte: defaults match the previous command line', () => {
  proc.lipidXte(APP_DIR, '/d/b/merged.csv', '/d/b', 'g1', 'g2', 'g3')
  const args = cp.execFileSync.mock.calls[0][1]
  expect(args.slice(-4)).toEqual(['--quant-option=Quantity', '--output-option=All', 'RemoveRef', 'SummarizeNCE'])
})

test('lipidXte: a failing java run reports false', () => {
  cp.execFileSync.mockImplementation(() => { throw new Error('exit 1') })
  expect(proc.lipidXte(APP_DIR, '/d/b/merged.csv', '/d/b', 'g1', 'g2', 'g3')).toBe(false)
})

test('lipidXplorer: class name and folder are separate arguments', () => {
  expect(proc.lipidXplorer('/d/b', 'PC')).toBe(true)
  const [cmd, args, opts] = cp.execFileSync.mock.calls[0]
  expect(cmd).toBe('python3')
  expect(args).toEqual(['src/lipidXplorer2Lipidx.py', 'PC', '/d/b'])
  expect(opts.cwd).toBe(path.join(APP_DIR, 'LipidXplorer'))
})

test('reorder and processRawfiles pass the folder as one argument', () => {
  expect(proc.reorder('/d/b')).toBe(true)
  expect(proc.processRawfiles('/d/b/')).toBe(true)
  expect(cp.execFileSync.mock.calls.map(c => [c[0], c[1]])).toEqual([
    ['python3', ['src/reorder.py', '/d/b']],
    ['python3', ['src/peakStrainer.py', '/d/b/']]
  ])
})
