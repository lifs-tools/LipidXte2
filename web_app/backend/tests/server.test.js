const _ = require('lodash')
const server = require('../index.js');
const supertest = require('supertest');
const requestWithSupertest = supertest(server);


describe('LipidXteServer', () => {
  it('GET / shows all the classes', async () => {
    const res = await requestWithSupertest.get('/classes');
    expect(res.status).toEqual(200);
    expect(res.type).toEqual(expect.stringContaining('json'));
    expect(res.body).toEqual(["PA", "PC", "PCO", "PCO-FANL", "PCO-M-60","PCO-PR","PE", "PEO", "PG", "PI", "PS"])
  });
});


describe('LipidXteServer', () => {
  it('GET / shows all the fractions', async () => {
    const res = await requestWithSupertest.get('/fractions');
    expect(res.status).toEqual(200);
    expect(res.type).toEqual(expect.stringContaining('json'));
    expect(res.body.length).toEqual(50)
    // console.log(_.map(res.body, t => t.name));
    expect(_.map(res.body, t => t.name)).toEqual( [
      '11:0',       '12:0',       '13:0',       '14:0',
      '14:1 (7z)',  '15:0',       '16:0',       '16:1 (9z)',
      '16:2 (9z)',  '16:3 (7z)',  '17:0',       '18:0',
      '18:1 (9z)',  '18:2 (9z)',  '18:3 (6z)',  '18:3 (9z)',
      '18:4 (8z)',  '19:0',       '20:0',       '20:1 (11z)',
      '20:2 (11z)', '20:3 (5z)',  '20:3 (11z)', '20:4 (5z)',
      '20:4 (8z)',  '20:5 (5z)',  '21:0',       '22:0',
      '22:1 (13z)', '22:2 (13z)', '22:3 (13z)', '22:4 (7z)',
      '22:4 (10z)', '22:5 (4z)',  '22:5 (7z)',  '22:6 (4z)',
      '24:0',       '24:1 (15z)', '24:2 (15z)', '24:3 (15z)',
      '24:4 (9z)',  '24:4 (12z)', '24:5 (6z)',  '24:5 (9z)',
      '24:6 (6z)',  '26:6 (8z)',  '28:6 (10z)', '30:6 (12z)',
      '32:6 (14z)', '34:6 (16z)'
    ])
  });
});


it('GET / shows the SN1 curve', async () => {
  const res = await requestWithSupertest.get('/sn1/PC/1');
  expect(res.status).toEqual(200);
  expect(res.type).toEqual(expect.stringContaining('json'));
  // console.log(res.body)
  // expect(res.body).toEqual(["PA", "PC", "PCO", "PE", "PEO", "PG", "PI", "PS"])
});

it('GET / shows the SN2 curve', async () => {
  const res = await requestWithSupertest.get('/sn2/PC/1');
  expect(res.status).toEqual(200);
  expect(res.type).toEqual(expect.stringContaining('json'));
  console.log(res.body)
  // expect(res.body).toEqual(["PA", "PC", "PCO", "PE", "PEO", "PG", "PI", "PS"])
});

it('GET / shows the SYM curve', async () => {
  const res = await requestWithSupertest.get('/SYM/PC/1');
  expect(res.status).toEqual(200);
  expect(res.type).toEqual(expect.stringContaining('json'));
  console.log(res.body)
  // expect(res.body).toEqual(["PA", "PC", "PCO", "PE", "PEO", "PG", "PI", "PS"])
});

it('GET / check the user permission', async () => {
  const res = await requestWithSupertest.get('/checkPass/lipidMpi003');
  expect(res.status).toEqual(200);
  expect(res.type).toEqual(expect.stringContaining('json'));
  expect(res.body).toEqual([true, ''])
});

it('GET / check the user permission with wrong password', async () => {
  const res = await requestWithSupertest.get('/checkPass/wrongPass');
  expect(res.status).toEqual(500);
  expect(res.type).toEqual(expect.stringContaining('json'));
  expect(res.body).toEqual([false, 'No Access'])
});

