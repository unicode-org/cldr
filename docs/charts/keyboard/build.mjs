// do the XML parsing and fs access in a build step

import { promises as fs } from "node:fs";
import * as path from "node:path";
import { XMLParser } from "fast-xml-parser";

const KEYBOARD_PATH = "../../../keyboards/3.0";
const IMPORT_PATH = "../../../keyboards/import";
const DATA_PATH = "static/data";

async function xmlList(basepath) {
  const dir = await fs.opendir(basepath);
  const xmls = [];
  for await (const ent of dir) {
    if (!ent.isFile() || !/\.xml$/.test(ent.name)) {
      continue;
    }
    xmls.push(ent.name);
  }
  return xmls;
}

/**
 * List of elements that are always arrays
 */
const alwaysArray = [
  "keyboard.transforms",
  "keyboard.transforms.transformGroup",
  "keyboard.transforms.transformGroup.transform",
];

/**
 * Loading helper for isArray
 * @param name
 * @param jpath
 * @param isLeafNode
 * @param isAttribute
 * @returns
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const isArray = (name, jpath, isLeafNode, isAttribute) => {
  if (alwaysArray.indexOf(jpath) !== -1) return true;
  return false;
};

/**
 * Do the XML Transform given raw XML source
 * @param xml XML source for transforms. entire keyboard file.
 * @param source source text
 * @returns target text
 */
export function parseXml(xml) {
  const parser = new XMLParser({
    ignoreAttributes: false,
    isArray,
  });
  const j = parser.parse(xml);
  return j;
}

async function readFile(path) {
  return fs.readFile(path, "utf-8");
}

async function main() {
  const xmls = await xmlList(KEYBOARD_PATH);
  const keyboards = await packXmls(KEYBOARD_PATH, xmls);
  const importFiles = await xmlList(IMPORT_PATH);
  const imports = await packXmls(IMPORT_PATH, importFiles);

  const allData = {
    keyboards,
    imports,
  };

  const outPath = path.join(DATA_PATH, "keyboard-data.json");
  const outJsPath = path.join(DATA_PATH, "keyboard-data.js");
  await fs.mkdir(DATA_PATH, { recursive: true });
  const json = JSON.stringify(allData, null, " "); // indent, in case we need to read it
  await fs.writeFile(outPath, json, "utf-8");
  await fs.writeFile(outJsPath, `const _KeyboardData = \n` + json);
  return { xmls, importFiles, outPath, outJsPath };
}

main().then(
  (done) => console.dir({ done }),
  (err) => {
    console.error(err);
    process.exitCode = 1;
  }
);

async function packXmls(basepath, xmls) {
  const allData = {};
  for (const fn of xmls) {
    const fp = path.join(basepath, fn);
    const data = await readFile(fp);
    const parsed = parseXml(data);
    allData[fn] = parsed;
  }
  return allData;
}
