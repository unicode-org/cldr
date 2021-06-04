#!/usr/bin/env node

// For CLDR-14804
// Usage:
// node locale-coverage-to-localestxt.js < rawdata.json | tee Locales.txt
import { stdin } from "process";
import { generateLocalesTxt } from "../src/esm/localesTxtGenerator.mjs";

function readFileAsText(f) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    f.on("readable", () => {
      let chunk;
      while (null !== (chunk = f.read())) {
        chunks.push(chunk);
      }
    });
    f.on("end", () => resolve(chunks.join("")));
    f.on("error", reject);
  });
}

async function readFileAsJson(f) {
  const data = await readFileAsText(f);
  return JSON.parse(data);
}

// Main function
readFileAsJson(stdin)
  .then(generateLocalesTxt, (str) => console.log(str))
  .then((lines) => console.log(lines.join("\n")))
  .catch((err) => console.error(err));
