<template>
  <span id="CompletionSection" v-if="!hide">
    <span v-if="fetchErr" class="st-sad">{{ fetchErr }}</span>
    <span v-if="total < 0 && !fetchErr"><a-spin></a-spin> </span>
    <span
      :title="
        'Voting Completion (locale: ' +
        localeName +
        '; coverage: ' +
        level +
        ')'
      "
      >Votes: {{ votes }} / Total: {{ total }}</span
    >
  </span>
</template>

<script>
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrCoverage from "../esm/cldrCoverage.js";
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrStatus from "../esm/cldrStatus.js";

export default {
  props: [],
  data() {
    return {
      hide: true,
      total: -1,
      votes: -1,
      fetchErr: null,
      locale: null,
      localeName: null,
      level: null,
    };
  },

  created() {
    this.fetchData();
  },

  methods: {
    handleCoverageChanged(level) {
      console.log("Completion changing level: " + level);
      this.fetchData();
    },

    fetchData() {
      if (!cldrStatus.getSurveyUser()) {
        this.hide = true;
        return;
      }
      this.hide = false;
      const locale = cldrStatus.getCurrentLocale();
      if (!locale) {
        this.fetchErr = "(no locale)";
        return;
      }
      const level = cldrCoverage.effectiveName(locale);
      if (!level) {
        this.fetchErr = "(no level)";
        return;
      }
      this.locale = locale;
      this.level = level;
      this.localeName = cldrLoad.getLocaleName(this.locale);
      this.reallyFetch();
    },

    reallyFetch() {
      const url = `api/completion/voting/${this.locale}/${this.level}`;
      cldrAjax
        .doFetch(url)
        .then((response) => {
          if (!response.ok) {
            throw Error(response.statusText);
          }
          return response;
        })
        .then((data) => data.json())
        .then((data) => {
          this.votes = data.votes;
          this.total = data.total;
        })
        .catch((err) => {
          console.error("Error loading Completion data: " + err);
          this.fetchErr = err;
        });
    },
  },

  computed: {
    console: () => console,
  },
};
</script>

<style scoped>
#CompletionSection {
  border: 1px solid purple;
  font-size: small;
}

.st-sad {
  font-style: italic;
  border: 1px dashed red;
  color: darkred;
}
</style>
