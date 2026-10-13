// Render mermaid code blocks in the built site (_site) to inline SVG, so that
// pages show diagrams without loading mermaid in the browser.
// Run after `jekyll build`: node assets/js/render-mermaid.mjs <site dir>
//
// Mermaid needs a browser to measure text, so this runs it in headless
// Chromium. The Cloudflare build image lacks Chrome's system libraries
// (libatk, libxkbcommon, …) and has no root to install them, so on Linux this
// uses @sparticuz/chromium, a Chromium build that brings its own libraries.

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { default as process } from "node:process";

// @sparticuz/chromium unpacks its libraries only when it detects AWS Lambda,
// so claim Lambda's Node 22 runtime. This must be set before the import.
process.env.AWS_LAMBDA_JS_RUNTIME ??= "nodejs22.x";
const { default: chromium } = await import("@sparticuz/chromium");
const { default: puppeteer } = await import("puppeteer-core");

const siteDir = process.argv[2] ?? "../../_site";
const MERMAID_JS = "node_modules/mermaid/dist/mermaid.min.js";

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

async function renderPage(page, file) {
  const html = await fs.readFile(file, "utf-8");
  if (!html.includes("language-mermaid")) return 0;
  let n = 0;
  let out = html;
  for (const pattern of BLOCK_PATTERNS) {
    for (const m of [...out.matchAll(pattern)]) {
      // ids must be unique within a page: mermaid scopes its CSS by svg id
      const svg = await page.evaluate(
        async (id, code) => (await mermaid.render(id, code)).svg,
        `mermaid-${n++}`,
        unescapeHtml(m[1]),
      );
      out = out.replace(m[0], () => `<div class="mermaid">${svg}</div>`);
    }
  }
  if (n) await fs.writeFile(file, out);
  return n;
}

// @sparticuz/chromium only has Linux binaries; elsewhere use the installed Chrome
const browser = await puppeteer.launch(
  process.platform === "linux"
    ? {
        args: await puppeteer.defaultArgs({ args: chromium.args, headless: "shell" }),
        executablePath: await chromium.executablePath(),
        headless: "shell",
      }
    : { channel: "chrome" },
);
let total = 0;
try {
  const page = await browser.newPage();
  await page.addScriptTag({ path: MERMAID_JS });
  await page.evaluate(() => mermaid.initialize({ startOnLoad: false }));
  for await (const file of htmlFiles(siteDir)) {
    const n = await renderPage(page, file);
    if (n) console.log(`Rendered ${n} mermaid diagram(s) in ${file}`);
    total += n;
  }
} finally {
  await browser.close();
}
console.log(`Rendered ${total} mermaid diagram(s)`);
