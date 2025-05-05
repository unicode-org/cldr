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
  const updatedRegex = escapedCharInfo.forceEscapeRegex
    .replace(/\\ /g, " ")
    .replace(/\\U[0]*([0-9a-fA-F]+)/g, `\\u{$1}`);
  console.log(updatedRegex);
  const forceEscape = new RegExp(updatedRegex, "u");
  data = { escapedCharInfo, forceEscape };
}

// we preload the static info
updateInfo(staticInfo);

export function needsEscaping(str) {
  if (!str) return false;
  return data?.forceEscape?.test(str);
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
  return data?.escapedCharInfo?.names[str];
}

/** Unconditionally escape (without testing) */
function escapeHtml(str) {
  return str.replace(data?.forceEscape, (o) => {
    const e = getCharInfo(o) || {
      name: `U+${Number(o.codePointAt(0)).toString(16).toUpperCase()}`,
    };
    return `<span class="visible-mark" title="${e.shortName || e.name}\n ${
      e.description || ""
    }">${e.name}</span>`;
  });
}
