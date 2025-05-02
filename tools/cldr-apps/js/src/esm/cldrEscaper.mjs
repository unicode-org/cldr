import * as cldrClient from "./cldrClient.mjs";

let escapedCharInfo = {
  namedInvisibleRegex: "[\\u200e\\u200f]",
  invisibleRegex: "[\\u200e\\u200f]",
  names: {
    "\u200e": { name: "LRM" },
    "\u200f": { name: "RLM" },
  },
};

export let namedInvisible;
export let invisible;

/** recompile regex */
function compile() {
  namedInvisible = new RegExp(escapedCharInfo.namedInvisibleRegex, "u");
  invisible = new RegExp(escapedCharInfo.invisibleRegex, "u");
}

compile(); // start from static info

/** load the escaper's map */
export async function load() {
  const client = await cldrClient.getClient();
  const { body } = await client.apis.info.getEscapedCharInfo();
  escapedCharInfo = body;
  compile(); // update regex
}

/**
 * Escape any named invisible code points, if needed
 * @param {string} str input string
 * @returns escaped string such as `<span class="visible-mark">&lt;LRM&gt;</span>` or falsy if no escaping was needed
 */
export function getEscapedHtml(str) {
  if (!str) return str;
  if (namedInvisible.test(str)) {
    const escaped = str.replace(namedInvisible, (o) => {
      const e = escapedCharInfo?.names[o];
      if (!e) return o; // could not find the entry
      return `<span class="visible-mark" title="${e.shortName || e.name}\n ${
        e.description || ""
      }">${e.name}</span>`;
    });
    return escaped;
  }
  return undefined;
}
