import { env } from "node:process";
import { makeClient } from "../../src/esm/cldrClientInternal.mjs";

export const { CLDR_VAP, SURVEYTOOL_URL } = env;
export const baseURI = `${SURVEYTOOL_URL}/cldr-apps/v#/en/Number_Formatting_Patterns/67afe297d3a17a3`;

if (!SURVEYTOOL_URL) {
  throw Error(`SURVEYTOOL_URL= **NOT SET**`);
}
console.log(`SURVEYTOOL_URL=${SURVEYTOOL_URL}`);
if (CLDR_VAP) {
  console.log(`CLDR_VAP=<REDACTED>`);
} else {
  throw Error(`CLDR_VAP= **NOT SET**`);
}

let fakeSessionId = undefined;

export function setSessionId(s) {
  fakeSessionId = s;
}

let client;

/** @returns {Promise<Client>} */
export function getClient() {
  if (!client) {
    client = makeClient(baseURI, () => fakeSessionId);
  }
  return client;
}

export function clientSetup() {
  // if needed, set up any globals
  // global.document = {
  //     // setup an example uri
  //     baseURI,
  // };
}
