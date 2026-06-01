import { expect } from "chai";
import mocha from "mocha";
import { clientSetup, getClient, baseURI } from "./util.mjs";

clientSetup(); // must be called first

describe(`client test`, async function () {
  it(`Should be able to create the Swagger client`, async function () {
    const client = await getClient();
    expect(client).to.be.ok;
    // list out the APIs
    // console.dir(client.apis);
    // console.dir(client);
  });
  it(`Should be able to getInfo`, async function () {
    const client = await getClient();
    const { body } = await client.apis.about.getAbout();
    // console.dir({body});
    expect(body).to.be.ok;
    const { CLDR_CODE_HASH, GEN_VERSION } = body;
    expect(CLDR_CODE_HASH).to.be.ok;
    expect(GEN_VERSION).to.be.ok;
    console.log(
      `https://github.com/unicode-org/cldr/commit/${CLDR_CODE_HASH} v ${GEN_VERSION}`
    );
  });
});

describe(`status test`, async () => {
  const STATUS_URL = new URL(`SurveyAjax?what=status`, baseURI);
  it(`Should be able to retrieve status from ${STATUS_URL}`, async () => {
    const r = await fetch(STATUS_URL);
    // console.dir(r);
    expect(r).to.be.ok;
    const body = await r.json();
    expect(r).to.be.ok;
    // const {surveyOK, isBusted,isSetup,status,triedToStartUp,wasInitCalled} = body;
    // console.dir(body);
  });
});
