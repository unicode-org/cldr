/**
 * @param {Document} document
 * @param any c
 * @returns {Node}
 */
export function createTableCell(document, c) {
    const td = document.createElement("td");
    if (typeof c === "string") {
        td.append(document.createTextNode(c));
    } else {
        td.appendChild(c); // assume it's a node-ish
    }
    return td;
}

export function createLink(document, href, txt) {
    const a = document.createElement("a");
    a.setAttribute("href", href);
    if(!txt) txt = href;
    a.appendChild(document.createTextNode(txt));
    return a;
}

export function createEmailNode(document, name, email, subject) {
    if (!email) return document.createTextNode(name);
    let mailurl = `mailto:${email}`;
    if (subject) {
        mailurl = mailurl + `?subject=${subject}`;
    }
    const frag = document.createDocumentFragment();
    frag.appendChild(document.createTextNode(`${name} (`));
    frag.appendChild(createLink(document, mailurl, email));
    frag.appendChild(document.createTextNode(`)`));
    return frag;
}

/**
 * @param {Document} document
 * @param any col1
 * @param any col2
 * @returns {Node}
 */
export function createTableRow(document, col1, col2) {
    const tr = document.createElement("tr");
    if (col1) {
        tr.appendChild(createTableCell(document, col1));
        if (col2) {
            tr.appendChild(createTableCell(document, col2));
        }
    }
    return tr;
}

/**
 * @param document {Document}
 */
export function createEditors(document, editors, subject) {
    if (!editors) editors = [];
    const frag = document.createDocumentFragment();
    const lf = new Intl.ListFormat("en-001", {style: "long", type: "conjunction"});
    const l = [...editors.map(o => JSON.stringify(o)), '{"others":true}'];
    lf.formatToParts(l)
    .forEach(({type, value}) => {
        if (type === 'literal') {
            frag.appendChild(document.createTextNode(value));
        } else {
            const {others, name, email} = JSON.parse(value);
            if (others) {
                frag.appendChild(createLink(document, "tr35-acknowledgments.html#acknowledgments", "other CLDR committee members"));
            } else {
                frag.appendChild(createEmailNode(document, name, email, subject));
            }
        }
    });
    return frag;
}

/**
 * @param info part1 metadata
 * @param data this part's metadata
 * @param document {Document}
 * @returns Table
 */
export function createHeaderTable(info, data, document) {
    const table = document.createElement("table");
    table.setAttribute("class", "simple");
    table.setAttribute("width", "90%");
    const tbody = document.createElement("tbody");
    // if not final, add the status
    const versionSuffix = (info.status !== 'final')?` ${info.status}`:'';
    function addRow(k, v) {
        const r = createTableRow(document, k, v);
        tbody.appendChild(r);
        return r;
    }
    const subject=`Unicode%20CLDR%20tr35-${info.revision}/${data.basename}`;
    addRow("CLDR Version", info.version + versionSuffix).setAttribute("class", "boldRow");
    if (data.part) {
        addRow("Part", `${data.part}: ${data.title}`).setAttribute("class", "boldRow");
    } else if(data.appendix) {
        addRow("Appendix", `${data.appendix}: ${data.title}`).setAttribute("class", "boldRow");
    }
    addRow("Editors", createEditors(document, data.editors, subject)).setAttribute("class", "boldRow");
    addRow("Date", info.date).setAttribute("class", "boldRow");
    if (data.part) {
        addRow("This Revision", createLink(document, `${info.latest}/tr35-${info.revision}/${data.basename}.html`));
        addRow("Previous Revision", createLink(document, `${info.latest}/tr35-${info.prevRevision}/${data.basename}.html`));
        addRow("Corrigenda", createLink(document, info.corrigenda));
        addRow("Latest Proposed Update", createLink(document, info.latestProposedUpdate));
    }
    if (data.part) {
        addRow("Namespace", createLink(document, info.namespace));
        addRow("DTDs", createLink(document, `${info.dtd}/${info.version}/`));
        addRow("Change History", createLink(document, `./tr35-modifications.html#modifications`, 'Modifications'));
    }
    table.appendChild(tbody);
    return table;
}
