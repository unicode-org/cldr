/*
 * cldrUserLevels: handle user level names and their associated numbers, such as
 * "admin" being level zero. The front end at certain times requests a list of these
 * from the back end. The front end also assumes that certain names like "admin" exist.
 * Encapsulate those assumption by defining constants for the names here, and checking
 * for mismatches between the front and back ends.
 */
import * as cldrAjax from "./cldrAjax.mjs";

// the following are all assumed to be lowercase in check()
const ADMIN = "admin";
const ANONYMOUS = "anonymous";
const GUEST = "guest";
const LOCKED = "locked";
const MANAGER = "manager";
const TC = "tc";
const VETTER = "vetter";

const ALL_NAMES = [ADMIN, ANONYMOUS, GUEST, LOCKED, MANAGER, TC, VETTER];

let checked = false;

/**
 * Get the numeric level corresponding to the given name
 *
 * @param {String} name the name like "admin"
 * @param {Object} list an object associating a name with each number; for example,
 *                      list[0].name = cldrUserLevels.ADMIN,
 *                      list[999].name = cldrUserLevels.LOCKED
 * @returns the numeric level, such as zero for "admin" or 999 for "locked"
 *
 * The list may also have other properties like "string"; for example, list[999].string = "999: (LOCKED)"
 * -- but those aren't used here
 */
function getUserLevel(name, list) {
  if (!checked) {
    check(list);
  }
  for (let number in list) {
    if (match(name, list[number].name)) {
      return parseInt(number);
    }
  }
  console.error("No user level found with the name " + name);
  return null;
}

function check(list) {
  for (let number in list) {
    const name = list[number].name.toLowerCase();
    if (!ALL_NAMES.includes(name)) {
      console.error("Server, but not client, has user level name: " + name);
    }
  }
  for (let name of ALL_NAMES) {
    let found = false;
    for (let number in list) {
      if (match(name, list[number].name)) {
        found = true;
        break;
      }
    }
    // Don't complain if anonymous is missing; sometimes it is missing intentionally
    if (!found && name !== ANONYMOUS) {
      console.error("Client, but not server, has user level name: " + name);
    }
  }
  checked = true;
}

function canVoteInNonOrgLocales(number, list) {
  const name = list[number]?.name;
  return match(name, ADMIN) || match(name, TC) || match(name, GUEST);
}

function match(a, b) {
  return a && b && a.toLowerCase() === b.toLowerCase();
}

async function getLevelList() {
  const url = cldrAjax.makeApiUrl("userlevels", null);
  const list = await cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(loadLevelList)
    .catch((e) => console.error(`Error: ${e} ...`));
  return list;
}

function loadLevelList(json) {
  if (!json.levels) {
    console.error("Level list not received from server");
    return null;
  } else {
    return json.levels;
  }
}

export {
  ADMIN,
  ANONYMOUS,
  GUEST,
  LOCKED,
  MANAGER,
  TC,
  VETTER,
  canVoteInNonOrgLocales,
  getLevelList,
  getUserLevel,
  match,
};
