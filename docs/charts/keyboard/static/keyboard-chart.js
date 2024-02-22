// helper functions for keyboard

/**
 * Unescape an escaped string
 * @param str input string such as '\u017c'
 * @returns
 */
function unescapeStr(str) {
  str = str.replace(/\\u{([0-9a-fA-F]+)}/g, (a, b) =>
    String.fromCodePoint(Number.parseInt(b, 16))
  );
  return str;
}

function getKeyboardLayers(id) {
  let q = _KeyboardData.keyboards[id].keyboard3.layers;
  if (!Array.isArray(q)) {
    q = [q];
  }
  mogrifyAttrs(q);
  const keybag = getKeyboardKeys(id);
  mogrifyLayerList(q, keybag);
  return q;
}

function mogrifyLayerList(layerList, keybag) {
  layerList.forEach(({ layer }) => {
    layer.forEach(({ row }) => {
      row.forEach((r) => {
        r.keys = r.keys.split(" ").map((id) =>
          Object.assign(
            {
              id,
            },
            keybag[id]
          )
        );
      });
    });
  });
}

function getImportFile(id) {
  return _KeyboardData.imports[id["@_path"].split("/")[1]];
}

function getImportKeys(id) {
  const imp = getImportFile(id);
  if (!imp) {
    throw Error(`Could not load import ${JSON.stringify(id)}`);
  }
  return imp.keys.key;
}

function mogrifyKeys(keys) {
  // drop @'
  if (!keys) {
    return [];
  }
  return keys.reduce((p, v) => {
    // TODO: any other swapping
    mogrifyAttrs(v);
    const { id, output } = v;
    if (output) {
      v.output = unescapeStr(output);
    }
    p[id] = v;
    return p;
  }, {});
}

function mogrifyAttrs(o) {
  for (const k of Object.keys(o)) {
    const ok = o[k];
    if (/^@_/.test(k)) {
      const attr = k.substring(2);
      o[attr] = ok;
      delete o[k];
    } else if (Array.isArray(ok)) {
      ok.forEach((e) => mogrifyAttrs(e));
    } else if (typeof ok === "object") {
      mogrifyAttrs(ok);
    }
  }
  return o;
}

function getKeyboardKeys(id) {
  const keys = _KeyboardData.keyboards[id].keyboard3.keys.key || [];
  if (!keys) {
    throw Error(`No keys for ${id}`);
  }
  let imports = [
    {
      // add implied import
      "@_base": "cldr",
      "@_path": "techpreview/keys-Latn-implied.xml",
    },
    ...(_KeyboardData.keyboards[id].keyboard3.keys.import || []),
  ];

  const importedKeys = [];
  for (const fn of imports) {
    for (const k of getImportKeys(fn)) {
      importedKeys.push(k);
    }
  }

  return mogrifyKeys([...importedKeys, ...keys]);
}

function getIds() {
  return Object.keys(_KeyboardData.keyboards);
}
