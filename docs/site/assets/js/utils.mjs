// utility functions

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { Dirent } from "node:fs";

// files to skip
export const SKIP_THESE = /(node_modules|\.jekyll-cache|^sitemap.tsv)/;

/** replace a/b/c.md with a/b */
export function path2dir(p) {
  const dir = p.split("/").slice(0, -1).join("/");
  return dir;
}

/** replace a/b/c.md with a/b/c.html */
export function md2html(p) {
  return p.replace(/\.md$/, ".html");
}

/** replace a/b/c.html with a/b/c.md */
export function html2md(p) {
  return p.replace(/\.html$/, ".md");
}

/** replace a/b/c.md with a/b/c */
export function dropmd(p) {
  return p.replace(/\.md$/, "");
}

/**
 *
 * @param {number} n
 * @returns string with n tabs
 */
export function tabs(n) {
  let s = [];
  for (let i = 0; i < n; i++) {
    s.push("\t");
  }
  return s.join("");
}


/**
 * Directory Crawler: process one dirent
 * @param {string} d directory paren
 * @param {object} out output object
 * @param {Dirent} e directory entry
 * @param {Function} fn called with .md files: (dirPath: string, filePath: string, out: output collection)
 * @returns
 */
export async function processEntry(d, out, e, fn) {
  const fullpath = path.join(d, e.name);
  if (SKIP_THESE.test(e.name)) return;
  if (e.isDirectory()) {
    // recurse
    return await traverse(fullpath, out, fn);
  } else if (!e.isFile() || !/\.md$/.test(e.name)) {
    return;
  }
  return fn(d, fullpath, out);
}

/**
 * Directory Crawler: kick off the crawl (or subcrawl) of a directory
 * @param {string} d path to directory
 * @param {object} out output struct
 * @param
 */
export async function traverse(d, out, fn) {
  const dirents = await fs.readdir(d, { withFileTypes: true });
  const promises = dirents.map((e) => processEntry(d, out, e, fn));
  return Promise.all(promises);
}

export async function mkdirNoisily(dirPath) {
    const createDir = await fs.mkdir(dirPath, { recursive: true });
    if (createDir) {
        console.log(`# mkdir ${createDir}`);
    }
}

export async function dstIsNewer(srcPath, dstPath) {
  return false; // TODO
}
