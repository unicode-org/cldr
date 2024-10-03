// extract site frontmatter and read from /sitemap.tsv, save to json

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { default as process } from "node:process";
import { default as matter } from "gray-matter";
import { SitemapStream, streamToPromise } from "sitemap";
import { Readable } from "node:stream";
import { Dirent } from "node:fs";

// utilities and constants

// files to skip
const SKIP_THESE = /(node_modules|\.jekyll-cache|^sitemap.tsv)/;

// final URL of site
const SITE = "https://cldr.unicode.org";

// input file
const SITEMAPFILE = "sitemap.tsv";

// utility collator
const coll = new Intl.Collator(["und"]);

/**
 * Directory Crawler: process one directory
 * @param {string} d directory paren
 * @param {string} fullPath path to this file
 * @param {object} out output object
 */
async function processFile(d, fullPath, out) {
  const f = await fs.readFile(fullPath, "utf-8");
  const m = matter(f);
  fullPath = fullPath.replace(/\\/g, "/"); // backslash with slash, for win
  if (m && m.data) {
    const { data } = m;
    out.all.push({ ...data, fullPath });
  } else {
    out.app.push({ fullPath }); // synthesize data?
  }
}

/**
 * Directory Crawler: process one dirent
 * @param {string} d directory paren
 * @param {object} out output object
 * @param {Dirent} e directory entry
 * @returns
 */
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
 * Directory Crawler: kick off the crawl (or subcrawl) of a directory
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

/**
 *
 * @param {number} n
 * @returns string with n tabs
 */
function tabs(n) {
  let s = [];
  for (let i = 0; i < n; i++) {
    s.push("\t");
  }
  return s.join("");
}

/** convert a markdown path to a final URL */
function mkurl(p) {
  return `${SITE}/${md2html(p)}`;
}

async function writeXmlSiteMap(out) {
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
}

async function readTsvSiteMap(out) {
  console.log(`Reading ${SITEMAPFILE}`);
  const lines = (await fs.readFile(SITEMAPFILE, "utf-8")).split("\n"); // don't skip comment lines here so we can get line numbers.
  const errors = [];

  // user's specified map
  const usermap = {
    /*
    index: {
      parent: null,
      title: 'CLDR Site',
      children: [
        'cldr-spec',
        'downloads',
        …
      ],
    },
    'cldr-spec': {
      parent: 'index',
      title: …,
      children: [
        'cldr-spec/collation-guidelines',
        …
      ],
    },
    'cldr-spec/collation-guidelines': {
      parent: 'cldr-spec',
      title: …,
      children: null,
    },
  */
  };
  // stack of parents, in order
  let parents = [];
  let n = 0;
  for (let line of lines) {
    n++;
    const location = `${SITEMAPFILE}:${n}: `; // for errors
    // skip comment or blank lines
    if (/^[ \t]*#/.test(line) || !line.trim()) continue;

    // # of leading
    const tabs = /^[\t]*/.exec(line)[0].length;
    // rest of line: the actual path
    const path = line.slice(tabs).trim();
    if (usermap[path]) {
      errors.push(`${location} duplicate path: ${path}`);
      continue;
    }
    const foundItem = out.all.find(({ fullPath }) => fullPath === `${path}.md`);
    if (!foundItem) {
      errors.push(`${location} could not find file: ${path}.md`);
      continue;
    }
    if (!foundItem.title) {
      errors.push(`${location} missing title in ${path}.md`);
      // let this continue on
    }
    usermap[path] = {
      title: foundItem.title ?? path,
    };
    const parentCount = parents.length;
    if (tabs < parentCount) {
      /**
       * index [1]
       *    foo [2]
       *
       */
      // outdent
      if (tabs == 0) {
        errors.push(`${location} can't have more than one root page!`);
        break;
      }
      // drop 'n' parents
      parents = parents.slice(0, tabs);
    } else if (tabs > parentCount) {
      // Error - wrong indent
      errors.push(
        `${location} indent too deep (expected ${parentCount} tabs at most)`
      );
      continue;
    }
    const parent = parents.slice(-1)[0] || null; // calculate parent (null for index page)
    usermap[path].parent = parent;
    if (parent) {
      // not for index
      usermap[parent].children = usermap[parent].children ?? [];
      usermap[parent].children.push(path);
    }
    parents.push(path); // for next time
  }
  out.usermap = usermap;
  out.all.forEach(({ fullPath }) => {
    if (!usermap[dropmd(fullPath)]) {
      errors.push(`${SITEMAPFILE}: missing: ${dropmd(fullPath)}`);
    }
  });
  if (errors.length) {
    errors.forEach((l) => console.error(l));
    throw Error(`${errors.length} errors reading tsv`);
  } else {
    console.log(`${SITEMAPFILE} Valid.`);
  }
}

/** top level async */
async function main() {
  const out = {
    all: [],
  };
  await fs.mkdir("assets/json/", { recursive: true });
  await traverse(".", out);
  await writeXmlSiteMap(out);
  await readTsvSiteMap(out);
  // write final json asset
  delete out.all; //not needed at this phase, so trim out of the deploy
  await fs.writeFile("assets/json/tree.json", JSON.stringify(out, null, " "));
  console.log("Wrote assets/json/tree.json");
}

main().then(
  () => console.log("Done."),
  (e) => {
    console.error(e);
    process.exitCode = 1;
  }
);
