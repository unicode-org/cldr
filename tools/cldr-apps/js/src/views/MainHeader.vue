<template>
  <header id="st-header">
    <ul>
      <li>{{ stVersionPhase }}</li>
      <li>
        <a href="#menu///"><span class="main-menu-icon">☰</span> Menu</a>
      </li>
      <li v-if="coverageLevel">
        <label for="coverageLevel">Coverage:</label>
        <select
          id="coverageLevel"
          name="coverageLevel"
          v-model="coverageLevel"
          v-bind:title="coverageTitle"
          v-on:change="setCoverageLevel()"
        >
          <option
            v-for="item in coverageMenu"
            v-bind:key="item.value"
            v-bind:value="item.value"
          >
            {{ coverageLabel(item) }}
          </option>
        </select>
      </li>
      <li v-if="voteCountMenu && voteCountMenu.length">
        <label for="voteLevelChanged">Votes:</label>
        <select
          id="voteLevelChanged"
          name="voteLevelChanged"
          v-model="voteLevelChanged"
          v-on:change="setVoteLevel()"
          title="Vote with a different number of votes"
        >
          <option :key="n" v-for="n in voteCountMenu">{{ n }}</option>
        </select>
      </li>
      <li>
        <a href="https://sites.google.com/site/cldr/translation" target="_blank"
          >Instructions</a
        >
      </li>
      <li id="st-special-header" class="specialmessage">{{ specialHeader }}</li>
      <li>
        <a
          href="https://www.unicode.org/policies/privacy_policy.html"
          target="_blank"
          >This site uses cookies.</a
        >
      </li>
      <li>
        <span id="flag-info"></span>
        <span id="st-session-message" class="v-status">{{
          sessionMessage
        }}</span>
      </li>
      <li>
        <span class="hasTooltip" v-bind:title="email">{{ userName }}</span>
        <span
          v-if="email"
          class="glyphicon glyphicon-user tip-log"
          v-bind:title="org"
        ></span>
        &nbsp;|&nbsp;
        <cldr-loginbutton />
      </li>
    </ul>
  </header>
</template>

<script>
import * as cldrCoverage from "../esm/cldrCoverage.js";
import * as cldrMenu from "../esm/cldrMenu.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as cldrText from "../esm/cldrText.js";
import * as cldrVote from "../esm/cldrVote.js";

export default {
  data() {
    return {
      coverageLevel: null,
      coverageMenu: null,
      coverageTitle: null,
      email: null,
      org: null,
      orgCoverage: null,
      sessionMessage: null,
      specialHeader: null,
      stVersionPhase: null,
      userName: null,
      voteCountMenu: null,
      voteLevelChanged: 0,
    };
  },

  created() {
    this.updateData();
  },

  methods: {
    coverageLabel(item) {
      if (item.value == this.orgCoverage) {
        return cldrText.sub("coverage_auto_msg", {
          surveyOrgCov: item.label,
        });
      }
      return item.label;
    },
    /**
     * Update the data, getting some data from other module(s).
     * This function is called both locally, to initialize, and from other module(s), to update.
     */
    updateData() {
      let needUpdate = false;
      const orgCoverage = cldrCoverage.getSurveyOrgCov(
        cldrStatus.getCurrentLocale()
      );
      if (orgCoverage != this.orgCoverage) {
        needUpdate = true;
        this.orgCoverage = orgCoverage;
      }
      const coverageMenu = cldrMenu.getCoverageMenu();
      if (coverageMenu != this.coverageMenu) {
        needUpdate = true;
        this.coverageMenu = coverageMenu;
      }
      this.coverageTitle = cldrText.get("coverage_menu_desc");
      const coverageLevel = cldrCoverage.getSurveyUserCov() || "auto";
      if (coverageLevel != this.coverageLevel) {
        needUpdate = true;
        this.coverageLevel = coverageLevel;
      }
      const user = cldrStatus.getSurveyUser();

      if (user) {
        this.email = user.email;
        this.org = user.org;
        this.userName = user.name;
        this.voteCountMenu = user.voteCountMenu;
        if (!this.voteLevelChanged) {
          this.voteLevelChanged = user.votecount;
        }
      } else {
        this.email = null;
        this.org = null;
        this.userName = null;
        this.voteCountMenu = null;
        this.voteLevelChanged = 0;
      }
      this.sessionMessage = cldrStatus.getSessionMessage();
      this.specialHeader = cldrStatus.getSpecialHeader();
      this.stVersionPhase =
        "Survey Tool " +
        cldrStatus.getNewVersion() +
        " " +
        cldrStatus.getPhase();
      if (needUpdate) {
        this.$forceUpdate();
      }
    },

    setCoverageLevel() {
      cldrMenu.setCoverageLevel(this.coverageLevel);
    },

    setVoteLevel() {
      cldrVote.setVoteLevelChanged(this.voteLevelChanged);
    },
  },
};
</script>

<style scoped>
header {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  text-align: center;
  margin: 0;
  padding: 0.2em;
  background-color: white;
  background-image: linear-gradient(white, #e7f7ff);
  z-index: 900; /* prevent transparency when scrolling; also stay in front of "overlay"  */
}

ul {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: baseline;
  width: 100%;
  margin: 0;
  padding: 0;
}

li {
  display: inline;
  margin-left: 0.5em;
  margin-right: 0.5em;
}

label {
  /* override bootstrap, which has margin-bottom: 5px; font-weight: bold;
     -- the 5px messes up vertical alignment */
  margin-bottom: 0;
  font-weight: normal;
}

.main-menu-icon {
  font-size: 2em;
  margin-top: -1em;
  margin-bottom: -1em;
  display: inline-block;
  vertical-align: -3%;
}

.main-menu:hover {
  background-color: white;
}

#st-special-header {
  /* This element, and those to the right of it, will be pushed to the right.
     The elements to the left of it will be pushed to the left. */
  margin-left: auto !important;
}

#coverageLevel {
  width: 16ch;
}
</style>
