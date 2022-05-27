<template>
  <div class="reportResponse">
    <div>
      <div class="d-dr-status statuscell" :class="statusClass">&nbsp;</div>
      <h4>Review: {{ reportName }}</h4>
      <a-spin size="small" v-if="!loaded" />
    </div>
    <a-alert v-if="error" type="error" v-model:message="error" />

    <p>
      Please read the
      <a
        target="CLDR-ST-DOCS"
        href="http://cldr.unicode.org/translation/getting-started/review-formats"
        >instructions</a
      >
      before continuing.
    </p>

    <a-radio-group v-model:value="state" @change="changed">
      <a-radio style="radioStyle" value="acceptable">
        I have reviewed the items below, and they are all acceptable</a-radio
      >
      <a-radio style="radioStyle" value="unacceptable">
        The items are not all acceptable, but I have entered in votes for the
        right ones or filed a ticket.</a-radio
      >
      <a-radio style="radioStyle" value="incomplete">
        I have not reviewed the items.</a-radio
      >
    </a-radio-group>
  </div>
</template>

<script>
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as cldrText from "../esm/cldrText.js";
export default {
  props: [
    "report", // e.g. 'numbers'
  ],
  data: function () {
    return {
      completed: false,
      acceptable: false,
      loaded: false,
      error: null,
      state: null,
    };
  },
  created: async function () {
    await this.reload();
  },
  computed: {
    reportName() {
      return cldrText.get(`special_r_${this.report}`);
    },
    statusClass() {
      if (this.completed && this.acceptable) {
        return "d-dr-approved";
      } else if (this.completed && !this.acceptable) {
        return "d-dr-contributed";
      } else {
        return "d-dr-missing";
      }
    },
  },
  methods: {
    async changed() {
      this.loaded = false;
      const user = cldrStatus.getSurveyUser();
      if (!user) {
        this.error = "Not logged in.";
        this.loaded = true;
        return;
      }
      switch (this.state) {
        case "acceptable":
          this.completed = true;
          this.acceptable = true;
          break;
        case "unacceptable":
          this.completed = true;
          this.acceptable = false;
          break;
        case "incomplete":
        default:
          this.completed = false;
          this.acceptable = false;
          break;
      }
      const theUrl = `api/voting/reports/users/${user.id}/locales/${this.$cldrOpts.locale}/reports/${this.report}`;
      await cldrAjax
        .doFetch(theUrl, {
          method: "POST",
          body: JSON.stringify({
            acceptable: this.acceptable,
            completed: this.completed,
          }),
          headers: {
            "Content-Type": "application/json",
            "X-SurveyTool-Session": this.$cldrOpts.sessionId,
          },
        })
        .catch((e) => {
          console.error(e);
          this.error = e;
          this.loaded = true;
        });
      return this.reload(); // will set loaded=true
    },
    reload() {
      this.loaded = false;
      const user = cldrStatus.getSurveyUser();
      if (!user) {
        this.error = "Not logged in.";
        this.loaded = true;
        return;
      }
      const theUrl = `api/voting/reports/users/${user.id}/locales/${this.$cldrOpts.locale}`;
      return cldrAjax
        .doFetch(theUrl, {
          headers: {
            "X-SurveyTool-Session": this.$cldrOpts.sessionId,
          },
        })
        .then((r) => r.json())
        .then(({ acceptable, completed }) => {
          this.completed = completed.includes(this.report);
          this.acceptable = acceptable.includes(this.report);
          if (this.acceptable && this.completed) {
            this.state = "acceptable";
          } else if (!this.acceptable && this.completed) {
            this.state = "unacceptable";
          } else {
            this.state = "incomplete";
          }
          this.loaded = true;
          this.error = null;
        })
        .catch((e) => {
          console.error(e);
          this.error = e;
          this.loaded = true;
        });
    },
  },
};
</script>

<style scoped>
.radioStyle {
  display: flex;
}

.reportResponse {
  border: 1px solid gray;
  padding: 0.5em;
  margin: 1em;
  box-shadow: 1em;
  background-color: bisque;
  width: 50%;
}

.reportResponse .statusCell,
.reportResponse h4 {
  display: inline;
}

.reportResponse h4 {
  padding-left: 1em;
}

.reportResponse .statusCell {
  padding: 5px 5px 3px gray;
}
</style>
>
