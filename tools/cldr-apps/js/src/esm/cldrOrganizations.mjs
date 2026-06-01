/*
 * cldrOrganizations: handle Organization names
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrClient from "./cldrClient.mjs";

let orgs = null;

/**
 * Get a complete list of organizations, with their short names and display names
 *
 * Short and display names are like "wikimedia" and "Wikimedia Foundation", respectively
 *
 * @returns the object with these elements:
 *          displayToShort - the map from display names to short names
 *          shortToDisplay - the map from short names to display names
 *          sortedDisplayNames - the sorted array of display names
 */
export async function get() {
  if (orgs) {
    return orgs;
  }
  const url = cldrAjax.makeApiUrl("organizations", null);
  return await cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(loadOrgs);
}

/** accessible for unit testing only, to set mock data  */
export function loadOrgs(json) {
  if (!json.map) {
    console.error("Organization list not received from server");
    return null;
  }
  const shortToDisplay = json.map;
  const displayToShort = {};
  for (let shortName in shortToDisplay) {
    displayToShort[shortToDisplay[shortName]] = shortName;
  }
  const sortedDisplayNames = Object.keys(displayToShort).sort((a, b) =>
    a.localeCompare(b)
  );
  orgs = { displayToShort, shortToDisplay, sortedDisplayNames };
  return orgs;
}

let orgCoverage = null;

/** @internal load the orgCoverage data */
async function loadOrgCoverage() {
  if (!orgCoverage) {
    const client = await cldrClient.getClient();
    orgCoverage = (await client.apis.organizations.getOrgCoverage()).body;
  }
  return orgCoverage;
}

/**
 *  @returns {Promise<map<string,map<string,number>>}  map from each organization to a map of locale to level
 */
export async function getOrgCoverage() {
  const { organization_locale_level } = await loadOrgCoverage();
  return organization_locale_level;
}

/** @returns {Promise<string[]>} flat list of organizations */
export async function getTcOrgs() {
  const { tc_orgs } = await loadOrgCoverage();
  return tc_orgs;
}
