// extract site frontmatter, save to json

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { default as process } from "node:process";
import { default as matter } from "gray-matter";
import { SitemapStream, streamToPromise } from "sitemap";
import { Readable } from "node:stream";

const SKIP_THESE = /(node_modules|\.jekyll-cache|^sitemap.*)/;

const SITE = "https://cldr.unicode.org";

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

/** replace a/b/c.md with a/b */
function path2dir(p) {
  const dir = p.split("/").slice(0, -1).join("/");
  return dir;
}

/** replace a/b/c.md with a/b/c.html */
function md2html(p) {
  return p.replace(/\.md$/, ".html");
}

/** replace a/b/c.html with a/b/c.md */
function html2md(p) {
  return p.replace(/\.html$/, ".md");
}

/** replace a/b/c.md with a/b/c */
function dropmd(p) {
  return p.replace(/\.md$/, "");
}

function tabs(n) {
  let s = [];
  for (let i = 0; i < n; i++) {
    s.push("\t");
  }
  return s.join("");
}

function mkurl(p) {
  return `${SITE}/${md2html(p)}`;
}

const coll = new Intl.Collator(["und"]);

function writeSiteMapSheet({ all, allDirs }, path, outsheet) {
  // write my index
  function indexForPath(p) {
    if (p === "") {
      p = "index.md";
    } else {
      p = path2dir(p) + ".md";
    }
    return all.findIndex(({ fullPath }) => fullPath === p);
  }
  const myIndex = indexForPath(path);
  if (myIndex === -1) {
    throw Error(`Could not find index for ${path}`);
  }
  const { title, fullPath: indexPath } = all[myIndex];
  // find how how much to indent.
  // 'path' is '' or 'foo/' or 'foo/bar/baz/' at this point.
  const slashes = path.replace(/[^\/]+/g, ""); // foo/bar/ => //
  const indent = tabs(slashes.length); // number of slashes => number of tabs
  outsheet.push(`${indent}${title}\t${mkurl(indexPath)}`);

  // now, gather the children.
  const children = all.filter(({ fullPath }) => {
    if (fullPath === indexPath) return false; // no self-list.
    const myDir = path2dir(fullPath);
    // would this item be under our dir?
    if (`${myDir}.md` === indexPath) return true;
    // special case for odd /index subdir
    if (indexPath === `index.md` && myDir === "") return true;
    return false;
  });

  children.sort((a, b) => coll.compare(a.fullPath, b.fullPath));

  children.forEach(({ title, fullPath }) => {
    // if an index, recurse instead.
    const baseName = dropmd(fullPath); // downloads.md -> downloads
    if (allDirs.has(baseName)) {
      // it's a non-leaf node, recurse.
      writeSiteMapSheet({ all, allDirs }, `${baseName}/`, outsheet);
    } else {
      // write leaf (non-index) child pages
      outsheet.push(`${indent}\t${title}\t${mkurl(fullPath)}`);
    }
  });
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
  const stream = new SitemapStream({ hostname: SITE });
  const data = (
    await streamToPromise(Readable.from(links).pipe(stream))
  ).toString();
  await fs.writeFile("./sitemap.xml", data, "utf-8");
  console.log(`Wrote sitemap.xml with ${links.length} entries`);

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

  // now, create sitemap.tsv by walking
  const outsheet = [];
  const allPaths = out.all.map(({ fullPath }) => fullPath);
  // Find all 'directories' (ending with /)
  const allDirs = new Set();
  allPaths.forEach((p) => {
    const segs = p.split("/").slice(0, -1); // ['', 'dir1']
    for (let n = 0; n <= segs.length; n++) {
      // add all parent paths, so: '', dir1, dir1/dir2 etc.
      const subpath = segs.slice(0, n).join("/");
      allDirs.add(subpath);
    }
  });

  writeSiteMapSheet({ all: out.all, allDirs }, "", outsheet);
  await fs.writeFile("./sitemap.tsv", outsheet.join("\n"), "utf-8");
  console.log(`wrote sitemap.tsv with ${outsheet.length} entries`);
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
