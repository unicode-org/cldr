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
import * as cldrClient from "../esm/cldrClient.js";
import * as cldrReport from "../esm/cldrReport.js";
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
    this.client = await cldrClient.getClient();
    await this.reload();
  },
  computed: {
    reportName() {
      return cldrReport.reportName(this.report);
    },
    statusClass() {
      return cldrReport.reportClass(this.completed, this.acceptable);
    },
  },
  methods: {
    /**
     * Map state to 2 fields
     */
    setFields(state) {
      switch (state) {
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
    },
    /**
     * map 2 fields to a string
     */
    getFields(fields) {
      const { completed, acceptable } = fields;
      if (acceptable && completed) {
        return "acceptable";
      } else if (!acceptable && completed) {
        return "unacceptable";
      } else {
        return "incomplete";
      }
    },
    async changed() {
      this.loaded = false;
      const user = cldrStatus.getSurveyUser();
      if (!user) {
        this.error = cldrText.get("E_NOT_LOGGED_IN");
        this.loaded = true;
        return;
      }
      try {
        this.setFields(this.state);
        await this.client.apis.voting.updateReport(
          {
            user: user.id,
            locale: this.$cldrOpts.locale,
            report: this.report,
          },
          {
            requestBody: {
              acceptable: this.acceptable,
              completed: this.completed,
            },
          }
        );
        await this.reload(); // will set loaded=true
      } catch (e) {
        console.error(e);
        this.error = e;
        this.loaded = true;
      }
    },
    async reload() {
      this.loaded = false;
      const user = cldrStatus.getSurveyUser();
      if (!user) {
        this.error = cldrText.get("E_NOT_LOGGED_IN");
        this.loaded = true;
        return;
      }
      try {
        const resp = await this.client.apis.voting.getReport({
          user: user.id,
          locale: this.$cldrOpts.locale,
        });
        const { acceptable, completed } = resp.obj;
        this.completed = completed.includes(this.report);
        this.acceptable = acceptable.includes(this.report);
        this.state = this.getFields({
          completed: this.completed,
          acceptable: this.acceptable,
        });
        this.loaded = true;
        this.error = null;
      } catch (e) {
        console.error(e);
        this.error = e;
        this.loaded = true;
      }
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
