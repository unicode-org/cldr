import * as cldrClient from "../esm/cldrClient.mjs";
import * as cldrNotify from "../esm/cldrNotify.mjs";
/**
 * see ClaSignature.java
 * @typedef {Object} ClaSignature
 * @property {boolean} corporate true if a corporate signature
 * @property {boolean} noRights true if employer asserts no rights
 * @property {string} email
 * @property {string} employer
 * @property {string} name
 * @property {boolean} unauthorized true if cla load failed
 * @property {boolean} readonly true if cla may not be modified
 * @property {boolean} signed true if signed, always true when returned from getCla()
 */

/** @return {ClaSignature} signed cla if present otherwise null if not accessible */
export async function getCla() {
  try {
    const client = await cldrClient.getClient();
    const { body } = await client.apis.user.getCla();
    return body;
  } catch (e) {
    if (e.statusCode === 401) {
      return { unauthorized: true };
    } else if (e.statusCode === 404) {
      return { signed: false };
    } else {
      cldrNotify.exception(e, `trying to load CLA`);
      throw e;
    }
  }
}

/**
 * Attempt to sign.
 * @throws {statusCode: 423} if the CLA may not be modified
 * @throws {statusCode: 406} if there is an imput validation error
 * @param {ClaSignature} cla
 * @returns nothing if successful
 */
export async function signCla(cla) {
  const client = await cldrClient.getClient();
  const result = await client.apis.user.signCla(
    {},
    {
      requestBody: cla,
    }
  );
}

/**
 * Attempt to revoke.
 * @throws {statusCode: 423} if the CLA may not be modified
 * @throws {statusCode: 404} if the CLA was never signed
 */
export async function revokeCla() {
  const client = await cldrClient.getClient();
  const result = await client.apis.user.revokeCla();
}
