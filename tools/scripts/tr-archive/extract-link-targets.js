const fs = require("fs").promises;
const jsdom = require("jsdom");
const { JSDOM } = jsdom;
const path = require("path");

/**
 * Run this after outputting html into 'dist'
 * It will update ../../../docs/ldml/*.anchors.json
 * Use source control to see if the links have changed.
 */

// We would ideally run marked and process the output here.
// But that might introduce duplicate code.
const DONE_ICON = "✅";
const GEAR_ICON = "⚙️";
const NONE_ICON = "∅";
const PACKAGE_ICON = "📦";
const SECTION_ICON = "📍";
const TYPE_ICON = "📂";
const WARN_ICON = "⚠️";
const POINT_ICON = "👉";
const MISSING_ICON = "❌";

/**
 *
 * @param {string} targetSection e.g. 'tr35-info'
 * @param {string} anchor e.g. 'Parts'
 * @returns 'tr35-info.md#Parts'
 */
function constructLink(targetSection, anchor) {
  const page = `${targetSection}.md`;
  if (!anchor) {
    return page;
  }
  return `${page}#${anchor}`;
}

/**
 * Read the input .md file, and write to a corresponding .html file
 * @param {string} infile path to input file
 * @returns {Promise<string>} name of output file (for status update)
 */
async function extractAnchors(infile) {
  const basename = path.basename(infile, ".html");
  dirname = '../../../docs/ldml';
  console.log(`${SECTION_ICON} Reading ${infile}`);
  let f1 = await fs.readFile(infile, "utf-8");

  // oh the irony of removing a BOM before posting to unicode.org
  if (f1.charCodeAt(0) == 0xfeff) {
    f1 = f1.substring(3);
  }

  const rawHtml = f1;

  // now fix. Spin up a JSDOM so we can manipulate
  const dom = new JSDOM(rawHtml);
  const document = dom.window.document;

  const anchors = new Set();
  const targets = new Set();

  function addAnchor(n) {
    if (!n) return;
    if (anchors.has(n)) {
      console.error(`${WARN_ICON} ${constructLink(basename)}: Duplicate anchor: #${n}`);
    } else {
      anchors.add(n);
    }
  }

  function addTarget(href) {
    const INTRA_PAGE_LINK = /^#(.*)$/; // starts with #  => 1=anchor
    const TR_SECTION_LINK = /^(tr35(?:[^.]*)).html(?:#(.*)){0,1}$/; // => 1=basename, 2=anchor
    const EXTERNAL_LINK = /^(http|https|mailto|ftp):.*$/; // scheme
    // Error on all other links

    const intra_page = INTRA_PAGE_LINK.exec(href);
    const tr_section = TR_SECTION_LINK.exec(href);
    const external   = EXTERNAL_LINK.exec(href);
    if (intra_page) {
      // same page
      targets.add(constructLink(basename, intra_page[1]));
    } else if (tr_section) {
      // another page
      targets.add(constructLink(tr_section[1], tr_section[2]));
    } else if (external) {
      // external
      // Do nothing
      // TODO: add to list of external links?
    } else {
      console.error(`${WARN_ICON} ${basename}: Unknown anchor: ${href}`);
    }
  }

  // extract anchors
  for (const a of dom.window.document.getElementsByTagName("*")) {
    const id = a.getAttribute("id");
    addAnchor(id);

    if (a.tagName === 'A') {
      const name = a.getAttribute("name");
      addAnchor(name);
    }
  }
  // extract targets
  for (const a of dom.window.document.getElementsByTagName("A")) {
    const href = a.getAttribute("href");
    if (href) {
      addTarget(href);
    }
  }

  const coll = new Intl.Collator(['und']);
  const anchorList = Array.from(anchors.values()).sort(coll.compare);
  const anchorFile = path.join(dirname, `${basename}.anchors.json`);
  await fs.writeFile(anchorFile, JSON.stringify(anchorList, null, '  '));
  const targetList = Array.from(targets.values()).sort(coll.compare);
  return [basename, anchorList, targetList];
}

/**
 * Convert all files
 * @returns Promise list of output files
 */
async function extractAll() {
  outbox = "./dist";

  const fileList = (await fs.readdir(outbox))
    .filter((f) => /\.html$/.test(f))
    .map((f) => path.join(outbox, f));
  return Promise.all(fileList.map(extractAnchors));
}

async function checkAll() {
  console.log(`${GEAR_ICON} Reading HTML`);
  const checked = await extractAll();
  console.log(`${GEAR_ICON} Collecting internal links`);

  const allInternalTargets = new Set();
  const allInternalAnchors = new Set();
  const sectionToTargets = {
    // e.g.  "tr35-info" : Set(["tr35-keyboards.md#Element_keyboard", …])
  };
  checked.forEach(([sourceSection,anchorList,targetList]) => {
    allInternalAnchors.add(constructLink(sourceSection)); // example: 'tr35-collation.md'
    targetList.forEach(target => allInternalTargets.add(target));
    sectionToTargets[sourceSection] = new Set(targetList); // for error checking
    const myInternalAnchors = anchorList.map(anchor => constructLink(sourceSection, anchor));
    myInternalAnchors.forEach(anchor => allInternalAnchors.add(anchor)); // tr35-collation.md#Parts
  });

  console.log(`${GEAR_ICON} Checking ${allInternalTargets.size} internal links against ${allInternalAnchors.size} anchors`);

  const missingInternalLinks = new Set();

  for (const expectedAnchor of allInternalTargets.values()) {
    if (!allInternalAnchors.has(expectedAnchor)) {
      missingInternalLinks.add(expectedAnchor);
    }
  }

  if (!!missingInternalLinks.size) {
    for (expectedAnchor of missingInternalLinks.values()) {
      // coalesce
      const sourceSections = ((Object.entries(sectionToTargets)
        .filter(([section,s]) => s.has(expectedAnchor))) // Does this section target this anchor?
        .map(([section]) => constructLink(section)) // drop the set
        .join(' & ') // join section name(s)
      ) || '(unknown section(s))'; // error
      console.error(`${MISSING_ICON} Broken internal link: ${sourceSections}: (${expectedAnchor})`);
    }
    console.error(`${WARN_ICON} ${missingInternalLinks.size} missing links.`);
    process.exitCode = 1;
  }

  console.log(`${POINT_ICON} use: 'lychee --cache docs/ldml' to check external links`);

  return checked.map(([anchorFile]) => anchorFile);
}
checkAll().then(
  (x) => x.forEach(section => {
    console.log(`${DONE_ICON} ${constructLink(section)}`);
  }),
  (e) => {
    console.error(e);
    process.exitCode = 1;
  }
);
