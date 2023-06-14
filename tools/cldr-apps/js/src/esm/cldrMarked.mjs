import { marked } from "marked";

// see archive.js in the tr-archiver directory

const renderer = new marked.Renderer();

marked.setOptions({
  renderer,
  pedantic: false,
  gfm: true,
  breaks: false,
  sanitize: false,
  smartLists: true,
  smartypants: false,
  xhtml: false,
});

// make links go to a new window
const oldLink = renderer.link;

renderer.link = function (href, title, text) {
  // call the old link renderer
  const oldHtml = oldLink.call(this, href, title, text);
  // replace with _blank
  return oldHtml.replace(/^<a /, `<a target='_blank' `);
};

export { marked };
