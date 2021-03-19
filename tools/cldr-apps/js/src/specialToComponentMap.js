import AboutPanel from "./views/AboutPanel.vue";
import LookUp from "./views/LookUp.vue";
import DashboardPanel from "./views/DashboardPanel.vue";
import UnknownPanel from "./views/UnknownPanel.vue";
import VettingSummary from "./views/VettingSummary.vue";
import WaitingPanel from "./views/WaitingPanel.vue";
import TestPanel from "./views/TestPanel.vue";

/**
 * Map of special (i.e. /v#about ) to components
 */
const specialToComponentMap = {
  about: AboutPanel,
  lookup: LookUp,
  retry: WaitingPanel,
  vsummary: VettingSummary,
  r_vetting_json: DashboardPanel,
  // If no match, end up here
  default: UnknownPanel,
  test_panel: TestPanel, // for testing
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

export { specialToComponent, listSpecials };
