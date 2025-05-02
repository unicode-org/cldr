let data = null;

const staticInfo = {
  forceEscapeRegex: "[\\u200e\\u200f]",
  names: {
    "\u200e": { name: "LRM" },
    "\u200f": { name: "RLM" },
  },
}; // start from static info - useful for tests

/** updates content and recompiles regex */
export function updateInfo(escapedCharInfo) {
  const forceEscape = new RegExp(escapedCharInfo.forceEscapeRegex, "u");
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
    const e = getCharInfo(o);
    if (!e) return o; // could not find the entry
    return `<span class="visible-mark" title="${e.shortName || e.name}\n ${
      e.description || ""
    }">${e.name}</span>`;
  });
}
