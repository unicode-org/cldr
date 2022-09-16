<template>
  <div>
    <div class="warnText" v-if="localeSpecialNote" v-html="localeSpecialNote" />
    <div class="warnText" v-if="betaNote" v-html="betaNote" />
    <p class="special_general" v-if="specialGeneral" v-html="specialGeneral" />
    <button
      @click="insertDashboard"
      class="cldr-nav-btn btn-primary open-dash general-open-dash"
      type="button"
    >
      Open Dashboard
    </button>
  </div>
</template>

<script>
import * as cldrGui from "../esm/cldrGui.js";
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as cldrText from "../esm/cldrText.js";

export default {
  data() {
    return {
      localeSpecialNote: null,
      betaNote: null,
      specialGeneral: null,
    };
  },
  mounted() {
    const locmap = cldrLoad.getTheLocaleMap();
    const bund = locmap.getLocaleInfo(cldrStatus.getCurrentLocale());
    let msg = cldrLoad.localeSpecialNote(bund, false);
    if (msg) {
      msg = locmap.linkify(msg);
      this.localeSpecialNote = msg;
    } else {
      this.localeSpecialNote = null;
    }

    // setup beta note
    if (cldrStatus.getIsPhaseBeta()) {
      this.betaNote = cldrText.sub("beta_msg", {
        info: bund,
        locale: cldrStatus.getCurrentLocale(),
        msg: msg,
      });
    } else {
      this.betaNote = null;
    }

    this.specialGeneral = cldrText.get("generalSpecialGuidance");
  },
  methods: {
    insertDashboard() {
      cldrGui.insertDashboard();
    },
  },
};
</script>

<style>
button.general-open-dash {
  /* We only want THIS button to float, not all Open Dashboard buttons. */
  float: right;
}
</style>
