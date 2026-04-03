// link fixer-upper

import * as fs from "node:fs/promises";
import * as path from "node:path";
import { traverse, mkdirNoisily, dstIsNewer, isExternalLink, isMarkdownLink, SKIP_THESE } from "./utils.mjs";
import { marked } from "marked";
// import { link } from "node:fs";


/**
 * Recursively fix an array of tokens
 * @param {Object[]} srcTokens array of source tokens
 * @returns true if any fix was needed
 */
function fixTokens(srcTokens) {
    // were any children fixed?
    let didFix = false;
    for(let i in srcTokens) {
        const t = srcTokens[i];
        const {type, raw, text, href, tokens} = t;

        // recurse

        if (type === 'link') {
            // TODO: lint here CLDR-18011
            // if (isSiteRelativeLink(href)) {
            //     throw Error(`Err! Site Relative Link ${href}, use relative link to .md file instead`);
            // }
            // TODO: lint here CLDR-18011
            // if (isRelativeLink(href) && !isMarkdownLink(href)) {
            //     throw Error(`Err! Relative Link ${href}, use relative link to ${href}.md instead`);
            // }
            if (!isExternalLink(href) && isMarkdownLink(href)) {
                didFix = true; // so that the parent re-renders
                //DUMP //console.dir({type, raw, href});

                t.href = href.slice(0, href.length-3); // remove .md

                // confirm the sub-tokens situation
                if (tokens.length !== 1 || tokens[0].type !== 'text') {
                    throw Error(`Unexpected link children tokens:${JSON.stringify(tokens)} in ${t.raw}`);
                }
                t.tokens = [];
                // now, re-render this link
                t.raw = `[${text}](${t.href})`;
            } else {
                // no fixup needed for this link.
                // we aren't processing subtokens here, however, they should not be needed,
                // since t.raw remains valid
            }
        } else if (fixTokens(tokens)) {
            didFix = true; // so parents get re-rendered
            if (type === 'paragraph') {
                delete t.raw; // recombine this paragraph
            } else {
                throw Error(`Can't fixup parent type ${type}: in ${t.raw}`);
            }
        }
    }
    return didFix; // propagate up
}

/**
 * Convert array of tokens to string
 * The convention is that if 'raw' is unset, then 'tokens' is used.
 * @param {object[]} tokens token array
 * @returns markdown source
 */
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
    const str = (await fs.readFile(srcPath, "utf-8")).replaceAll(/\r\n/g, '\n');

    const tokens = marked.lexer(str);

    const didFix = fixTokens(tokens);

    // DUMP console.dir({ tokens }, { depth: Infinity });

    const outStr = flattenTokens(tokens);
    // TODO: READ the prev file, don't rewrite if unchanged. CLDR-18011
    let existingText;
    try {
        existingText = await fs.readFile(dstPath, "utf-8");
    } catch(e) {
        existingText = null;
    }

    if (outStr === existingText) {
        // File same content, no rewrite needed
    } else if (str === outStr) {
        // console.log(`# ${dstPath} [no change needed] ${didFix}`)
    } else {
        console.log(`# ${dstPath} [updated] ${didFix}`);
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
    await traverse(srcDir, out, async (dirPath, srcPath, out, e) => {
        await mkdirNoisily(path.join(dstDir, dirPath)); // mkdir each time
        const dstPath = path.join(dstDir, srcPath);
        if (await dstIsNewer(srcPath, dstPath)) {
            return;  // skip if unchanged
        }
        if (!SKIP_THESE.test(srcPath) && e.name.endsWith(".md")) {
            await linkFix(srcPath, dstPath);
        } else {
            fs.copyFile(srcPath, dstPath);
        }
}, null);
}
