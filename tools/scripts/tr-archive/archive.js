const fs = require("fs").promises;
const { marked } = require("marked");
const jsdom = require("jsdom");
const { JSDOM } = jsdom;
const path = require("path");
const markedAlert = require("marked-alert");
const matter = require("gray-matter");
const AnchorJS = require("anchor-js");

// this one is closer to GitHub, it seems.
const amh = require("anchor-markdown-header");

// array of elements to put anhors on
const ELEMENTS = "h2 h3 h4 h5 h6 caption dfn".split(" ");

// Not great, but do this so AnchorJS will work
global.document = new jsdom.JSDOM(`...`).window.document;
const anchorjs = new AnchorJS();

/** give the anchor as Anchor.js would do it */
function anchorurlify(t) {
  // undocumented
  return anchorjs.urlify(t);
}

/** give the anchor as GFM probably would do it */
function gfmurlify(t) {
  const md = amh(t); // '[message.abnf](#messageabnf)'
  return /.*\(#([^)]+)\)/.exec(md)[1];
}

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

marked.use(markedAlert());

/**
 * Read the input .md file, and write to a corresponding .html file
 * @param {string} infile path to input file
 * @returns {Promise<string>} name of output file (for status update)
 */
async function renderit(infile) {
  const gtag = (await fs.readFile("gtag.html", "utf-8")).trim();
  console.log(`Reading ${infile}`);
  const basename = path.basename(infile, ".md");
  const outfile = path.join(path.dirname(infile), `${basename}.html`);
  let f1 = await fs.readFile(infile, "utf-8");
  // any metadata on the file?
  const { data, content } = matter(f1);

  f1 = content; // skip the frontmatter (YAML block within ---)

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
    "\n" +
    gtag +
    "\n" +
    `<meta charset="utf-8">\n` +
    `<link rel='stylesheet' type='text/css' media='screen' href='../reports-v2.css'>\n` +
    `<link rel='stylesheet' type='text/css' media='screen' href='css/tr35.css'>\n`;

  // Assume there's not already a title and that we need to add one.
  if (dom.window.document.getElementsByTagName("title").length >= 1) {
    console.log("Already had a <title>… not changing.");
  } else {
    const title = document.createElement("title");
    const first_h1_text = document
      .getElementsByTagName("h1")[0]
      .textContent.replace(")Part", ") Part");
    title.appendChild(document.createTextNode(first_h1_text));
    head.appendChild(title);
  }

  // calculate the header object
  const header = dom.window.document.createElement("div");
  header.setAttribute("class", "header");

  // taken from prior TRs, read from the header in 'header.html'
  header.innerHTML = (await fs.readFile("header.html", "utf-8")).trim();

  // Move all elements out of the top level body and into a subelement
  // The subelement is <div class="body"/>
  const body = dom.window.document.getElementsByTagName("body")[0];
  const bp = body.parentNode;
  div = dom.window.document.createElement("div");
  div.setAttribute("class", "body");
  let sawFirstTable = false;
  for (const e of body.childNodes) {
    body.removeChild(e);
    if (div.childNodes.length === 0 && e.tagName === "P") {
      // update title element to <h2 class="uaxtitle"/>
      const newTitle = document.createElement("h2");
      newTitle.setAttribute("class", "uaxtitle");
      newTitle.appendChild(document.createTextNode(e.textContent));
      div.appendChild(newTitle);
    } else {
      if (!sawFirstTable && e.tagName === "TABLE") {
        // Update first table to simple width=90%
        // The first table is the document header (Author, etc.)
        e.setAttribute("class", "simple");
        e.setAttribute("width", "90%");
        sawFirstTable = true;
      }
      div.appendChild(e);
    }
  }
  body.innerHTML = body.innerHTML.trim(); // trim whitespae

  /**
   * create a <SCRIPT/> object.
   * Choose ONE of src or code.
   * @param {Object} obj
   * @param {string} obj.src source of script as url
   * @param {string} obj.code code for script as text
   * @returns
   */
  function getScript({ src, code }) {
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
  body.appendChild(document.createTextNode("\n\n  "));
  body.appendChild(document.createComment(`Header from header.html`));
  body.appendChild(document.createTextNode("\n"));
  body.appendChild(header);
  body.appendChild(document.createTextNode("\n\n  "));
  body.appendChild(document.createComment(`Converted from ${basename}.md`));
  body.appendChild(document.createTextNode("\n"));
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

  body.appendChild(document.createTextNode("\n\n  "));
  body.appendChild(document.createComment("additional scripts and fixups"));
  body.appendChild(document.createTextNode("\n  "));

  body.appendChild(getScript({ src: "./js/anchor.min.js" }));
  body.appendChild(document.createTextNode("\n  "));
  body.appendChild(getScript({ src: "./js/tr35search.js" }));
  body.appendChild(document.createTextNode("\n  "));

  // put this last
  body.appendChild(
    getScript({
      // This invokes anchor.js
      code: `anchors.add('${ELEMENTS.join(", ")}');`,
    })
  );
  body.appendChild(document.createTextNode("\n"));

  // Now, fixup captions
  // Look for:  <h6>Table: …</h6> followed by <table>…</table>
  // Move the h6 inside the table, but as <caption/>
  const h6es = dom.window.document.getElementsByTagName("h6");
  const toRemove = [];
  for (const h6 of h6es) {
    if (!h6.innerHTML.startsWith("Table: ")) {
      console.error("Does not start with Table: " + h6.innerHTML);
      continue; // no 'Table:' marker.
    }
    const next = h6.nextElementSibling;
    if (next.tagName !== "TABLE") {
      console.error("Not a following table for " + h6.innerHTML);
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

  // Drop generated anchors where there is an explicit anchor
  const anchors = dom.window.document.getElementsByTagName("a");
  for (const a of anchors) {
    // a needs to have a name
    const aname = a.getAttribute("name");
    if (!aname) continue;
    // parent needs to have a single child node and its own 'id'.
    const parent = a.parentElement;
    if (parent.childElementCount !== 1) continue;
    const parid = parent.getAttribute("id");
    if (!parid) continue;
    // Criteria met. swap the name and id
    parent.setAttribute("id", aname);
    a.setAttribute("name", parid);
  }

  // If the document requests it, linkify terms
  if (data.linkify) {
    linkify(dom.window.document);
  }

  // find any link ids that are likely to be mismatches with GFM
  // Workaround: https://github.com/bryanbraun/anchorjs/issues/197
  for (const tag of ELEMENTS) {
    for (const e of dom.window.document.getElementsByTagName(tag)) {
      const id = e.getAttribute("id");
      if (id) continue; // skip elements that already have an id
      const txt = e.textContent.trim();
      const anchor_id = anchorurlify(txt);
      const gfm_id = gfmurlify(txt);
      if (anchor_id !== gfm_id) {
        // emit fixups
        // console.log({ txt, gfm_id, anchor_id });
        if (dom.window.document.getElementById(gfm_id)) {
          console.error(`${basename}: duplicate id ${gfm_id}`);
        } else {
          e.setAttribute("id", gfm_id);
        }
      }
      // add the 'original' casing as an anchor, i.e. "Some Thing" -> "#Some_Thing" vs "some-thing" if it doesn't already exist
      if (txt !== gfm_id
        && /^[a-zA-Z -]+$/.test(txt)) {
        const n = txt.replace(/ /g, '_');
        if (!dom.window.document.getElementById(n)
          && !(dom.window.document.getElementsByName(n)?.length)) {

          //console.log({ txt, gfm_id, n });
          const origAnchor = document.createElement("a");
          origAnchor.setAttribute("name", n);
          origAnchor.setAttribute("x-orig-casing", "true");
          e.prepend(origAnchor);
        }
      }
    }
  }

  // OK, done munging the DOM, write it out.
  console.log(`Writing ${outfile}`);

  // TODO: we assume that DOCTYPE is not written.
  await fs.writeFile(outfile, `<!DOCTYPE html>\n` + dom.serialize());
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

function linkify(document) {
  const terms = findTerms(document);
  const missing = new Set();
  const used = new Set();
  const links = document.querySelectorAll("em");

  links.forEach((item) => {
    const target = generateId(item.textContent);
    if (terms.has(target)) {
      const el = item.lastElementChild ?? item;
      el.innerHTML = `<a href="#${target}">${item.textContent}</a>`;

      used.add(target);
    } else {
      missing.add(target);
    }
  });

  if (missing.size > 0) {
    console.log("Potentially missing definitions:");
    Array.from(missing)
      .sort()
      .forEach((item) => {
        console.log(item);
      });
  }

  if (terms.size === used.size) return;
  console.log("Some definitions were not used:");
  Array.from(terms).forEach((item) => {
    if (!used.has(item)) {
      console.log(item);
    }
  });
}

function findTerms(document) {
  const terms = new Set();
  let duplicateCount = 0;
  document.querySelectorAll("dfn").forEach((item) => {
    const term = generateId(item.textContent);
    if (term.length === 0) return; // skip empty terms
    if (terms.has(term)) {
      console.log(`Duplicate term: ${term}`);
      duplicateCount++;
    }
    terms.add(term);
    item.setAttribute("id", term);
  });

  if (duplicateCount > 0) {
    console.log("Duplicate Terms: " + duplicateCount);
  }
  return terms;
}

function generateId(term) {
  const id = term.toLowerCase().replace(/\s+/g, "-"); // Replaces spaces safely
  // TODO: do better than hardcoding the one case in message-format
  if (id.endsWith("rategies")) {
    return id.slice(0, -3) + "y";
  } else if (id.endsWith("s") && id !== "status") {
    return id.slice(0, -1);
  }
  return id;
}
