// Render mermaid code blocks in the built site (_site) to inline SVG, so that
// pages show diagrams without loading mermaid in the browser.
// Run after `jekyll build`: node assets/js/render-mermaid.mjs <site dir>

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { default as process } from "node:process";
import puppeteer from "puppeteer";
import { renderMermaid } from "@mermaid-js/mermaid-cli";

const siteDir = process.argv[2] ?? "../../_site";

// kramdown writes a ```mermaid block as one of these, depending on whether
// a syntax highlighter (Rouge) is used. Mermaid source is HTML-escaped.
const BLOCK_PATTERNS = [
  /<div class="language-mermaid highlighter-rouge">\s*<div class="highlight">\s*<pre class="highlight"><code>([\s\S]*?)<\/code><\/pre>\s*<\/div>\s*<\/div>/g,
  /<pre><code class="language-mermaid">([\s\S]*?)<\/code><\/pre>/g,
];

function unescapeHtml(s) {
  return s
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, "&");
}

async function* htmlFiles(dir) {
  for (const e of await fs.readdir(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) yield* htmlFiles(p);
    else if (e.name.endsWith(".html")) yield p;
  }
}

async function renderPage(browser, file) {
  const html = await fs.readFile(file, "utf-8");
  if (!html.includes("language-mermaid")) return 0;
  let n = 0;
  let out = html;
  for (const pattern of BLOCK_PATTERNS) {
    const matches = [...out.matchAll(pattern)];
    for (const m of matches) {
      // ids must be unique within a page: mermaid scopes its CSS by svg id
      const svgId = `mermaid-${n++}`;
      const { data } = await renderMermaid(
        browser,
        unescapeHtml(m[1]),
        "svg",
        { backgroundColor: "transparent", svgId },
      );
      const svg = new TextDecoder().decode(data);
      out = out.replace(m[0], () => `<div class="mermaid">${svg}</div>`);
    }
  }
  if (n) await fs.writeFile(file, out);
  return n;
}

const browser = await puppeteer.launch({
  // build containers usually run as root, where Chrome's sandbox can't start
  args: ["--no-sandbox", "--disable-setuid-sandbox"],
});
let total = 0;
try {
  for await (const file of htmlFiles(siteDir)) {
    const n = await renderPage(browser, file);
    if (n) console.log(`Rendered ${n} mermaid diagram(s) in ${file}`);
    total += n;
  }
} finally {
  await browser.close();
}
console.log(`Rendered ${total} mermaid diagram(s)`);
