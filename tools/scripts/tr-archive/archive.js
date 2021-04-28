const fs = require("fs").promises;
const marked = require("marked");
const jsdom = require("jsdom");
const { JSDOM } = jsdom;
const path = require("path");

marked.setOptions({
  renderer: new marked.Renderer(),
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

async function renderit(infile) {
  console.log(`Reading ${infile}`);
  basename = path.basename(infile, ".md");
  const outfile = path.join(path.dirname(infile), `${basename}.html`);
  let f1 = await fs.readFile(infile, "utf-8");
  // oh the irony
  if (f1.charCodeAt(0) == 0xfeff) {
    f1 = f1.substring(3);
  }

  // render
  const rawHtml = marked(f1);

  // now fix
  const dom = new JSDOM(rawHtml);
  const document = dom.window.document;
  const head = dom.window.document.getElementsByTagName("head")[0];

  // add CSS
  head.innerHTML =
    head.innerHTML +
    `<meta charset="utf-8">` +
    `<link rel='stylesheet' type='text/css' media='screen' href='../reports.css'>`;

  // Move all elements out of the top level body and into a subelement
  const body = dom.window.document.getElementsByTagName("body")[0];
  const bp = body.parentNode;
  div = dom.window.document.createElement("div");
  div.setAttribute("class", "body");
  for (const e of body.childNodes) {
    body.removeChild(e);
    div.appendChild(e);
  }
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

  // OK, done munging the DOM, write it out.
  console.log(`Writing ${outfile}`);
  await fs.writeFile(outfile, dom.serialize());
  return outfile;
}

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
  (e) => console.error(e)
);
