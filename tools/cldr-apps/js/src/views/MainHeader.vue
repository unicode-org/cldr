<template>
  <header id="st-header">
    <a-spin v-if="!loaded" :delay="250" />
    <ul>
      <li>
        {{ stVersion }}
        <a
          href="https://cldr.unicode.org/translation/getting-started/survey-tool-phases"
          _target="CLDR_ST_DOCS"
        >
          {{ stPhase }}
        </a>
        <!-- <span
          class="extendedException"
          v-if="extendedException"
          title="Note: This phase has been extended for this locale."
        >
          (extended)
        </span> -->
      </li>
      <li>
        <a href="#menu///"><span class="main-menu-icon">☰</span></a>
      </li>
      <li v-if="unreadAnnouncementCount">
        <a href="#announcements///" v-bind:title="announcementsTitle"
          ><span class="attention-icon">🎈</span>
          {{ unreadAnnouncementCount }}</a
        >
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
            {{ item.label }}
            <span v-if="item.value === this.orgCoverage"> (Default) </span>
          </option>
        </select>
      </li>
      <li v-if="voteCountMenu && voteCountMenu.length && !needCla">
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
      <a-alert
        v-if="needCla"
        @click="showCla"
        message="CLA must be signed before data can be input (click here)"
        type="error"
        show-icon
      />
      <li>
        <a href="https://cldr.unicode.org/translation/" target="_blank"
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
        &nbsp;
        <cldr-searchbutton />
        <cldr-loginbutton />
      </li>
    </ul>
  </header>
</template>

<script>
import * as cldrAnnounce from "../esm/cldrAnnounce.mjs";
import * as cldrCoverage from "../esm/cldrCoverage.mjs";
import * as cldrLoad from "../esm/cldrLoad.mjs";
import * as cldrMenu from "../esm/cldrMenu.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";
import * as cldrText from "../esm/cldrText.mjs";
import * as cldrVote from "../esm/cldrVote.mjs";

export default {
  data() {
    return {
      loaded: false,
      announcementsTitle: null,
      coverageLevel: null,
      coverageMenu: [],
      coverageTitle: null,
      email: null,
      org: null,
      orgCoverage: null,
      sessionMessage: null,
      specialHeader: null,
      stPhase: null,
      stVersion: null,
      tcLocale: true,
      extendedException: false,
      unreadAnnouncementCount: 0,
      userName: null,
      voteCountMenu: null,
      voteLevelChanged: 0,
      needCla: false,
    };
  },

  mounted() {
    // load after the localemap is ready
    cldrLoad.onLocaleMapReady(() => {
      this.updateData();
    });
    // reload if locale changes
    cldrStatus.on("locale", () => this.updateData());
  },

  methods: {
    /**
     * Update the data, getting some data from other module(s).
     * This function is called both locally, to initialize, and from other module(s), to update.
     */
    updateData() {
      this.loaded = true;
      const loc = cldrStatus.getCurrentLocale();
      const orgCoverage = cldrCoverage.getSurveyOrgCov(loc);
      if (orgCoverage != this.orgCoverage) {
        this.orgCoverage = orgCoverage;
      }
      const coverageMenu = cldrMenu.getCoverageMenu();
      // this.coverageMenu is an array of refs, so don't just assign.
      if (coverageMenu.length > this.coverageMenu.length) {
        // remove all items
        while (this.coverageMenu.length) {
          this.coverageMenu.pop();
        }
        for (const item of coverageMenu) {
          this.coverageMenu.push(item);
        }
      }
      this.coverageTitle = cldrText.get("coverage_menu_desc");
      const coverageLevel = cldrCoverage.getSurveyUserCov() || "auto";
      if (coverageLevel != this.coverageLevel) {
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

      // only need CLA if logged in
      this.needCla = !!user && !user.claSigned;

      this.sessionMessage = cldrStatus.getSessionMessage();
      this.specialHeader = cldrStatus.getSpecialHeader();
      this.stVersion = "Survey Tool " + cldrStatus.getNewVersion();
      this.extendedException = cldrLoad.getLocaleInfo(loc)?.extended;
      if (!loc) {
        if (
          cldrStatus.getExtendedPhase() &&
          cldrStatus.getExtendedPhase() != cldrStatus.getPhase()
        ) {
          // VETTING/SUBMIT etc.
          this.stPhase = `${cldrStatus.getPhase()}/${cldrStatus.getExtendedPhase()}`;
        } else {
          // single phase
          this.stPhase = cldrStatus.getPhase();
        }
      } else if (!this.extendedException) {
        // no exception, just one phase
        this.stPhase = cldrStatus.getPhase();
      } else if (this.extendedException) {
        // we've got an exception
        this.stPhase = cldrStatus.getExtendedPhase();
      }
      cldrAnnounce.getUnreadCount(this.setUnreadCount);
    },

    setCoverageLevel() {
      cldrMenu.setCoverageLevel(this.coverageLevel);
    },

    setVoteLevel() {
      cldrVote.setVoteLevelChanged(this.voteLevelChanged);
    },

    setUnreadCount(n) {
      this.unreadAnnouncementCount = n;
      this.announcementsTitle = n
        ? "You have " + n + " unread announcement(s)"
        : "";
    },

    showCla() {
      window.location.replace("#cla///");
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

.attention-icon {
  width: 2rem;
  margin-top: -1em;
  margin-bottom: -1em;
  display: inline-block;
  vertical-align: -15%;
  animation-name: announcements;
  animation-duration: 3s;
  animation-direction: alternate;
  animation-iteration-count: infinite;
}

@keyframes announcements {
  from {
    font-size: 1em;
  }
  to {
    font-size: 2em;
  }
}

#st-special-header {
  /* This element, and those to the right of it, will be pushed to the right.
     The elements to the left of it will be pushed to the left. */
  margin-left: auto !important;
}

#coverageLevel {
  width: 16ch;
}

.extendedException {
  background-color: yellow;
}
</style>
