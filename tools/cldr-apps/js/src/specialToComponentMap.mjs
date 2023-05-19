import AboutPanel from "./views/AboutPanel.vue";
import AnnouncePanel from "./views/AnnouncePanel.vue";
import AddUser from "./views/AddUser.vue";
import AutoImport from "./views/AutoImport.vue";
import GeneralInfo from "./views/GeneralInfo.vue";
import LockAccount from "./views/LockAccount.vue";
import LookUp from "./views/LookUp.vue";
import MainMenu from "./views/MainMenu.vue";
import TestPanel from "./views/TestPanel.vue";
import TransferVotes from "./views/TransferVotes.vue";
import UnknownPanel from "./views/UnknownPanel.vue";
import VettingParticipation2 from "./views/VettingParticipation2.vue";
import VettingSummary from "./views/VettingSummary.vue";
import WaitingPanel from "./views/WaitingPanel.vue";

/**
 * Map of special (i.e. /v#about ) to components
 */
const specialToComponentMap = {
  about: AboutPanel,
  announcements: AnnouncePanel,
  add_user: AddUser,
  auto_import: AutoImport,
  general: GeneralInfo,
  lock_account: LockAccount,
  lookup: LookUp,
  menu: MainMenu,
  retry: WaitingPanel,
  retry_inplace: WaitingPanel, // Like retry, but do NOT redirect after resume.
  test_panel: TestPanel, // for testing
  transfervotes: TransferVotes,
  vetting_participation2: VettingParticipation2,
  vsummary: VettingSummary,
  // If no match, end up here
  default: UnknownPanel,
};

/**
 * Lookup the correct Vue component to use
 * @param {String} specialName
 * @returns {Component} vue component (or the default)
 */
function specialToComponent(specialName) {
  let component = specialToComponentMap[specialName];
  // manage default
  if (!component) {
    component = specialToComponentMap.default;
  }
  return component;
}

/**
 * Get a list of available specials
 * @returns String[] list of specials
 */
function listSpecials() {
  return Object.keys(specialToComponentMap);
}

/**
 * Returns true if this is a vue special.
 * Will return true for "default" also.
 */
function isVueSpecial(specialName) {
  return specialName in specialToComponentMap;
}

export { isVueSpecial, listSpecials, specialToComponent };
