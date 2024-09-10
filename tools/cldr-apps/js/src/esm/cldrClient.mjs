import SwaggerClient from "swagger-client";
import { getSessionId } from "./cldrStatus.mjs";
import { SURVEY_TOOL_SESSION_HEADER } from "./cldrAjax.mjs";

const OAS3_ROOT = "/openapi"; // Path to the 'openapi' (sibling to cldr-apps). Needs to be a host-relative URL.
let client = null; // cached client object
const RESOLVED_ROOT = new URL(OAS3_ROOT, document.baseURI); // workaround relative resolution issues

function makeClient() {
  return SwaggerClient({
    url: RESOLVED_ROOT,
    requestInterceptor: (obj) => {
      // add the session header to each request
      obj.headers[SURVEY_TOOL_SESSION_HEADER] = getSessionId();
      return obj;
    },
  });
}

/**
 * Create a promise to a swagger client for ST operations.
 *
 * So, `const client = await cldrClient.getClient();`
 *
 * For more documentation on how to use this, see
 *
 * <https://github.com/swagger-api/swagger-js/blob/master/docs/usage/tags-interface.md#openapi-v3x>
 *
 * @returns {Promise<SwaggerClient>}
 */
function getClient() {
  if (!client) {
    client = makeClient();
  }
  return client;
}

export { getClient };
