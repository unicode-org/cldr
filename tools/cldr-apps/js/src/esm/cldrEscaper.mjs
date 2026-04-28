let data = null;

let mapByName = null;

const staticInfo = {
  forceEscapeRegex: "[\\u200e\\u200f\\uFFF0]",
  names: {
    "\u0020": { name: "SP" },
    "\u200e": { name: "LRM" },
    "\u200f": { name: "RLM" },
  },
  namesForMenu: ["LRM", "RLM", "SP"],
}; // start from static info - useful for tests

// we preload the static info
updateInfo(staticInfo);

/** updates content and recompiles regex */
function updateInfo(escapedCharInfo) {
  const updatedRegex = escapedCharInfo.forceEscapeRegex;
  const forceEscape = new RegExp(updatedRegex, "gu");
  data = { escapedCharInfo, forceEscape };
  mapByName = {};
  for (const key of Object.keys(escapedCharInfo.names)) {
    const info = escapedCharInfo.names[key];
    mapByName[info.name] = {
      char: key,
      shortName: info.shortName,
      description: info.description,
    };
  }
}

function needsEscaping(str) {
  if (!str) return false;
  return !!str.match(data.forceEscape);
}

/** get information for one char, or null */
function getCharInfo(str) {
  return data.escapedCharInfo?.names[str];
}

function getShortName(str) {
  const e = getCharInfo(str);
  if (e) {
    return e.name || e.shortName;
  }
  return null;
}

function getMapByName() {
  return mapByName;
}

function getNamesForMenu() {
  return data.escapedCharInfo?.namesForMenu;
}

export {
  getCharInfo,
  getMapByName,
  getNamesForMenu,
  getShortName,
  needsEscaping,
  updateInfo,
};
