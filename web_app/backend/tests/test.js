

describe('Response with TSV file', () => {
  it('Returns the file content', async () => {
    const path = require('path')
    const fsext = require('fs-extra')       //File System - for file manipulation

    const BASE_DIR = path.join(__dirname, '.')

    let outputTsv = 'output_Profile_All(SummarizeNCE).tsv'

    let file = BASE_DIR + path.sep  + outputTsv

    let status
    if (fsext.existsSync(file)) {
      let data = fsext.readFileSync(file, 'utf-8')

      data = data.replace(/\t/g, ',')
      // console.log(data)
      // outputTsv = outputTsv.replace('.tsv', '.csv')
      // res.writeHead(200, {
      //   'Content-Type': 'text/csv',
      //   'Content-disposition': 'attachment;filename="' + outputTsv + '"',
      //   'Content-Length': data.length
      // })
      // res.end(new Buffer(data, 'binary'))
      status = 200
    } else {
      console.error(file + ' does not exist. Try to run LipidXte to produce it...')

      // res.sendStatus(500)
      status = 500
    }

    // const res = await requestWithSupertest.get('/classes');
    expect(status).toEqual(200);
    // expect(res.type).toEqual(expect.stringContaining('text/csv'));
  });
});

describe('Check NCE from merged file', () => {
  it('Returns the file content', async () => {
    const path = require('path')
    const fsext = require('fs-extra')       //File System - for file manipulation

    const BASE_DIR = path.join(__dirname, '.')

    let outputTsv = 'resources/merged_25-35.csv'

    let file = BASE_DIR + path.sep  + outputTsv

    let status
    if (fsext.existsSync(file)) {
      let data = fsext.readFileSync(file, 'utf-8')

      data = data.split('\n').filter(c => c.startsWith('#NCE')).map(line => parseFloat(line.split(',')[1]))
      console.log(JSON.stringify(data).replaceAll(',', ', '))
      status = 200
    } else {
      console.error(file + ' does not exist. Try to run LipidXte to produce it...')

      // res.sendStatus(500)
      status = 500
    }

    // const res = await requestWithSupertest.get('/classes');
    expect(status).toEqual(200);
    // expect(res.type).toEqual(expect.stringContaining('text/csv'));
  });
});
describe('Check bcrypt', () => {
  const bcrypt = require('bcrypt')

  it('Generating a hash', async () => {
    const storedHash = await bcrypt.hash('lipidMpi003', 10);
    // console.log(storedHash)

    const result = await bcrypt.compare('lipidMpi003', storedHash );

    expect(result).toEqual(true)
  })

  it('Compare a hash', async () => {
    const result = await bcrypt.compare('lipidMpi003', '$2b$10$GtwrFMoKrcmbKK/dC/FcOOrkkRoT36cPY0LvMqfvEzOUL5y9MUZUW');

    expect(result).toEqual(true)
  })
})
