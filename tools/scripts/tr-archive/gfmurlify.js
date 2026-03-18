// this one is closer to GitHub, it seems.
const amh = require("anchor-markdown-header");

/** give the anchor as GFM probably would do it */
function gfmurlify(t) {
  if (!t) return null;
  const md = amh(t); // '[message.abnf](#messageabnf)'
  const r = /.*\(#([^)]+)\)/.exec(md);
  if (r) {
    return r[1];
  } else {
    console.error(`nada for ${t} / ${md}`);
    return null;
  }
}

const ELEMENTS = "h2 h3 h4 h5 h6 caption dfn".split(" ");

module.exports = { gfmurlify, ELEMENTS };
