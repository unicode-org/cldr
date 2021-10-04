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

  // // setup doctype
  // if (document.doctype) {
  //   console.log("have a doctype " + document.doctype);
  // } else {
  //   document.doctype = document.implementation.createDocumentType("html","","");
  //   document.insertBefore(document.childNodes[0], document.doctype);
  // }

  const head = dom.window.document.getElementsByTagName("head")[0];

  // add CSS
  head.innerHTML =
    head.innerHTML +
    `<meta charset="utf-8">` +
    `<link rel='stylesheet' type='text/css' media='screen' href='../reports-v2.css'>`;

  // Is there a title?
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
  // taken from prior TRs
  header.innerHTML = `<table class="header" cellpadding="0" cellspacing="0" width="100%">
  <tbody>
      <tr>
          <td class="icon"><a href="http://www.unicode.org/"><img style="vertical-align:middle;border:0" alt="[Unicode]"
                        src="http://www.unicode.org/webscripts/logo60s2.gif"
                        height="33"
                        width="34" /></a>  <a class="bar" href="http://www.unicode.org/reports/">Technical Reports</a></td>
      </tr>
      <tr>
          <td class="gray"> </td>
      </tr>
  </tbody>
  </table>`;

  // Move all elements out of the top level body and into a subelement
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
        e.setAttribute("class", "simple");
        e.setAttribute("width", "90%");
        sawFirstTable = true;
      }
      div.appendChild(e);
    }
  }
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

  // OK, done munging the DOM, write it out.
  console.log(`Writing ${outfile}`);
  // TODO: assume that DOCTYPE is not written.
  await fs.writeFile(outfile, `<!DOCTYPE html>\n` + dom.serialize());
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
  (e) => {
    console.error(e);
    process.exitCode = 1;
  }
);
