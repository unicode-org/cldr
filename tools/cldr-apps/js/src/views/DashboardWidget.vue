<template>
  <nav id="DashboardSection" class="halfheight">
    <p v-if="fetchErr" class="st-sad">Error loading data: {{ fetchErr }}</p>
    <div class="while-loading" v-if="!data && !fetchErr">
      <a-spin>
        <i>{{ loadingMessage }}</i>
      </a-spin>
    </div>
    <template v-if="data && !fetchErr">
      <header class="sidebyside-column-top">
        <button
          class="cldr-nav-btn dash-closebox"
          title="Close"
          @click="closeDashboard"
        >
          X
        </button>
        <span
          class="i-am-dashboard"
          :title="
            'Dashboard (locale: ' + localeName + '; coverage: ' + level + ')'
          "
          >Dashboard</span
        >
        <button
          class="cldr-nav-btn dash-reload"
          title="Reload"
          @click="reloadDashboard"
        >
          ↻
        </button>
        <span v-for="n in data.notifications" :key="n.notification">
          <template v-if="n.total">
            <button
              :notification="n.notification"
              class="scrollto cldr-nav-btn"
              v-on:click.prevent="scrollToCategory"
              :title="
                categoryComment[n.notification] || humanize(n.notification)
              "
            >
              {{ humanize(n.notification) }} ({{ n.total }})
            </button>
            &nbsp;&nbsp;
          </template>
        </span>
        <span class="right-control">
          <input
            type="checkbox"
            title="Hide checked items"
            id="hideChecked"
            v-model="hideChecked"
          /><label for="hideChecked">hide</label>
        </span>
      </header>
      <section id="DashboardScroller" class="sidebyside-scrollable">
        <template
          v-for="n in data.notifications"
          :key="'template-' + n.notification"
        >
          <template v-for="g in n.entries" :key="g.section + g.page + g.header">
            <template v-for="e in g.entries">
              <p
                v-if="!(hideChecked && e.checked)"
                :key="'dash-item-' + e.xpath + '-' + n.notification"
                :id="'dash-item-' + e.xpath + '-' + n.notification"
                :class="
                  'dash-' +
                  n.notification +
                  (lastClicked === e.xpath + '-' + n.notification
                    ? ' last-clicked'
                    : '')
                "
              >
                <span class="dashEntry">
                  <a
                    v-bind:href="'#/' + [locale, g.page, e.xpath].join('/')"
                    @click="
                      () => setLastClicked(e.xpath + '-' + n.notification)
                    "
                  >
                    <span
                      class="notification"
                      :title="
                        categoryComment[n.notification] ||
                        humanize(n.notification)
                      "
                      >{{ abbreviate(n.notification) }}</span
                    >
                    <span class="section-page" title="section—page">{{
                      humanize(g.section + "—" + g.page)
                    }}</span>
                    |
                    <span class="entry-header" title="entry header">{{
                      g.header
                    }}</span>
                    |
                    <span class="code" title="code">{{ e.code }}</span>
                    |
                    <span
                      class="previous-english"
                      title="previous English"
                      v-if="e.previousEnglish"
                    >
                      {{ e.previousEnglish }} →
                    </span>
                    <span class="english" title="English">{{ e.english }}</span>
                    |
                    <span
                      class="winning"
                      title="Winning"
                      v-bind:dir="$cldrOpts.localeDir"
                      >{{ e.winning }}</span
                    >
                    <template v-if="e.comment">
                      |
                      <span v-html="e.comment" title="comment"></span>
                    </template>
                  </a>
                </span>
                <input
                  v-if="n.notification !== 'Error'"
                  type="checkbox"
                  class="right-control"
                  title="You can hide checked items with the hide checkbox above"
                  v-model="e.checked"
                  @change="
                    (event) => {
                      entryCheckmarkChanged(event, e.xpath, n.notification);
                    }
                  "
                />
              </p>
            </template>
          </template>
        </template>
        <p class="bottom-padding">...</p>
      </section>
    </template>
  </nav>
</template>

<script>
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrCoverage from "../esm/cldrCoverage.js";
import * as cldrDash from "../esm/cldrDash.js";
import * as cldrGui from "../esm/cldrGui.js";
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrStatus from "../esm/cldrStatus.js";

export default {
  props: [],
  data() {
    return {
      data: null,
      fetchErr: null,
      hideChecked: false,
      lastClicked: null,
      loadingMessage: "Loading Dashboard…",
      locale: null,
      localeName: null,
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
    scrollToCategory(event) {
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

    reopen() {
      if (
        cldrStatus.getCurrentLocale() !== this.locale ||
        cldrCoverage.effectiveName(this.locale) !== this.level
      ) {
        this.reloadDashboard();
      }
    },

    handleCoverageChanged(level) {
      console.log("Dashboard changing level: " + level);
      this.reloadDashboard();
    },

    reloadDashboard() {
      this.data = null;
      this.fetchData();
    },

    fetchData() {
      if (!cldrStatus.getSurveyUser()) {
        this.fetchErr = "Please log in to see the Dashboard.";
        return;
      }
      this.locale = cldrStatus.getCurrentLocale();
      this.level = cldrCoverage.effectiveName(this.locale);
      if (!this.locale || !this.level) {
        this.fetchErr = "Please choose a locale and a coverage level first.";
        return;
      }
      this.localeName = cldrLoad.getLocaleName(this.locale);
      this.loadingMessage = `Loading ${this.localeName} dashboard at ${this.level} level`;
      this.reallyFetch();
    },

    reallyFetch() {
      const url = `api/summary/dashboard/${this.locale}/${this.level}`;
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
          this.data = cldrDash.setData(data);
          this.resetScrolling();
        })
        .catch((err) => {
          console.error("Error loading Dashboard data: " + err);
          this.fetchErr = err;
        });
    },

    /**
     * A user has voted. Update the Dashboard as needed.
     *
     * @param json - the response to a request by cldrTable.refreshSingleRow
     */
    updateRow(json) {
      cldrDash.updateRow(this.data, json);
    },

    resetScrolling() {
      setTimeout(function () {
        const el = document.getElementById("DashboardScroller");
        if (el) {
          el.scrollTo(0, 0);
        }
      }, 500 /* half a second */);
    },

    setLastClicked(id) {
      this.lastClicked = id;
    },

    closeDashboard(event) {
      cldrGui.hideDashboard();
    },

    abbreviate(str) {
      if (str === "English_Changed") {
        return "EC";
      } else {
        return str.substr(0, 1); // first letter, e.g., "E" for "Error"
      }
    },

    humanize(str) {
      return str.replaceAll("_", " ");
    },

    entryCheckmarkChanged(event, xpath, category) {
      cldrDash.saveEntryCheckmark(
        event.target.checked,
        xpath,
        category,
        this.locale
      );
    },
  },

  computed: {
    console: () => console,
  },
};
</script>

<style scoped>
.st-sad {
  font-style: italic;
  border: 1px dashed red;
  color: darkred;
}

#DashboardSection {
  border-top: 4px solid #cfeaf8;
  font-size: small;
}

.while-loading {
  padding-top: 3em;
}

header {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  text-align: baseline;
  align-items: center;
  margin: 0;
  padding: 1ex 0;
  background-color: white;
  background-image: linear-gradient(white, #e7f7ff);
}

.dash-closebox {
  margin-left: 1ex;
}

.i-am-dashboard {
  font-weight: bold;
  margin-right: 1ex;
}

.dash-reload {
  margin-right: 2em;
}

p {
  padding: 0.1em;
  margin: 0;
  line-height: 1.75;
  display: flex;
}

p.bottom-padding {
  line-height: 5;
  color: white;
}

.dashEntry {
  display: flex;
  width: 100%;
  flex-direction: row;
  text-align: left;
  align-items: baseline;
}

.notification {
  border: 0.1em solid;
  border-radius: 50%;
  height: 100%;
  padding: 0.1em 0.33em;
  margin-right: 0.5em;
  font-weight: bold;
}

.right-control {
  /* This element will be pushed to the right.
     The elements to the left of it will be pushed to the left. */
  margin-left: auto !important;
  margin-right: 1ex;
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

.winning {
  color: green;
}

a {
  /* enable clicking on space to right of text, as well as on text itself */
  display: block;
  width: 100%;
  color: inherit;
}

a:hover {
  background-color: #ccdfff; /* light blue */
}

.last-clicked {
  background-color: #eee; /* light gray */
}
</style>
