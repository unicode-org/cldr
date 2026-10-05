/**
 * Report anchors that differ only in case but take the reader to different places.
 *
 * Many headings get two anchors: the GitHub-style id (e.g. "currencydecimal")
 * and, from archive.js, one with the original casing ("currencyDecimal").
 * That is fine as long as every case variant lands in the same place. If they
 * land in different places, a link with the other casing silently goes to the
 * wrong place.
 *
 * Usage: npm run build && npm run check-anchor-case
 * Reads ./dist/*.html. Exits with 1 if it finds such anchors.
 */
const fs = require("fs").promises;
const path = require("path");
const { JSDOM } = require("jsdom");
const { gfmurlify, ELEMENTS } = require("./gfmurlify");

/** Elements that count as one place on the page. */
const BLOCKS = new Set(
  "body blockquote caption dd div dt figure h1 h2 h3 h4 h5 h6 li p pre section table td th".split(
    " "
  )
);

/** The block element that contains e, i.e. the place a link to e lands on. */
function placeOf(e) {
  let p = e;
  while (p && !BLOCKS.has(p.tagName.toLowerCase())) p = p.parentElement;
  return p ?? e;
}

function describe(e) {
  const txt = e.textContent.trim().replace(/\s+/g, " ");
  return `<${e.tagName.toLowerCase()}> "${
    txt.length > 60 ? txt.slice(0, 59) + "…" : txt
  }"`;
}

async function checkFile(file) {
  const page = path.basename(file, ".html");
  let html = await fs.readFile(file, "utf-8");
  if (html.charCodeAt(0) == 0xfeff) html = html.substring(1);
  const document = new JSDOM(html).window.document;

  // Where a link to #x lands, as in a browser: the first element with id x,
  // otherwise the first <a> with name x.
  const byId = new Map();
  const byName = new Map();
  for (const e of document.querySelectorAll("[id]")) {
    if (!byId.has(e.id)) byId.set(e.id, e);
  }
  for (const e of document.querySelectorAll("a[name]")) {
    const n = e.getAttribute("name");
    if (!byName.has(n)) byName.set(n, e);
  }
  // Headings etc. without an id get their GitHub-style id when the page loads
  // (anchor-js), unless another element already has it.
  for (const e of document.querySelectorAll(ELEMENTS.join(","))) {
    if (e.id) continue;
    const id = gfmurlify(e.textContent.trim());
    if (id && !byId.has(id) && !byName.has(id)) byId.set(id, e);
  }
  const target = new Map([...byName, ...byId]); // an id wins over a name

  const groups = new Map(); // lowercased anchor -> its spellings
  for (const a of target.keys()) {
    const k = a.toLowerCase();
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k).push(a);
  }

  const problems = [];
  for (const spellings of groups.values()) {
    if (spellings.length < 2) continue;
    const places = new Set(spellings.map((a) => placeOf(target.get(a))));
    if (places.size < 2) continue;
    problems.push(
      `❌ ${page}.md: ${spellings
        .map((a) => "#" + a)
        .join(" and ")} go to different places:\n` +
        spellings
          .map((a) => `     #${a} → ${describe(placeOf(target.get(a)))}`)
          .join("\n")
    );
  }
  return problems;
}

async function main() {
  const dist = "./dist";
  const files = (await fs.readdir(dist))
    .filter((f) => f.endsWith(".html") && f !== "index.html") // index.html is a copy of tr35.html
    .map((f) => path.join(dist, f));
  const problems = (await Promise.all(files.map(checkFile))).flat();
  problems.forEach((p) => console.error(p));
  if (problems.length) {
    console.error(
      `⚠️ ${problems.length} anchor(s) whose case variants go to different places.`
    );
    process.exitCode = 1;
  } else {
    console.log(
      `✅ In ${files.length} files, anchors that differ only in case go to the same place.`
    );
  }
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
