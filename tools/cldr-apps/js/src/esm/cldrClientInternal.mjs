import SwaggerClient from "swagger-client";
import { SURVEY_TOOL_SESSION_HEADER, OAS3_ROOT } from "./cldrConstants.mjs";

/**
 *
 * @param {string|URL} baseURI base URI such as document.baseURI
 * @param {() => String} getSessionId  function for getting session id
 * @returns {Promise<Client>}
 */
export function makeClient(baseURI, getSessionId) {
  const RESOLVED_ROOT = new URL(OAS3_ROOT, baseURI); // workaround relative resolution issues
  return SwaggerClient({
    url: RESOLVED_ROOT,
    requestInterceptor: (obj) => {
      // add the session header to each request
      obj.headers[SURVEY_TOOL_SESSION_HEADER] = getSessionId();
      return obj;
    },
  });
}
