// For CLDR-14804
const FIRST_RELEASE = 30;

// General utils

// return a - b
function setMinus(a, b) {
  const x = new Set();
  for (const i of a.values()) {
    if (!b.has(i)) {
      x.add(i);
    }
  }
  return x;
}

// return a || b
function setUnion(a, b) {
  const x = new Set();
  for (const i of a.values()) {
    x.add(i);
  }
  for (const i of b.values()) {
    x.add(i);
  }
  return x;
}

function setToList(a) {
  const l = [];
  for (const i of a.values()) {
    l.push(i);
  }
  return l;
}

// mismatch between user table and enum names
function orgMogrify(org) {
  org = org.toLowerCase();
  org = org.replace(/ +/g, ""); // Long Now -> longnow
  org = org.replace(/\./g, "_"); // openoffice.org -> openoffice_org
  return org;
}

function resultsByOrg(results) {
  const r = {};
  for (const d of results) {
    const { org } = d;
    r[org] = d;
  }
  return r;
}

/**
 * Generate the Locales.txt file
 * @param {Object} data input object
 * @param {Function} outFunction if null, will return array of lines
 */
function generateLocalesTxt(data, outFunction) {
  const returnLines = [];
  if (!outFunction) {
    outFunction = (str) => returnLines.push(str);
  }
  outFunction(`# Locales.txt generated from data dump`);
  outFunction(`# where CLDR version ≥ ${FIRST_RELEASE}`);
  outFunction();
  const { participationByVersion, results } = data;
  // turn results into a map
  const allOrgs = resultsByOrg(results);
  // now, per org
  for (const org of Object.keys(allOrgs).sort()) {
    const localesWithUsers = new Set(allOrgs[org].localesWithVetters);
    const localesWithVotes = new Set();
    // now look for actual votes
    outFunction(`# ${org}`);
    for (const [ver, data] of Object.entries(participationByVersion)) {
      if (ver.substring(1) < FIRST_RELEASE) {
        continue; // too old
      }
      const { allParticipationByOrgLocale } = data;
      for (const [org2, locCount] of Object.entries(
        allParticipationByOrgLocale
      )) {
        // validate org mapping
        const orgLower = orgMogrify(org2);
        if (!allOrgs[orgLower]) {
          throw Error(`${ver} has unknown org ${org2}`);
        }

        if (org !== orgLower) {
          continue; // wrong org
        }

        // collect all data
        for (const loc of Object.keys(locCount)) {
          localesWithVotes.add(loc);
        }
      }
    }
    // Participation but no vetters?
    const participationNoVetters = setMinus(localesWithVotes, localesWithUsers);
    if (participationNoVetters.size !== 0) {
      outFunction(
        `#  Participation but no vetters: ` +
          setToList(participationNoVetters).join(" ")
      );
    }
    const allLocales = setUnion(localesWithVotes, localesWithUsers);
    if (allLocales.size == 0) {
      outFunction(`#  no vetters, no votes`);
    }
    const userButNoVotes = setMinus(localesWithUsers, localesWithVotes);
    for (const l of setToList(localesWithVotes).sort()) {
      if (localesWithVotes.has(l)) {
        outFunction(`${org} ; ${l} ; moderate ; `);
      }
    }
    if (userButNoVotes.size !== 0) {
      outFunction("#");
      outFunction("# No votes in the following:");
      for (const l of setToList(userButNoVotes).sort()) {
        outFunction(`${org} ; ${l} ; moderate ; `);
      }
    }
    const { defaultCoverage } = allOrgs[org];
    if (defaultCoverage !== "undetermined") {
      outFunction(`${org} ; * ; ${defaultCoverage}`);
    }
    outFunction();
  }
  return returnLines;
}

export { generateLocalesTxt, FIRST_RELEASE };
