import { zipSync } from "cross-zip";
import { join } from "node:path";
import { fixAllTocs } from "./fix-tocs.js";
import { archive } from "./archive.js";
import { copyFile, mkdir, cp, unlink } from "node:fs/promises";

const DIST_DIR="dist/";

const JS_DIR = join(DIST_DIR, "js/");

function cpAndLog(src, dst) {
    console.log(`- cp ${src} ${dst}`);
    return copyFile(src, dst);
}

function mkdirAndLog(dst) {
    console.log(`- mkdir -p ${dst}`);
    return mkdir(dst, {recursive: true});
}

function cpDir(src, dst) {
    console.log(`- cp ${src}* ${dst}`);
    return cp(src, dst, {errorOnExist:false, force: true, recursive: true});
}

export async function buildAll() {
    console.log("# Pre-work: Update ToC (and validate) ----------");
    await Promise.all([
        fixAllTocs(),
        mkdirAndLog(JS_DIR)
    ]);
    console.log("# Copy some stuff into place -------------------");
    await Promise.all([
        await cpAndLog("node_modules/anchor-js/anchor.min.js", join(JS_DIR, "anchor.min.js")),
        await cpDir("../../../docs/ldml/", DIST_DIR),
        await cpDir("assets/", DIST_DIR),
        await cpAndLog("../../../LICENSE", join(DIST_DIR, "LICENSE"))
    ]);
    // cleanup (ignored)
    await unlink(join(DIST_DIR, ".markdownlint.json")).catch(e => {});
    console.log("# Convert md->html (archive) -------------------");
    const meta = await archive();
    console.log(`#--archiver done, built tr35-${meta.info.revision}---------------------------------------------`);
    // Whew. What's next.
    // console.dir(meta);
    const outname = `tr35-${meta.info.revision}.zip`;
    console.log(`# Building ${outname}`);
    zipSync(DIST_DIR, outname);
    cp(outname, "tr35.zip"); // in case someone is looking for the old filename
    return {meta, outname};
}

buildAll()
    .then(({outname}) => console.log(`## DONE: Built into ${DIST_DIR} and ${outname}`),
        err => { console.error(err); console.error("# Build failed."); process.exitCode = 1; });
