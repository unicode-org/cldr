// extract site frontmatter and read from /sitemap.tsv, save to json

import * as fs from "node:fs/promises";
import { default as process } from "node:process";
import { default as matter } from "gray-matter";
import { SitemapStream, streamToPromise } from "sitemap";
import { Readable } from "node:stream";
import { watchTree } from "watch";
import { dropmd, SKIP_THESE, traverse } from "./utils.mjs";
import { syncAndFixLinks } from "./linkfix.mjs";
import { Dirent } from "node:fs";
// utilities and constants

// final URL of site
const SITE = "https://cldr.unicode.org";

// input file
const SITEMAPFILE = "sitemap.tsv";

// changed file (may not exist)
const CHANGEDFILE = "changed.txt";

// root source location - here
const IN_DIR = ".";

// output for site sync
const OUT_TMP_DIR = "../_site-tmp";

// // utility collator
// const coll = new Intl.Collator(["und"]);

/**
 * Directory Crawler: process one file
 * @param {string} d directory paren
 * @param {string} fullPath path to this file
 * @param {object} out output object
 * @param {Dirent} e dirent
 */
async function processFile(d, fullPath, out, e) {
  if (!/\.md$/.test(e.name)) {
    return;
  }
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

async function readChangedFile(out) {
  let changed;
  try {
    changed = (await fs.readFile(CHANGEDFILE, "utf-8"))
      .trim()
      .split("\n")
      .filter((s) => s !== "");
    console.log(`Found ${changed.length} changed entries from ${CHANGEDFILE}`);
  } catch (e) {
    console.log(`Could not read ${CHANGEDFILE} - no change list set`);
    changed = [];
  }
  // set the changed list
  out.changed = changed;
}

async function buildTree() {
  const out = {
    all: [],
  };
  await fs.mkdir("assets/json/", { recursive: true });
  await traverse(".", out, processFile, SKIP_THESE);
  await writeXmlSiteMap(out);
  await readTsvSiteMap(out);
  await readChangedFile(out);
  // write final json asset
  delete out.all; //not needed at this phase, so trim out of the deploy
  await fs.writeFile("assets/json/tree.json", JSON.stringify(out, null, " "));
  console.log("Wrote assets/json/tree.json");
}

async function syncLinks() {
  await syncAndFixLinks(IN_DIR, OUT_TMP_DIR);
  console.log(`Synced ${IN_DIR} to ${OUT_TMP_DIR}`);
}

async function buildAll() {
  await Promise.all([buildTree(), syncLinks()]);
  console.log("Rebuilt all.");
}

/** top level async */
async function main(argv) {
  let useWatch = false;
  if (argv[0] === "--watch") {
    useWatch = true;
    argv = argv.slice(1);
  }

  await buildAll(); // run at least once

  if (useWatch) {
    watchTree(
      ".",
      {
        filter(path, stat) {
          if (
            path.startsWith("assets/js/build") ||
            path.startsWith("assets/json") ||
            path.startsWith("sitemap.xml") ||
            path.startsWith("node_modules/")
          )
            return false;
          return true;
        },
        interval: 10,
        ignoreDotFiles: true,
        ignoreUnreadableDir: true,
      },
      (f, curr, prev) => {
        if (prev !== null || curr !== null) {
          console.log(`Changed: ${f}`);
          buildAll().then(() => console.log());
        }
      }
    );
  }
}

main(process.argv.slice(2)).then(
  () => console.log("Done."),
  (e) => {
    console.error(e);
    process.exitCode = 1;
  }
);
