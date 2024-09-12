// extract site frontmatter, save to json

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { default as process } from "node:process";
import { default as matter } from "gray-matter";
import { SitemapStream, streamToPromise } from "sitemap";
import { Readable } from "node:stream";

const SKIP_THESE = /(node_modules|\.jekyll-cache|^sitemap.*)/;

async function processFile(d, fullPath, out) {
  const f = await fs.readFile(fullPath, "utf-8");
  const m = matter(f);
  if (m && m.data) {
    const { data } = m;
    out.all.push({ ...data, fullPath });
  } else {
    out.app.push({ fullPath }); // synthesize data?
  }
}

/** process one dirent */
async function processEntry(d, out, e) {
  const fullpath = path.join(d, e.name);
  if (SKIP_THESE.test(e.name)) return;
  if (e.isDirectory()) {
    return await traverse(fullpath, out);
  } else if (!e.isFile() || !/\.md$/.test(e.name)) {
    return;
  }
  await processFile(d, fullpath, out);
}

/**
 * @param {string} d path to directory
 * @param {object} out output struct
 */
async function traverse(d, out) {
  const dirents = await fs.readdir(d, { withFileTypes: true });
  const promises = dirents.map((e) => processEntry(d, out, e));
  return Promise.all(promises);
}

/** replace a/b/c.md with a/b/c */
function dropmd(p) {
  return p.replace(/\.md$/, "");
}

async function writeSiteMaps(out) {
  // simple list of links
  const links = await Promise.all(
    out.all.map(async ({ fullPath, title }) => {
      const stat = await fs.stat(fullPath);
      return {
        url: dropmd(`/${fullPath}`),
        lastmod: stat.mtime.toISOString(),
      };
    })
  );
  const stream = new SitemapStream({ hostname: "https://cldr.unicode.org" });
  const data = (
    await streamToPromise(Readable.from(links).pipe(stream))
  ).toString();
  await fs.writeFile("./sitemap.xml", data, "utf-8");
  console.log("Wrote sitemap.xml");

  /*
  const coll = new Intl.Collator(["und"]);
  const allSorted = [...out.all].sort((a, b) =>
    coll.compare(a.fullPath, b.fullPath)
  );
  await fs.writeFile(
    "./sitemap.md",
    `---\ntitle: Site Map\n---\n\n` +
      allSorted
        .map(
          ({ fullPath, title }) =>
            `- [/${fullPath}](/${dropmd(fullPath)}) - ${title}`
        )
        .join("\n"),
    "utf-8"
  );
  console.log("Wrote sitemap.md");
  */
}

async function main() {
  const out = {
    all: [],
    dirs: {},
  };
  await fs.mkdir("assets/json/", { recursive: true });
  await traverse(".", out);
  await fs.writeFile("assets/json/tree.json", JSON.stringify(out, null, " "));
  console.log("Wrote assets/json/tree.json");
  await writeSiteMaps(out);
}

main().then(
  () => console.log("Done."),
  (e) => {
    console.error(e);
    process.exitCode = 1;
  }
);
