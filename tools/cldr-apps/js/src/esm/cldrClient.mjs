import { getSessionId } from "./cldrStatus.mjs";
import { makeClient } from "./cldrClientInternal.mjs";

let client = null; // cached client object

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
    client = makeClient(document.baseURI, () => getSessionId());
  }
  return client;
}

export { getClient };
