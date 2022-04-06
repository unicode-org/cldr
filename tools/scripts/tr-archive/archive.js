const fs = require("fs").promises;
const marked = require("marked");
const jsdom = require("jsdom");
const { JSDOM } = jsdom;
const path = require("path");

// Setup some options for our markdown renderer
marked.setOptions({
  renderer: new marked.Renderer(),

  // Add a code highlighter
  highlight: function (code, forlanguage) {
    const hljs = require("highlight.js");
    language = hljs.getLanguage(forlanguage) ? forlanguage : "plaintext";
    return hljs.highlight(code, { language }).value;
  },
  pedantic: false,
  gfm: true,
  breaks: false,
  sanitize: false,
  smartLists: true,
  smartypants: false,
  xhtml: false,
});

/**
 * Read the input .md file, and write to a corresponding .html file
 * @param {string} infile path to input file
 * @returns {Promise<string>} name of output file (for status update)
 */
async function renderit(infile) {
  console.log(`Reading ${infile}`);
  basename = path.basename(infile, ".md");
  const outfile = path.join(path.dirname(infile), `${basename}.html`);
  let f1 = await fs.readFile(infile, "utf-8");

  // oh the irony of removing a BOM before posting to unicode.org
  if (f1.charCodeAt(0) == 0xfeff) {
    f1 = f1.substring(3);
  }

  // render to HTML
  const rawHtml = marked(f1);

  // now fix. Spin up a JSDOM so we can manipulate
  const dom = new JSDOM(rawHtml);
  const document = dom.window.document;

  // First the HEAD
  const head = dom.window.document.getElementsByTagName("head")[0];

  // add CSS to HEAD
  head.innerHTML =
    head.innerHTML +
    `<meta charset="utf-8">\n` +
    `<link rel='stylesheet' type='text/css' media='screen' href='../reports-v2.css'>\n`;

  // Assume there's not already a title and that we need to add one.
  if (dom.window.document.getElementsByTagName("title").length >= 1) {
    console.log("Already had a <title>… not changing.");
  } else {
    const title = document.createElement("title");
    const first_h1_text = document.getElementsByTagName("h1")[0].textContent.replace(')Part', ') Part');
    title.appendChild(document.createTextNode(first_h1_text))
    head.appendChild(title);
  }

  // calculate the header object
  const header = dom.window.document.createElement("div");
  header.setAttribute("class", "header");

  // taken from prior TRs, read from the header in 'header.html'
  header.innerHTML = (await fs.readFile('header.html', 'utf-8')).trim();

  // Move all elements out of the top level body and into a subelement
  // The subelement is <div class="body"/>
  const body = dom.window.document.getElementsByTagName("body")[0];
  const bp = body.parentNode;
  div = dom.window.document.createElement("div");
  div.setAttribute("class", "body");
  let sawFirstTable = false;
  for (const e of body.childNodes) {
    body.removeChild(e);
    if (div.childNodes.length === 0 && e.tagName === 'P') {
      // update title element to <h2 class="uaxtitle"/>
      const newTitle = document.createElement('h2');
      newTitle.setAttribute("class", "uaxtitle");
      newTitle.appendChild(document.createTextNode(e.textContent));
      div.appendChild(newTitle);
    } else {
      if (!sawFirstTable && e.tagName === 'TABLE') {
        // Update first table to simple width=90%
        // The first table is the document header (Author, etc.)
        e.setAttribute("class", "simple");
        e.setAttribute("width", "90%");
        sawFirstTable = true;
      }
      div.appendChild(e);
    }
  }

  /**
   * create a <SCRIPT/> object.
   * Choose ONE of src or code.
   * @param {Object} obj
   * @param {string} obj.src source of script as url
   * @param {string} obj.code code for script as text
   * @returns
   */
  function getScript({src, code})  {
    const script = dom.window.document.createElement("script");
    if (src) {
      script.setAttribute("src", src);
    }
    if (code) {
      script.appendChild(dom.window.document.createTextNode(code));
    }
    return script;
  }

  // body already has no content to it at this point.
  // Add all the pieces back.
  body.appendChild(getScript({ src: './js/anchor.min.js' }));
  body.appendChild(header);
  body.appendChild(div);

  // now, fix all links from  ….md#…  to ….html#…
  for (const e of dom.window.document.getElementsByTagName("a")) {
    const href = e.getAttribute("href");
    let m;
    if ((m = /^(.*)\.md#(.*)$/.exec(href))) {
      e.setAttribute("href", `${m[1]}.html#${m[2]}`);
    } else if ((m = /^(.*)\.md$/.exec(href))) {
      e.setAttribute("href", `${m[1]}.html`);
    }
  }

  // put this last
  body.appendChild(getScript({
    // This invokes anchor.js
    code: `anchors.add('h1, h2, h3, h4, h5, h6, caption');`
  }));

  // Now, fixup captions
  // Look for:  <h6>Table: …</h6> followed by <table>…</table>
  // Move the h6 inside the table, but as <caption/>
  const h6es = dom.window.document.getElementsByTagName("h6");
  const toRemove = [];
  for (const h6 of h6es) {
    if (!h6.innerHTML.startsWith("Table: ")) {
      console.error('Does not start with Table: ' + h6.innerHTML);
      continue; // no 'Table:' marker.
    }
    const next = h6.nextElementSibling;
    if (next.tagName !== 'TABLE') {
      console.error('Not a following table for ' + h6.innerHTML);
      continue; // Next item is not a table. Maybe a PRE or something.
    }
    const caption = dom.window.document.createElement("caption");
    for (const e of h6.childNodes) {
      // h6.removeChild(e);
      caption.appendChild(e.cloneNode(true));
    }
    for (const p of h6.attributes) {
      caption.setAttribute(p.name, p.value);
      h6.removeAttribute(p.name); // so that it does not have a conflicting id
    }
    next.prepend(caption);
    toRemove.push(h6);
  }
  for (const h6 of toRemove) {
    h6.remove();
  }

  // OK, done munging the DOM, write it out.
  console.log(`Writing ${outfile}`);

  // TODO: we assume that DOCTYPE is not written.
  await fs.writeFile(outfile, `<!DOCTYPE html>\n`
                              + dom.serialize());
  return outfile;
}

/**
 * Convert all files
 * @returns Promise<String[]> list of output files
 */
async function fixall() {
  outbox = "./dist";

  // TODO: move source file copy into JavaScript?
  // srcbox = '../../../docs/ldml';

  const fileList = (await fs.readdir(outbox))
    .filter((f) => /\.md$/.test(f))
    .map((f) => path.join(outbox, f));
  return Promise.all(fileList.map(renderit));
}

fixall().then(
  (x) => console.dir(x),
  (e) => {
    console.error(e);
    process.exitCode = 1;
  }
);
