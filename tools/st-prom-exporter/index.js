/*
 * Copyright (C) 2929, Unicode, Inc.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */

const express = require('express');
const process = require('process');
const server = express();
const client = require('prom-client');
const bent = require('bent');
const getJson = bent('json');
const config = require('./config.json');
const {register} = client; // global registry

const items = {
  responses: new client.Counter({
    name: 'surveytool_exporter_responses',
    help: 'Number of pages served by this exporter'
  }),
  oks: new client.Counter({
    name: 'surveytool_exporter_oks',
    help: 'Number of OK fetches',
    labelNames: ['instance']
  }),
  fails: new client.Counter({
    name: 'surveytool_exporter_fails',
    help: 'Number of failed fetches',
    labelNames: ['instance']
  }),
  ok: new client.Gauge({
    name: 'surveytool_ok',
    help: '1 if the surveytool is ok, otherwise 0',
    labelNames: ['instance']
  }),
  isSetup: new client.Gauge({
    name: 'surveytool_setup',
    help: '1 if the surveytool is setup, otherwise 0',
    labelNames: ['instance']
  }),
  isBusted: new client.Gauge({
    name: 'surveytool_busted',
    help: '1 if the surveytool is busted, otherwise 0',
    labelNames: ['instance'/*, 'err'*/]
  }),
  fetchTime: new client.Gauge({
    name: 'surveytool_fetchTime',
    help: 'time of successful fetch',
    labelNames: ['instance']
  }),
  fetchErr: new client.Gauge({
    name: 'surveytool_fetchErr',
    help: 'error code on failed fetch, or 200',
    labelNames: ['instance'/*, 'err'*/]
  }),
  pages: new client.Gauge({
    name: 'surveytool_pages',
    help: 'page count',
    labelNames: ['instance']
  }),
  users: new client.Gauge({
    name: 'surveytool_users',
    help: 'user count',
    labelNames: ['instance']
  }),
  stamp: new client.Gauge({
    name: 'surveytool_stamp',
    help: 'survey running stamp',
    labelNames: ['instance' /*,
  'phase', 'sysprocs', 'environment', 'currev', 'newVersion'*/]
  }),
  memtotal: new client.Gauge({
    name: 'surveytool_memtotal',
    help: 'total memory in process',
    labelNames: ['instance']
  }),
  memfree: new client.Gauge({
    name: 'surveytool_memfree',
    help: 'total free memory',
    labelNames: ['instance']
  }),
  dbused: new client.Gauge({
    name: 'surveytool_dbused',
    help: 'db queries used',
    labelNames: ['instance']
  }),
  sysload: new client.Gauge({
    name: 'surveytool_sysload',
    help: 'system load, if available',
    labelNames: ['instance']
  }),
};

async function update(e) {
  const [instance, url] = e;
  try {
    const res = await getJson(url);
    items.oks.inc({instance});
    items.fetchErr.set({ instance }, 200);
    items.fetchTime.set({ instance }, new Date().getTime()/1000);
    items.ok.set({instance}, Number(res.SurveyOK));
    items.isSetup.set({instance}, Number(res.isSetup));
    if(res.status) {
      const{phase, memtotal, sysprocs, isBusted, isUnofficial, lockOut,
        users, uptime, memfree, environment, pages, specialHeader, currev,
        dbopen, surveyRunningStamp, guests, dbused,sysload, isSetup, newVersion} = res.status;
        items.pages.set({instance}, Number(pages));
        items.users.set({instance}, Number(users));
        items.memtotal.set({instance}, Number(memtotal));
        items.memfree.set({instance}, Number(memfree));
        items.dbused.set({instance}, Number(dbused));
        items.sysload.set({instance}, Number(sysload));
        items.stamp.set({instance /*,
        phase, sysprocs, environment, currev, newVersion*/},
          surveyRunningStamp);
        if(isBusted) {
          items.isBusted.set({instance/*, err: isBusted*/}, Number(1));
        } else {
          items.isBusted.set({instance/*, err: (res.err || '')*/}, Number(res.isBusted));
        }
    } else {
      items.isBusted.set({instance/*, err: (res.err || '')*/}, Number(res.isBusted));
    }
  } catch(ex) {
    items.fails.inc({instance});
    items.fetchErr.set({ instance /*, err: ex.toString()*/ }, 999);
  }
}

async function updateAll() {
  return Promise.all(Object.entries(config.instances)
    .map(e => update(e)));
}

server.get('/metrics', async (req, res) => {
  items.responses.inc();
  res.contentType(register.contentType);
  await updateAll();
  res.end(register.metrics());
});

const port = process.env.PORT || config.port || 3000;

server.listen(port, () => {
  console.log('ST exporter listening on port ' + port);
});