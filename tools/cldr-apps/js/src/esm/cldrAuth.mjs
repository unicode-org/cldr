import * as cldrClient from "../esm/cldrClient.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

/**
 * Get the oauth login URL. See GithubLoginFactory#getLoginUrl()
 * @param {object} o options bag
 * @param {string} o.service which service, currently only 'github' is accepted
 * @param {string} o.intent what the login URL is used for, currently only 'cla'
 * @param {boolean} o.relogin if true, attempt re-login
 */
export async function getLoginUrl(o) {
  const { service, intent, relogin } = o;
  if (service !== "github")
    throw Error(`only support service='github' but got ${service}`);
  if (intent !== "cla")
    throw Error(`only support intent='cla' but got ${intent}`);
  const client = await cldrClient.getClient();
  const { url } = (await client.apis.auth.oauthUrl()).body;
  const u = new URL(url);
  const redir = new URL(window.location);
  redir.search = "";
  redir.hash = "";
  redir.pathname = cldrStatus.getContextPath() + "/github-login";
  u.searchParams.set("redirect_uri", redir);
  if (relogin) {
    u.searchParams.set("prompt", "select_account");
  }
  return u;
}

/**
 * If a valid github ID is in the session, return it, otherwise falsy
 * @returns github ID or falsy
 */
export async function getGithubIdFromSession() {
  try {
    const client = await cldrClient.getClient();
    const { id } = (await client.apis.auth.oauthSession()).body;
    return id;
  } catch (e) {
    console.error(e);
    console.error("getGithubIdFromSession() failed");
  }
  return null;
}
