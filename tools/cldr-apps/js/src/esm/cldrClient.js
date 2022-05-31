import SwaggerClient from "swagger-client";
import { getSessionId } from "./cldrStatus.js";
import { SURVEY_TOOL_SESSION_HEADER } from "./cldrAjax.js";

const OAS3_ROOT = "/openapi"; // Path to the 'openapi' (sibling to cldr-apps). Needs to be a host-relative URL.

/**
 * Create a promise to a swagger client for ST operations.
 * @returns Promise<SwaggerClient>
 */
function getClient() {
  return new SwaggerClient(OAS3_ROOT, {
    requestInterceptor: (obj) => {
      // add the session header to each request
      obj.headers[SURVEY_TOOL_SESSION_HEADER] = getSessionId();
      return obj;
    },
  });
}

export { getClient };
