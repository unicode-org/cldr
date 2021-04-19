<template>
  <nav id="DashboardSection" class="halfheight">
    <p v-if="fetchErr" class="st-sad">Error loading data: {{ fetchErr }}</p>
    <div class="while-loading" v-if="!data">
      <a-spin>
        <i>{{ loadingMessage }}</i>
      </a-spin>
    </div>
    <template v-if="data && !fetchErr">
      <header class="sidebyside-column-top">
        <span class="i-am-dashboard">Dashboard</span>
        <span class="coverage">(Coverage: {{ level }})</span>
        <span v-for="n in data.notifications" :key="n.notification">
          <button
            :notification="n.notification"
            class="scrollto cldr-nav-btn"
            v-on:click.prevent="scrollto"
            :title="categoryComment[n.notification] || humanize(n.notification)"
          >
            {{ humanize(n.notification) }} ({{ n.total }})
          </button>
          &nbsp;&nbsp;
        </span>
        <button
          class="right-button cldr-nav-btn"
          title="Close"
          @click="closeDashboard"
        >
          x
        </button>
      </header>
      <section id="DashboardScroller" class="sidebyside-scrollable">
        <template
          v-for="n in data.notifications"
          :key="'template-' + n.notification"
        >
          <template v-for="g in n.entries" :key="g.header">
            <p
              v-for="e in g.entries"
              :key="e.xpath"
              :class="'dash-' + n.notification"
            >
              <a v-bind:href="'#/' + [locale, g.page, e.xpath].join('/')">
                <span class="notification">{{ humanize(n.notification) }}</span>
                |
                <span class="section-page">{{
                  humanize(g.section + "—" + g.page)
                }}</span>
                |
                <span class="entry-header">{{ g.header }}</span>
                |
                <span class="code">{{ e.code }}</span>
                |
                <span class="previous-english" v-if="e.previousEnglish">
                  {{ e.previousEnglish }} →
                </span>
                <span class="english">{{ e.english }}</span>
                |
                <span class="old" v-bind:dir="$cldrOpts.localeDir">{{
                  e.old
                }}</span>
                |
                <span class="winning" v-bind:dir="$cldrOpts.localeDir">{{
                  e.winning
                }}</span>
              </a>
            </p>
          </template>
        </template>
      </section>
    </template>
  </nav>
</template>

<script>
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrCoverage from "../esm/cldrCoverage.js";
import * as cldrGui from "../esm/cldrGui.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as cldrSurvey from "../esm/cldrSurvey.js";

export default {
  props: [],
  data() {
    return {
      data: null,
      fetchErr: null,
      loadingMessage: "Loading Dashboard…",
      locale: null,
      level: null,
      categoryComment: {
        Provisional:
          "The item is provisional, and needs additional votes for confirmation",
        English_Changed:
          "The English version has changed, but the locale has not",
      },
    };
  },

  created() {
    this.fetchData();
  },

  methods: {
    scrollto(event) {
      const whence = event.target.getAttribute("notification");
      if (this.data && this.data.notifications) {
        for (let n of this.data.notifications) {
          if (n.notification == whence) {
            const whither = document.querySelector(".dash-" + whence);
            if (whither) {
              whither.scrollIntoView(true);
            }
            return;
          }
        }
      }
    },

    handleCoverageChanged(level) {
      console.log("Dashboard changing level: " + level);
      this.data = null;
      this.fetchData();
      return true;
    },

    fetchData() {
      this.locale = cldrStatus.getCurrentLocale();
      this.level = cldrCoverage.effectiveName();
      if (!this.locale || !this.level) {
        this.fetchErr = "Please choose a locale and a coverage level first.";
        return;
      }
      this.loadingMessage =
        "Loading Dashboard for locale " + this.locale + ", level " + this.level;
      cldrAjax
        .doFetch(this.getUrl())
        .then((data) => data.json())
        .then((data) => {
          this.updateData(data);
        })
        .catch((err) => {
          console.error("Error loading Dashboard data: " + err);
          this.fetchErr = err;
        });
    },

    getUrl() {
      const api = `summary/dashboard/${this.locale}/${this.level}`;
      const p = new URLSearchParams();
      p.append("session", cldrStatus.getSessionId());
      return cldrAjax.makeApiUrl(api, p);
    },

    updateData(data) {
      // TODO: if the user chose a different locale while waiting for data,
      // don't show the dashboard for the old locale! This may be complicated
      // if multiple dashboard requests are overlapping -- ideally should tell
      // the back end to stop working on out-dated requests

      // calculate total counts
      for (let e in data.notifications) {
        const n = data.notifications[e];
        n.total = 0;
        for (let g in n.entries) {
          n.total += n.entries[g].entries.length;
        }
      }
      this.data = data;
      this.resetScrolling();
    },

    resetScrolling() {
      setTimeout(function () {
        const el = document.getElementById("DashboardScroller");
        if (el) {
          el.scrollTo(0, 0);
        }
      }, 500 /* half a second */);
    },

    closeDashboard(event) {
      cldrGui.hideDashboard();
      if (event.shiftKey) {
        data = null;
      }
    },

    humanize(str) {
      return str.replaceAll("_", " ");
    },
  },

  computed: {
    console: () => console,
  },
};
</script>

<style scoped>
#DashboardSection {
  border-top: 2px solid #cfeaf8;
  font-size: small;
}

.while-loading {
  padding-top: 3em;
}

header {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  text-align: center;
  align-items: center;
  margin: 0;
  padding: 1ex 0;
  background-color: white;
  background-image: linear-gradient(white, #e7f7ff);
}

.i-am-dashboard {
  font-weight: bold;
}

.coverage {
  margin-left: 0.5em;
  margin-right: 0.5em;
}

.notification {
  font-style: italic;
}

.right-button {
  /* This element will be pushed to the right.
     The elements to the left of it will be pushed to the left. */
  margin-left: auto !important;
}

.section-page {
  font-weight: bold;
}

.entry-header {
  font-weight: bold;
}

.code {
  font: bold small "Courier New", Courier, mono;
}

.english {
  color: blue;
}

.previous-english {
  color: #900;
}

.old {
  color: gray;
}

.winning {
  color: green;
}

a {
  /* enable clicking on space to right of text, as well as on text itself */
  display: block;
}

a:hover {
  color: black;
  background-color: #ccdfff; /* light blue */
}
</style>
