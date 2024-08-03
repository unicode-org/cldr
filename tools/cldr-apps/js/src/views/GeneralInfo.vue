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

    <!-- <cldr-overall-errors /> -->
  </div>
</template>

<script>
import * as cldrDashContext from "../esm/cldrDashContext.mjs";
import * as cldrLoad from "../esm/cldrLoad.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";
import * as cldrText from "../esm/cldrText.mjs";

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

    this.showDashboardOrButton();
  },

  methods: {
    showDashboardOrButton() {
      // If Dashboard is already visible, hide the button for opening it.
      if (cldrDashContext.isVisible()) {
        const els = document.getElementsByClassName("general-open-dash");
        for (let i = 0; i < els.length; i++) {
          els[i].style.display = "none";
        }
      } else if (cldrDashContext.shouldBeShown()) {
        this.insertDashboard();
      }
    },

    insertDashboard() {
      cldrDashContext.insert();
    },
  },
};
</script>

<style>
button.general-open-dash {
  /* We only want THIS button to float, not all Open Dashboard buttons. */
  float: right;
}

p.special_general {
  margin: 1em;
  padding: 1em;
  border: 2px solid gray;
}
</style>
