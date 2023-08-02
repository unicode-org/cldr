import * as XLSX from "xlsx";
import * as cldrLoad from "./cldrLoad.mjs";

/**
 * Read an XLSX (or whatever) file.
 * @param {File} file file object
 * @param {Function} cb callback for returning data: (err, wb)
 * @returns Promise
 */
function xlsUpload(file, cb) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        // todo: err?
        const data = new Uint8Array(reader.result);
        const wb = XLSX.read(data, { type: "array" });
        cb(null, wb);
        return resolve;
      } catch (err) {
        cb(err);
        return reject(err);
      }
    };
    reader.readAsArrayBuffer(file);
  });
}

/**
 * read the header row from the sheet
 * @param {XLSX.WorkBook} wb
 */
function xlsHeaders(wb) {
  if (!wb) throw Error(`No workbook`);
  const sheet = wb.Sheets[wb.SheetNames[0]];
  const headers = [];

  for (let c = 0; ; c++) {
    const cell_ref = XLSX.utils.encode_cell({ c, r: 0 });
    if (!sheet[cell_ref]?.v) {
      break;
    }
    headers.push(sheet[cell_ref].v);
  }
  return headers;
}

function xlsGenerateXml(locale, wb, xColumn, vColumn) {
  if (!wb) throw Error(`No workbook`);
  const sheet = wb.Sheets[wb.SheetNames[0]];
  const headers = xlsHeaders(wb);
  const xNumber = headers.findIndex((v) => v === xColumn); // col # for XPath
  const vNumber = headers.findIndex((v) => v === vColumn); // col # for Value

  if (xNumber === -1 || vNumber === -1 || xNumber === vNumber) {
    throw Error(`xpath and value columns must be present and distinct`);
  }

  const locmap = cldrLoad.getTheLocaleMap();
  const info = locmap.getLocaleInfo(locale);
  const { bcp47, name } = info;
  const { language, script, region, variant } = new Intl.Locale(bcp47);

  // ok make it up.
  // skeletal XML for ordering
  const xml = `<?xml version="1.0" encoding="UTF-8" ?>
<!-- Don't change the DOCTYPE. -->
<!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">
<!--
 Generated ${new Date().toLocaleDateString()} using cldrBulkConverter from a spreadsheet
 Locale: ${locale} / ${bcp47} / ${name}
-->
<ldml>
	<identity>
        <version number="$Revision$"/><!-- don't remove this.-->
    </identity>
</ldml>`;

  const xparser = new DOMParser();
  const doc = xparser.parseFromString(xml, "text/xml");

  /** create a dom tag */
  function newTag(tag, content, attrs) {
    const n = doc.createElement(tag);
    // add any content
    if (content) {
      const t = doc.createTextNode(content);
      n.appendChild(t);
    }
    if (attrs) {
      setAttrs(attrs, n);
    }
    return n;
  }

  function setAttrs(a, newChild) {
    for (const [k, v] of Object.entries(a)) {
      newChild.setAttribute(k, v);
    }
    return newChild;
  }

  /** extract attrs to map<string,string> */
  function nodeAttrMap(n) {
    const attrs = {};
    for (const a of n.attributes) {
      attrs[a.name] = a.value;
    }
    return attrs;
  }

  /** half of a deep equality */
  function matchHalf(a, b) {
    for (const [k, v] of Object.entries(a)) {
      // TODO: skip excluded
      if (b[k] !== v) return false;
    }
    return true;
  }
  /** deep equality for Map<string,string> */
  function matchAttrs(a, b) {
    return matchHalf(a, b) && matchHalf(b, a);
  }

  /** find mathing subnode */
  function findSubNode(parent, tag, attrs) {
    for (const n of parent.children) {
      if (!n.nodeType === Node.ELEMENT_NODE) continue;
      if (n.nodeName != tag) continue;
      // check attrs
      const subAttrs = nodeAttrMap(n);
      if (!matchAttrs(attrs, subAttrs)) {
        continue;
      }
      return n;
    }
    return null;
  }

  // setup identity
  const ident = doc.getElementsByTagName("identity")[0];
  if (language) {
    ident.appendChild(newTag("language", null, { type: language }));
  }
  if (script) {
    ident.appendChild(newTag("script", null, { type: script }));
  }
  if (region) {
    ident.appendChild(newTag("region", null, { type: region }));
  }
  if (variant) {
    ident.appendChild(newTag("variant", null, { type: variant })); // may be wrong for valencia etc.
  }

  // now, walk each row
  for (let r = 1; ; r++) {
    const xpath = sheet[XLSX.utils.encode_cell({ c: xNumber, r })]?.v;
    const value = sheet[XLSX.utils.encode_cell({ c: vNumber, r })]?.v;
    if (!xpath) break;
    if (!value) {
      // skip missing value
      continue;
    }
    if (!xpath.startsWith("//ldml")) {
      throw Error(`Invalid xpath on row ${r} - ${xpath}`);
    }

    // now the fun part
    // //ldml/localeDisplayNames/territories/territory[@type="IO"][@alt="chagos"]
    let xpathParts = xpath.split(/(?<!\[@[^=]+="[^"]+)\//).slice(2);
    const root = doc.getRootNode(); // our node
    let n = root; // start with root
    const NAME = /^[^\[]*/;
    const ATTRS = /\[@([^=]+)="([^"]+)"\]/g;
    // parse
    for (const p of xpathParts) {
      const a = {};
      const tag = p.match(NAME)[0];
      const attrstr = p.substr(tag.length);
      let g;
      while ((g = ATTRS.exec(attrstr))) {
        const [, k, v] = g;
        a[k] = v; // add to map
      }
      console.dir({ tag, a });
      // Now, go find that node
      const child = findSubNode(n, tag, a);
      if (!child) {
        // special: don't create 2 subnodes
        if (n === root) {
          throw Error(
            `could not find root node ${tag} - root is ${root.children[0].nodeName}?`
          );
        }
        // then make it
        const newChild = newTag(tag);
        // add attrs
        setAttrs(a, newChild);
        n.appendChild(newChild);
        n = newChild;
      } else {
        n = child;
      }
    }
    // whew. Created.
    n.appendChild(doc.createTextNode(value.trim()));
  }

  // write it out
  const xser = new XMLSerializer();

  return xser.serializeToString(doc);
}

export { xlsGenerateXml, xlsHeaders, xlsUpload };
