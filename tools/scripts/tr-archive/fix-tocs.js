// Run this to update the ToCs in the TRs

const { generateGfmToc } = require('@not-dalia/gfm-toc');
const fs = require('fs').promises;
const path = require('path');

const dir = '../../../docs/ldml';
const trfile = /^tr35.*\.md$/;

const contentsStart = /^## <a name="Contents".*$/;  // beginning of contents: always has #Contents
const contentsEnd = /^## .*$/; // end of contents: the next ##

// ToC entries we don't want, for deletion
const tocDelete = [
    /^\* \[.*Unicode Technical Standard.*$/,
    /^  \* \[_Summary_\].*$/,
    /^  \* \[_Status_\].*$/,
    /^\* \[Parts\].*$/,
    /^\* \[Contents of.*$/
];

const gfmOpts = {
    // see gfm-toc docs
    includeUnlinked: true,
    createLinks: true,
};

/**
 *
 * @returns promise to array of source files
 */
async function getSrcFiles() {
    const f = [];
    const items = await fs.opendir(dir);
    for await (const dirent of items) {
        if (trfile.test(dirent.name)) {
            f.push(path.join(dir, dirent.name));
        }
    }
    return f;
}

/**
 * Process a single file
 * @param {String} f
 * @returns
 */
async function processFile(f) {
    console.log('Reading: ' + f);
    const contents = await fs.readFile(f, 'utf-8');

    // now, reinsert
    const lines = contents.split('\n');

    // new lines go into this array.
    const out = [];

    let i;

    // go through the lines, looking for the header to the old ToC.
    for (i = 0; i < lines.length; i++) {
        out.push(lines[i]); // Emit the header line for the old ToC
        if (contentsStart.test(lines[i])) {
            break;
        }
    }
    if (i == lines.length) {
        throw Error(`in ${f}: ran out of lines looking for start of ToC`);
    }
    i++;
    out.push(''); // blank line before ToC

    // Generate the ToC
    let toc = generateGfmToc(contents, gfmOpts);

    // Delete any patterns in tocDelete from the ToC
    for (pat of tocDelete) {
        if (pat.test(toc[0])) {
            toc = toc.splice(1); // delete first entry
        }
    }

    // Push the whole ToC out
    out.push(toc.join('\n'));
    out.push('');

    // Now, look for the end of the old ToC
    // (the next section following the old ToC)
    for (; i < lines.length; i++) {
        if (contentsEnd.test(lines[i])) {
            break;
        }
    }
    if (i == lines.length) {
        throw Error(`in ${f}: ran out of lines looking for end of ToC`);
    }
    // Write out all remaining lines in the file.
    for (; i < lines.length; i++) {
        out.push(lines[i]);
    }

    // Write the whole file to disk.
    await fs.writeFile(f, out.join('\n'), 'utf-8');

    return {
        name: path.basename(f),
        lines: out.length,
        toclines: toc.length
    };
}

// Process everything.

getSrcFiles()
    .then(f => Promise.all(f.map(p => processFile(p))))
    .then(x => console.dir(x), console.error);
