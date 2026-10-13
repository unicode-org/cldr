#!/bin/bash
# TEMPORARY (CLDR-19852): find out why rendering mermaid with headless Chrome
# fails in the Cloudflare build, and whether @sparticuz/chromium (Chromium that
# ships its own libraries) works there.
# Run from docs/site after `jekyll build`. Writes _site/mermaid-debug.txt and
# _site/mermaid-debug.svg, and always exits 0 so that the preview is deployed.
SITE=$(pwd)
exec > "$SITE/../../_site/mermaid-debug.txt" 2>&1
set -x
cat /etc/os-release
id
node --version
npm --version
echo "HOME=$HOME"
fc-list | wc -l

# 1. Chrome downloaded by puppeteer, as the PR does now
npx puppeteer browsers install chrome
npx puppeteer browsers install chrome-headless-shell
for b in $(find "$HOME/.cache/puppeteer" -type f \( -name chrome -o -name chrome-headless-shell \)); do
  echo "== missing libraries for $b"
  ldd "$b" | grep "not found"
done
npm run render-mermaid
echo "render-mermaid exit code: $?"

# 2. @sparticuz/chromium; AWS_LAMBDA_JS_RUNTIME makes it unpack its libraries
mkdir -p /tmp/sp && cd /tmp/sp && echo '{"private":true,"type":"module"}' > package.json
npm install --no-audit --no-fund @sparticuz/chromium@^153 puppeteer-core@^24.43.1
cat > /tmp/sp/test.mjs <<'EOF'
import * as fs from "node:fs/promises";
import chromium from "@sparticuz/chromium";
import puppeteer from "puppeteer-core";

const [mermaidJs, out] = process.argv.slice(2);
const code = `flowchart LR
  narrowFormat(["Format with<br>narrow symbol"]) --> hasNarrow{"Narrow symbol<br>in locale data?"}
  hasNarrow -->|yes| narrow("Narrow symbol<br>CAD in en: $")
  hasNarrow -->|no| iso("ISO code<br>RUB in de: RUB")`;
console.log("LD_LIBRARY_PATH", process.env.LD_LIBRARY_PATH);
const executablePath = await chromium.executablePath();
console.log("executablePath", executablePath);
const browser = await puppeteer.launch({
  args: await puppeteer.defaultArgs({ args: chromium.args, headless: "shell" }),
  executablePath,
  headless: "shell",
});
console.log("browser version", await browser.version());
const page = await browser.newPage();
await page.addScriptTag({ path: mermaidJs });
const svg = await page.evaluate(async (code) => {
  mermaid.initialize({ startOnLoad: false });
  return (await mermaid.render("mermaid-0", code)).svg;
}, code);
await browser.close();
await fs.writeFile(out, svg);
console.log("sparticuz OK, svg bytes:", svg.length);
EOF
AWS_LAMBDA_JS_RUNTIME=nodejs22.x node /tmp/sp/test.mjs "$SITE/node_modules/mermaid/dist/mermaid.min.js" "$SITE/../../_site/mermaid-debug.svg"
echo "sparticuz exit code: $?"
ls /tmp/al2023/lib
LD_LIBRARY_PATH=/tmp/al2023/lib ldd /tmp/chromium | grep "not found"
exit 0
