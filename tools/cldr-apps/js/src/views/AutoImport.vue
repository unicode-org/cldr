<template>
  <h3>{{ header }}</h3>

  <div v-if="errors.length">
    <span class="autoImportErrors">Error(s):</span>
    <ul>
      <li v-for="error in errors">{{ error }}</li>
    </ul>
  </div>

  <p><a-spin v-if="!haveResult" :delay="100" /> {{ message }}</p>

  <button v-if="haveResult" n v-on:click="begone()">Continue</button>
</template>

<script>
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as cldrSurvey from "../esm/cldrSurvey.js";
import * as cldrText from "../esm/cldrText.js";

export default {
  data() {
    return {
      errors: [],
      header: "",
      haveResult: false,
      message: "Loading...",
    };
  },

  mounted() {
    cldrStatus.setAutoImportBusy(true);
    this.run();
  },

  unmounted() {
    cldrStatus.setAutoImportBusy(false);
  },

  methods: {
    run() {
      this.header = cldrText.get("v_oldvote_auto_msg");
      this.message = cldrText.get("v_oldvote_auto_progress_msg");
      const xhrArgs = {
        url: this.getUrl(),
        handleAs: "json",
        load: this.load,
        error: (err) => this.errors.push(err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    getUrl() {
      const p = new URLSearchParams();
      // See WHAT_AUTO_IMPORT = "auto_import" in SurveyAjax.java
      p.append("what", "auto_import");
      p.append("s", cldrStatus.getSessionId());
      p.append("cacheKill", cldrSurvey.cacheBuster());
      return cldrAjax.makeUrl(p);
    },

    load(json) {
      if (json.autoImportedOldWinningVotes) {
        const vals = {
          count: json.autoImportedOldWinningVotes,
        };
        this.message = cldrText.sub("v_oldvote_auto_desc_msg", vals);
        this.haveResult = true;
      } else {
        this.begone();
      }
    },

    begone() {
      cldrStatus.setAutoImportBusy(false);
      this.header = this.message = "";
      this.haveResult = false;
      window.location.href = "#locales///";
    },
  },
};
</script>

<style scoped>
.autoImportErrors {
  font-weight: bold;
  font-size: large;
  color: red;
}
</style>
