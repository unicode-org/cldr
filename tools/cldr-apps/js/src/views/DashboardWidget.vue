<template>
  <nav id="DashboardSection" class="halfheight">
    <p v-if="fetchErr" class="st-sad">{{ fetchErr }}</p>
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
          ✕
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
        <span v-for="cat of data.cats" :key="cat">
          <template v-if="data.catSize[cat]">
            <input
              type="checkbox"
              :title="describeShow(cat)"
              :id="'dash-cat-checkbox-' + cat"
              :checked="!catCheckboxIsUnchecked[cat]"
              @change="
                (event) => {
                  catCheckmarkChanged(event, cat);
                }
              "
            />
            <button
              :category="cat"
              class="scrollto cldr-nav-btn"
              v-on:click.prevent="scrollToCategory"
              :title="describeScrollTo(cat)"
              :disabled="catIsHidden[cat]"
            >
              {{ humanize(cat) }} ({{ data.catSize[cat] }})
            </button>
            &nbsp;&nbsp;
          </template>
        </span>
        <span class="right-control">
          <a-spin v-if="downloadMessage">
            <i>{{ downloadMessage }}</i>
          </a-spin>
          <button
            v-if="!downloadMessage"
            class="cldr-nav-btn"
            @click="downloadXlsx"
          >
            Download…
          </button>
          <input
            type="checkbox"
            title="Hide checked items"
            id="hideChecked"
            v-model="hideChecked"
            @change="hideCheckedChanged"
          /><label for="hideChecked">&nbsp;hide</label>
        </span>
      </header>
      <section id="DashboardScroller">
        <template v-if="updatingVisibility">
          <!-- for unknown reason, the a-spin fails to appear on current Chrome/Firefox if any :delay is specified here -->
          <a-spin size="large" />
        </template>
        <template v-else>
          <!-- Use "item" instead of "entry" here for compatibility with DynamicScroller -->
          <DynamicScroller
            class="scroller"
            :items="filteredEntries"
            key-field="xpstrid"
            :min-item-size="54"
            ref="dynamicScrollerRef"
          >
            <template v-slot="{ item, index, active }">
              <DynamicScrollerItem
                :item="item"
                :active="active"
                :size-dependencies="[item.comment, item.english, item.winning]"
                :data-index="index"
              >
                <p
                  :class="
                    'dash-' +
                    (lastClicked === item.xpstrid ? ' last-clicked' : '')
                  "
                  :key="'dash-item-' + item.xpstrid"
                  :id="'dash-item-' + item.xpstrid"
                >
                  <span class="dashEntry">
                    <a
                      v-bind:href="getLink(locale, item)"
                      @click="() => setLastClicked(item.xpstrid)"
                    >
                      <span v-bind:key="cat" v-for="cat of item.cats">
                        <span
                          v-if="!catIsHidden[cat]"
                          class="category"
                          :title="describeAbbreviation(cat)"
                          >{{ abbreviate(cat) }}</span
                        >
                      </span>
                      <span class="section-page" title="section—page">{{
                        humanize(item.section + "—" + item.page)
                      }}</span>
                      |
                      <span
                        v-if="item.header"
                        class="entry-header"
                        title="entry header"
                        >{{ item.header }}</span
                      >
                      |
                      <span class="code" title="code">{{ item.code }}</span>
                      |
                      <cldr-value
                        class="previous-english"
                        title="previous English"
                        lang="en"
                        dir="ltr"
                        v-if="item.previousEnglish"
                        >{{ item.previousEnglish }} →</cldr-value
                      >
                      <cldr-value
                        class="english"
                        lang="en"
                        dir="ltr"
                        title="English"
                        v-if="item.english"
                        >{{ item.english }}</cldr-value
                      >
                      |
                      <cldr-value
                        v-if="item.winning"
                        class="winning"
                        title="Winning"
                        >{{ item.winning }}</cldr-value
                      >
                      <template v-if="item.comment">
                        |
                        <span v-html="item.comment" title="comment"></span>
                      </template>
                      <span v-if="item.cats.has('Reports')"
                        >{{ humanizeReport(item.code) }} Report</span
                      >
                    </a>
                  </span>
                  <input
                    v-if="canBeHidden(item.cats)"
                    type="checkbox"
                    class="right-control"
                    title="You can hide checked items with the hide checkbox above"
                    v-model="item.checked"
                    @change="
                      (event) => {
                        entryCheckmarkChanged(event, item);
                      }
                    "
                  />
                </p>
              </DynamicScrollerItem>
            </template>
          </DynamicScroller>
          <p class="bottom-padding">...</p>
        </template>
      </section>
    </template>
  </nav>
</template>

<script>
import * as cldrCoverage from "../esm/cldrCoverage.mjs";
import * as cldrDashContext from "../esm/cldrDashContext.mjs";
import * as cldrDashData from "../esm/cldrDashData.mjs";
import * as cldrLoad from "../esm/cldrLoad.mjs";
import * as cldrNotify from "../esm/cldrNotify.mjs";
import * as cldrReport from "../esm/cldrReport.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";
import * as cldrText from "../esm/cldrText.mjs";
import { nextTick } from "vue";

export default {
  props: {
    items: Array,
  },
  data() {
    return {
      data: null,
      fetchErr: null,
      filteredEntries: null,
      hideChecked: false,
      lastClicked: null,
      loadingMessage: "Loading Dashboard…",
      locale: null,
      localeName: null,
      level: null,
      downloadMessage: null,
      catCheckboxIsUnchecked: {}, // default unchecked = false, checked = true
      catIsHidden: {}, // default hidden = false, visible = true
      updatingVisibility: false,
    };
  },

  created() {
    if (cldrStatus.getPermissions()?.userIsTC) {
      this.catIsHidden["Abstained"] = this.catCheckboxIsUnchecked[
        "Abstained"
      ] = true;
    }
    this.fetchData();
  },

  methods: {
    getLink(locale, entry) {
      if (entry.cats.has("Reports")) {
        return `#r_${entry.code}/${locale}`;
      } else {
        return `#/${locale}/${entry.page}/${entry.xpstrid}`;
      }
    },

    scrollToCategory(event) {
      const cat = event.target.getAttribute("category");
      const xpstrid = this.data.catFirst[cat];
      if (xpstrid) {
        const selector = "#dash-item-" + xpstrid;
        const el = document.querySelector(selector);
        if (el) {
          el.scrollIntoView(true);
        } else {
          // Generally el is null with DynamicScroller so try this instead.
          // The method scrollToItem appears to be internal, undocumented, but this works.
          for (let i = 0; i < this.filteredEntries.length; i++) {
            const entry = this.filteredEntries[i];
            if (entry.xpstrid == xpstrid) {
              const scroller = this.$refs.dynamicScrollerRef;
              if (!scroller) {
                this.console.warn("No scroller for scrollToCategory");
              } else if (!scroller.scrollToItem) {
                this.console.warn(
                  "No scroller.scrollToItem for scrollToCategory"
                );
              } else {
                scroller.scrollToItem(i);
              }
              return;
            }
          }
          this.console.warn("No xpstrid for scrollToCategory");
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
      if (this.level !== level) {
        this.reloadDashboard();
      }
    },

    reloadDashboard() {
      this.data = null;
      this.fetchData();
    },

    fetchData() {
      this.locale = cldrStatus.getCurrentLocale();
      this.level = cldrCoverage.effectiveName(this.locale);
      if (!this.locale || !this.level) {
        this.fetchErr = "Please choose a locale and a coverage level first.";
        return;
      }
      this.localeName = cldrLoad.getLocaleName(this.locale);
      this.loadingMessage = `Loading ${this.localeName} dashboard at ${this.level} level`;
      cldrDashData.doFetch(this.setData);
      this.fetchErr = cldrDashData.getFetchError();
    },

    setData(data) {
      this.data = data;
      this.filterEntries();
      this.resetScrolling();
    },

    filterEntries() {
      this.filteredEntries = new Array();
      for (let entry of this.data.entries) {
        if (
          this.anyCatIsShown(entry.cats) &&
          !(this.hideChecked && entry.checked)
        ) {
          this.filteredEntries.push(entry);
        }
      }
    },

    downloadXlsx() {
      cldrDashData
        .downloadXlsx(
          this.data,
          this.locale,
          (status) => (this.downloadMessage = status)
        )
        .then(
          () => (this.downloadMessage = null),
          (err) => {
            console.error(err);
            cldrNotify.exception(err, `Loading ${this.locale} Dash.xlsx`);
            this.downloadMessage = null;
          }
        );
    },

    /**
     * A user has voted. Update the Dashboard as needed.
     *
     * @param json - the response to a request by cldrTable.refreshSingleRow
     */
    updatePath(json) {
      cldrDashData.updatePath(this.data, json);
      this.filterEntries();
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

    closeDashboard() {
      cldrDashContext.hide();
    },

    abbreviate(category) {
      // The category is like "English_Changed"; also allow "English Changed" with space not underscore
      if (category.toLowerCase().replaceAll(" ", "_") === "english_changed") {
        return "EC";
      } else {
        return category.substr(0, 1); // first letter, like "E" for "Error"
      }
    },

    describeShow(category) {
      return `Show this notification category [${this.humanize(
        category
      )}]: ${this.describe(category)}`;
    },

    describeScrollTo(category) {
      return `Scroll to this notification category [${this.humanize(
        category
      )}]: ${this.describe(category)}`;
    },

    describeAbbreviation(category) {
      return `Notification category [${this.humanize(
        category
      )}]: ${this.describe(category)}`;
    },

    describe(category) {
      // The category is like "English_Changed" or "English Changed"
      // The corresponding key is like "notification_category_english_changed"
      const key =
        "notification_category_" + category.toLowerCase().replaceAll(" ", "_");
      let description = cldrText.get(key);
      if (description === key) {
        console.error(
          "Dashboard is missing a description for the category: " + category
        );
        description = this.humanize(category);
      }
      return description;
    },

    humanize(str) {
      // For categories like "English_Changed", page names like "Languages_K_N",
      // and section names like "Locale_Display_Names"
      return str.replaceAll("_", " ");
    },

    humanizeReport(report) {
      return cldrReport.reportName(report);
    },

    entryCheckmarkChanged(event, entry) {
      cldrDashData.saveEntryCheckmark(event.target.checked, entry, this.locale);
    },

    catCheckmarkChanged(event, category) {
      // setTimeout is intended to solve a weakness in the Vue implementation: if the number of
      // notifications is large, the checkbox in the header can take a second or even a minute
      // to change its visible state in response to the user's click, during which time
      // the user may click again thinking the first click wasn't recognized. Postponing
      // the DOM update of thousands of rows should help ensure that the header checkbox updates
      // without delay.
      // Also the booleans catCheckboxIsUnchecked and catIsHidden are distinct in order for
      // the checkbox itself to update immediately even if the rows for the corresponding
      // category may take a long time to update.
      // Unfortunately, neither of these mechanisms seems guaranteed to prevent a very very
      // long delay between the time the user clicks the checkbox and the time that the checkbox
      // changes its state.
      // NOTE: this complication may be unnecessary now that DashboardScroller is in use.
      this.catCheckboxIsUnchecked[category] = !event.target.checked; // redundant?
      const USE_NEXT_TICK = true;
      this.updatingVisibility = true;
      if (USE_NEXT_TICK) {
        nextTick().then(() => {
          this.updateVisibility(event.target.checked, category);
        });
      } else {
        const DELAY_FOR_VISIBILITY_UPDATE = 100; // milliseconds
        setTimeout(
          () => this.updateVisibility(event.target.checked, category),
          DELAY_FOR_VISIBILITY_UPDATE
        );
      }
    },

    updateVisibility(checked, category) {
      this.catIsHidden[category] = !checked;
      this.filterEntries();
      this.updatingVisibility = false;
    },

    hideCheckedChanged() {
      this.filterEntries();
    },

    canBeHidden(cats) {
      // All categories can be hidden except Error and Missing
      // cats is a Set, not an array
      return !Array.from(cats).some(
        (cat) => cat === "Error" || cat === "Missing"
      );
    },

    anyCatIsShown(cats) {
      // cats is a Set, not an array
      return Array.from(cats).some((cat) => !this.catIsHidden[cat]);
    },
  },

  computed: {
    console: () => console,
  },
};
</script>

<style scoped>
.scroller {
  height: 100%;
}

.st-sad {
  font-style: italic;
  border: 1px dashed red;
  color: darkred;
}

#DashboardSection {
  border-top: 4px solid #cfeaf8;
  font-size: small;
}

#DashboardScroller {
  /* Compare sidebyside-scrollable in redesign.css
   Here we want overflow: hidden not auto, since DynamicScroller provides its own scrollbar */
  overflow: hidden;
  margin: 4px;
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

.category {
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
