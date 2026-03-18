// import { fork } from "node:child_process";
import { fixAllTocs } from "./fix-tocs.js";

const DIST_DIR="dist/";

async function buildAll() {
    await fixAllTocs();
}


buildAll()
    .then(x => console.log(`Built into ${DIST_DIR}`),
        err => { console.error(err); process.exitCode = 1; });
