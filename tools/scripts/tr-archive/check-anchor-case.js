/**
 * Report similar anchors that take the reader to different sections. Anchors
 * are similar if they differ only in case or in "_" vs "-".
 *
 * Many headings get two anchors: the GitHub-style id (e.g. "number-formats")
 * and, from archive.js, one with the original casing and "_" for spaces
 * ("Number_Formats"). That is fine as long as both land in the same section,
 * like a heading and the definition right under it. If they land in different
 * sections, a link with the other spelling silently goes to the wrong place.
 *
 * Usage: npm run build && npm run check-anchor-case
 * Reads ./dist/*.html, and the Markdown sources to show the line of each
 * anchor. Exits with 1 if it finds such anchors. In GitHub Actions, it also
 * writes the list to the job summary, with links to those lines.
 */
const fs = require("fs").promises;
const path = require("path");
const { JSDOM } = require("jsdom");
const { gfmurlify, ELEMENTS } = require("./gfmurlify");

const SRC_DIR = "../../../docs/ldml/";

/** Elements that count as one place on the page. */
const BLOCKS = new Set(
  "body blockquote caption dd div dt figure h1 h2 h3 h4 h5 h6 li p pre section table td th".split(
    " "
  )
);

const HEADINGS = "h1,h2,h3,h4,h5,h6";
const isHeading = (e) => /^H[1-6]$/.test(e.tagName);

/** The block element that contains e, i.e. the place a link to e lands on. */
function placeOf(e) {
  let p = e;
  while (p && !BLOCKS.has(p.tagName.toLowerCase())) p = p.parentElement;
  return p ?? e;
}

/** Maps each element to the heading it comes under (a heading to itself). */
function sectionsOf(document) {
  const sections = new Map();
  let heading = null;
  for (const e of document.body.querySelectorAll("*")) {
    if (isHeading(e)) heading = e;
    sections.set(e, heading);
  }
  return sections;
}

/** The closest earlier heading of a higher level, i.e. the section h is in. */
function parentOf(h, headings) {
  for (let i = headings.indexOf(h) - 1; i >= 0; i--) {
    if (headings[i].tagName < h.tagName) return headings[i];
  }
  return null;
}

/** Text for matching a heading or term in Markdown with the one in HTML. */
function key(s) {
  return s
    .replace(/&nbsp;/g, " ")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, "&")
    .replace(/<[^>]*>/g, "") // HTML tags
    .replace(/!?\[([^\]]*)\]\([^)]*\)/g, "$1") // [text](url)
    .replace(/\\(?=[^\w\s])/g, "") // backslash escapes
    .replace(/[*_`]/g, "") // emphasis and code marks
    .replace(/\s+/g, " ")
    .trim();
}

/** The lines of the headings and <dfn> terms in Markdown, by tag and text. */
function markdownLines(md) {
  const lines = new Map();
  const add = (k, line) => {
    if (!lines.has(k)) lines.set(k, []);
    lines.get(k).push(line);
  };
  let fence = null;
  md.split("\n").forEach((text, i) => {
    const f = /^\s*(```|~~~)/.exec(text);
    if (f) {
      if (!fence) fence = f[1];
      else if (fence === f[1]) fence = null;
      return;
    }
    if (fence) return;
    const h = /^(#{1,6})\s+(.*?)(?:\s+#+)?\s*$/.exec(text);
    if (h) add(`h${h[1].length} ${key(h[2])}`, i + 1);
    for (const d of text.matchAll(/<dfn[^>]*>(.*?)<\/dfn>/g)) {
      add(`dfn ${key(d[1])}`, i + 1);
    }
  });
  return lines;
}

/** Maps the headings and <dfn> terms in the HTML to their Markdown lines. */
function sourceLines(document, md) {
  const lines = markdownLines(md);
  const seen = new Map();
  const lineOf = new Map();
  for (const e of document.querySelectorAll(`${HEADINGS},dfn`)) {
    const k = `${e.tagName.toLowerCase()} ${key(e.textContent)}`;
    const n = seen.get(k) ?? 0;
    seen.set(k, n + 1);
    const line = lines.get(k)?.[n];
    if (line) lineOf.set(e, line);
  }
  return lineOf;
}

function short(e) {
  const txt = e.textContent.trim().replace(/\s+/g, " ");
  return txt.length > 60 ? txt.slice(0, 59) + "…" : txt;
}

function describe(e) {
  return `<${e.tagName.toLowerCase()}> "${short(e)}"`;
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

  // Similar anchors -> their spellings
  const groups = new Map();
  for (const a of target.keys()) {
    const k = a.toLowerCase().replace(/_/g, "-");
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k).push(a);
  }

  // A heading and the text under it, up to the next heading, are one section.
  const sections = sectionsOf(document);
  const headings = [...document.querySelectorAll(HEADINGS)];
  let lineOf = new Map();
  try {
    const md = await fs.readFile(path.join(SRC_DIR, `${page}.md`), "utf-8");
    lineOf = sourceLines(document, md);
  } catch (e) {
    // No Markdown source: report without line numbers.
  }

  const problems = [];
  for (const spellings of groups.values()) {
    if (spellings.length < 2) continue;
    const places = new Set(spellings.map((a) => sections.get(target.get(a))));
    if (places.size < 2) continue;
    const anchors = spellings.map((a) => {
      const t = target.get(a);
      const section = sections.get(t);
      const spot = t.closest(`${HEADINGS},dfn`) ?? t;
      const under = spot === section ? parentOf(section, headings) : section;
      return {
        anchor: a,
        line: lineOf.get(spot) ?? lineOf.get(section),
        exact: lineOf.has(spot),
        what: describe(isHeading(spot) ? spot : placeOf(t)),
        under: under && short(under),
      };
    });
    problems.push({ page, anchors });
  }
  return problems;
}

/** Where an anchor goes, e.g. 'line 12: <h4> "Examples", under "Units"'. */
function where(a, esc = (s) => s) {
  const line = a.line
    ? `${a.exact ? "line" : "section at line"} ${a.line}: `
    : "";
  const under = a.under ? `, under "${esc(a.under)}"` : "";
  return `${line}${esc(a.what)}${under}`;
}

function toText({ page, anchors }) {
  return (
    `❌ ${page}.md: ${anchors
      .map((a) => "#" + a.anchor)
      .join(" and ")} go to different sections:\n` +
    anchors.map((a) => `     #${a.anchor} → ${where(a)}`).join("\n")
  );
}

/** The job summary, with links to the source lines on GitHub. */
function toMarkdown(problems) {
  const { GITHUB_SERVER_URL, GITHUB_REPOSITORY, GITHUB_SHA } = process.env;
  const blob =
    GITHUB_REPOSITORY && GITHUB_SHA
      ? `${
          GITHUB_SERVER_URL || "https://github.com"
        }/${GITHUB_REPOSITORY}/blob/${GITHUB_SHA}/docs/ldml`
      : null;
  const esc = (s) => s.replace(/[\\`*_{}[\]<>|#]/g, "\\$&");
  const out = ["### Similar anchors that go to different sections", ""];
  if (!problems.length) {
    out.push("✅ None found.");
  } else {
    out.push(
      `⚠️ ${problems.length} set(s) of anchors differ only in case or in \`_\` vs \`-\`, but go to different sections ([CLDR-19840](https://unicode-org.atlassian.net/browse/CLDR-19840)). Each anchor links to its line in the Markdown source.`,
      ""
    );
    for (const { page, anchors } of problems) {
      out.push(
        `- **${page}.md**: ${anchors
          .map((a) => `\`#${a.anchor}\``)
          .join(" and ")}`
      );
      for (const a of anchors) {
        const label = `\`#${a.anchor}\``;
        const link =
          blob && a.line ? `[${label}](${blob}/${page}.md#L${a.line})` : label;
        out.push(`  - ${link} → ${where(a, esc)}`);
      }
    }
  }
  return out.join("\n") + "\n";
}

async function main() {
  const dist = "./dist";
  const files = (await fs.readdir(dist))
    .filter((f) => f.endsWith(".html") && f !== "index.html") // index.html is a copy of tr35.html
    .map((f) => path.join(dist, f));
  const problems = (await Promise.all(files.map(checkFile))).flat();
  problems.forEach((p) => console.error(toText(p)));
  if (process.env.GITHUB_STEP_SUMMARY) {
    await fs.appendFile(process.env.GITHUB_STEP_SUMMARY, toMarkdown(problems));
  }
  if (problems.length) {
    console.error(
      `⚠️ ${problems.length} set(s) of similar anchors go to different sections.`
    );
    process.exitCode = 1;
  } else {
    console.log(
      `✅ In ${files.length} files, anchors that differ only in case or in "_" vs "-" go to the same section.`
    );
  }
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
