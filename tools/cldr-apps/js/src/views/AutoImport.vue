<template>
  <h3>{{ header }}</h3>

  <div v-if="errors.length">
    <span class="autoImportErrors">Error(s):</span>
    <ul>
      <li v-for="error in errors">{{ error }}</li>
    </ul>
  </div>

  <p><a-spin v-if="!haveResult" :delay="100" /> {{ message }}</p>

  <div class="progressSection" v-if="versionsTotal">
    <a-progress type="dashboard" showInfo="false" :percent="percent" />
    <p v-if="versionsTotal > versionsDone">
      {{ imported }} votes imported .. {{ remaining }} votes remaining
    </p>
  </div>

  <button v-if="haveResult" n v-on:click="begone()">Continue</button>
</template>

<script>
import * as cldrAjax from "../esm/cldrAjax.mjs";
import * as cldrClient from "../esm/cldrClient.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";
import * as cldrSurvey from "../esm/cldrSurvey.mjs";
import * as cldrText from "../esm/cldrText.mjs";

export default {
  data() {
    return {
      errors: [],
      header: "",
      haveResult: false,
      message: "Loading...",
      remaining: 0,
      imported: 0,
      versionsDone: 0,
      versionsTotal: 0,
      timeout: undefined,
    };
  },

  mounted() {
    cldrStatus.setAutoImportBusy(true);
    this.run();
  },

  unmounted() {
    cldrStatus.setAutoImportBusy(false);
  },

  computed: {
    percent() {
      if (this.versionsTotal == 0) return 100;
      return Number((this.versionsDone / this.versionsTotal) * 100).toFixed(0);
    },
  },

  methods: {
    setupTimeout() {
      this.timeout = setTimeout((t) => t.checkStatus(), 2000, this);
    },

    async checkStatus() {
      // clear so we only run once
      clearTimeout(this.timeout);
      this.timeout = undefined;

      const client = await cldrClient.getClient();
      const resp = await client.apis.voting.getOldImportStatus();
      const { remaining, imported, versionsDone, versionsTotal } =
        await resp.obj;

      this.remaining = remaining;
      this.imported = imported;
      this.versionsDone = versionsDone;
      this.versionsTotal = versionsTotal;

      // call us again.
      this.setupTimeout();
    },

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
      this.setupTimeout();
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
          count: Number(json.autoImportedOldWinningVotes).toLocaleString([
            "en",
          ]),
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
      window.clearTimeout(this.timeout);
      this.timeout = undefined;
      this.versionsTotal = 0;
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
.progressSection {
  max-width: 500px;
}
</style>
