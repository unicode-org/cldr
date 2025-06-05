let data = null;

const staticInfo = {
  forceEscapeRegex: "[\\u200e\\u200f\\uFFF0]",
  names: {
    "\u200e": { name: "LRM" },
    "\u200f": { name: "RLM" },
  },
}; // start from static info - useful for tests

/** updates content and recompiles regex */
export function updateInfo(escapedCharInfo) {
  const updatedRegex = escapedCharInfo.forceEscapeRegex;
  const forceEscape = new RegExp(updatedRegex, "gu");
  data = { escapedCharInfo, forceEscape };
}

// we preload the static info
updateInfo(staticInfo);

export function needsEscaping(str) {
  if (!str) return false;
  return !!str.match(data.forceEscape);
}

/**
 * Escape any named invisible code points, if needed
 * @param {string} str input string
 * @returns escaped string such as `<span class="visible-mark">&lt;LRM&gt;</span>` or falsy if no escaping was needed
 */
export function getEscapedHtml(str) {
  if (needsEscaping(str)) {
    const escaped = escapeHtml(str);
    return escaped;
  }
  return undefined;
}

/** get information for one char, or null */
export function getCharInfo(str) {
  return data.escapedCharInfo?.names[str];
}

const PREFIX = "❰";
const SUFFIX = "❱";

/** Unconditionally escape (without testing) */
function escapeHtml(str) {
  return str.replaceAll(data.forceEscape, (o) => {
    const hexName = `U+${Number(o.codePointAt(0)).toString(16).toUpperCase()}`;
    let e = getCharInfo(o) || {};
    const name = e.name || e.shortName;
    const body = name || hexName;
    const description = e.description;
    const title = `${e.shortName || ""} ${hexName}\n${description || ""}`;
    return `<span class="visible-mark" title="${title}">${PREFIX}${body}${SUFFIX}</span>`;
  });
}
