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

/**
 * Read the input .md file, and write to a corresponding .html file
 * @param {string} infile path to input file
 * @returns {Promise<string>} name of output file (for status update)
 */
async function extractAnchors(infile) {
  const basename = path.basename(infile, ".html");
  dirname = '../../../docs/ldml';
  console.log(`Reading ${infile}`);
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

  function addAnchor(n) {
    if (!n) return;
    if (anchors.has(n)) {
      console.error(`${basename}: Duplicate anchor: ${n}`);
    } else {
      anchors.add(n);
    }
  }

  for (const a of dom.window.document.getElementsByTagName("*")) {
    const id = a.getAttribute("id");
    addAnchor(id);

    if (a.tagName === 'A') {
      const name = a.getAttribute("name");
      addAnchor(name);
    }
  }
  const coll = new Intl.Collator(['und']);
  const anchorList =  Array.from(anchors.values()).sort(coll.compare);
  const anchorFile = path.join(dirname, `${basename}.anchors.json`);
  await fs.writeFile(anchorFile, JSON.stringify(anchorList, null, '  '));
  return anchorFile;
}

/**
 * Convert all files
 * @returns Promise<String[]> list of output files
 */
async function checkAll() {
  outbox = "./dist";

  // TODO: move source file copy into JavaScript?

  const fileList = (await fs.readdir(outbox))
    .filter((f) => /\.html$/.test(f))
    .map((f) => path.join(outbox, f));
  return Promise.all(fileList.map(extractAnchors));
}

checkAll().then(
  (x) => console.dir(x),
  (e) => {
    console.error(e);
    process.exitCode = 1;
  }
);
