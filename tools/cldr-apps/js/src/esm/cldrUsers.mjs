import * as cldrClient from "./cldrClient.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

const userCache = {};

async function getUserInfo(uid) {
  let info = userCache[`u${uid}`];
  if (info !== undefined) {
    return info;
  }
  if (!cldrStatus.getSurveyUser()) {
    // don't bother if not logged in.
    return null;
  }
  info = null;
  const client = await cldrClient.getClient();
  try {
    const { body } = await client.apis.user.getUserInfo({ uid });
    info = body;
  } catch (e) {
    console.error(`Error loading uid${uid}: ${e}`);
  }
  userCache[`u${uid}`] = info; // cache status, even failing
  return info;
}

export { getUserInfo };
