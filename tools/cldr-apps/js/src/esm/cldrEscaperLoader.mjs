import * as cldrEscaper from "./cldrEscaper.mjs";
import * as cldrClient from "./cldrClient.mjs";

/** load the escaper's map from the server */
export async function updateEscaperFromServer() {
  const client = await cldrClient.getClient();
  const { body } = await client.apis.info.getEscapedCharInfo();
  cldrEscaper.updateInfo(body); // update regex
}
