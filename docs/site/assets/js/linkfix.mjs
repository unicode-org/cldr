// link fixer-upper

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { traverse, mkdirNoisily, dstIsNewer } from "./utils.mjs";
import { marked } from "marked";


/**
 * Convert array of tokens to string
 * @param {object[]} tokens token array
 */
function fixTokens(srcTokens) {
    // TODO
}

function flattenTokens(srcTokens) {
    const out = [];
    for(const {type, raw, tokens} of srcTokens) {
        if (tokens && !raw) {
            // if the raw was deleted…
            out.push(flattenTokens(tokens));
        } else {
            out.push(raw);
        }
    }
    return out.join('');
}

/**
 * Fix links in one .md file
 * @param {string} srcPath
 * @param {string} dstPath
 */
async function linkFix(srcPath, dstPath) {
    if(srcPath  !== 'ddl.md') {
        return;
    } else {
        console.log("@@@ ", srcPath, dstPath);
    }

    if (await dstIsNewer(srcPath, dstPath)) {
        return;  // skip if unchanged
    }
    const str = await fs.readFile(srcPath, "utf-8");

    const tokens = marked.lexer(str);
    // const html = marked.parser(tokens);

    // DUMP
    // console.dir({ tokens /*, html*/ }, { depth: Infinity });

    fixTokens(tokens);

    const outStr = flattenTokens(tokens);
    if (str === outStr) {
        console.log(`# ${dstPath} [no change needed]`)
    } else {
        console.log(`# ${dstPath} [updated]`);
    }
    await fs.writeFile(dstPath, outStr, "utf-8");
}

/**
 * Sync from srcDir to dstDir, and fixup links
 * @param {string} srcDir
 * @param {string} dstDir
 */
export async function syncAndFixLinks(srcDir, dstDir) {
    const out = {};
    await traverse(srcDir, out, async (dirPath, filePath, out) => {
        await mkdirNoisily(path.join(dstDir, dirPath)); // mkdir each time
        await linkFix(filePath, path.join(dstDir, filePath));
    });
}
