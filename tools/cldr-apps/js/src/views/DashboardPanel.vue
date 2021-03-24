<template>
  <div id="dashboard">
    <p v-if="fetchErr" class="st-sad">Error loading data: {{ fetchErr }}</p>
    <a-spin v-if="!data">
      <i>Loading…</i>
    </a-spin>
    <div v-if="data && !fetchErr" id="scrollingwindow">
      <!-- summary -->
      <h4>
        Coverage level <b>{{ level }}</b
        >:
        <span v-for="n in data.notifications" :key="n.notification">
          <button
            :notification="n.notification"
            class="scrollto"
            v-on:click.prevent="scrollto"
          >
            {{ n.notification }} ({{ n.total }})
          </button>
          &nbsp;&nbsp;
        </span>
      </h4>
      <!-- tables -->
      <div
        v-for="n in data.notifications"
        :key="n.notification"
        class="notificationcontainer"
        v-bind:id="'notification-' + n.notification"
      >
        <h3 v-on:click="n.hidden = !n.hidden" class="collapse-review">
          <i v-if="!n.hidden" class="glyphicon glyphicon-chevron-down" />
          <i v-if="n.hidden" class="glyphicon glyphicon-chevron-right" />

          {{ n.notification }} ({{ n.total }})
        </h3>
        <div class="notificationgroups" v-if="!n.hidden">
          <div class="notificationgroup" v-for="g in n.entries" :key="g.header">
            <div class="notificationgroup-info info">
              <b>{{ g.section }} — {{ g.page }}</b>
              :
              {{ g.header }}
            </div>

            <table
              class="table table-responsive table-fixed-header table-review table-dashboard"
            >
              <thead>
                <tr>
                  <th width="15%">Code</th>
                  <th width="20%">English</th>
                  <th width="20%">Baseline</th>
                  <th width="30%">
                    Winning {{ $cldrOpts.cldrStatus.getNewVersion() }}
                  </th>
                  <th width="5%">Status</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in g.entries" :key="e.xpath" class="data-review">
                  <td class="button-review">
                    <a v-bind:href="'#/' + [locale, g.page, e.xpath].join('/')">
                      <span class="label label-info">
                        {{ e.code }}
                      </span>
                    </a>
                  </td>
                  <td>
                    <span>{{ e.english }}</span>
                    <span class="previousEnglish" v-if="e.previousEnglish"
                      ><b>OLD:</b> {{ e.previousEnglish }}</span
                    >
                  </td>
                  <td>
                    <cldr-value
                      v-bind:value="e.old"
                      v-bind:dir="$cldrOpts.localeDir"
                    />
                  </td>
                  <td>
                    <cldr-value
                      v-bind:value="e.winning"
                      v-bind:dir="$cldrOpts.localeDir"
                    />
                  </td>
                  <td class="button-review">
                    <Popover
                      v-if="true || e.comment"
                      title="Information"
                      trigger="click"
                    >
                      <template #content>
                        <p
                          v-html="
                            e.comment ||
                            categoryComment[n.notification] ||
                            `Unknown type: ${n.notification}`
                          "
                        />
                      </template>
                      <button class="btn btn-default help-comment">
                        <span class="glyphicon glyphicon-info-sign"> </span>
                      </button>
                    </Popover>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: [],
  data() {
    return {
      data: null,
      fetchErr: null,
      locale: null,
      level: null,
      categoryComment: {
        Provisional:
          "Item is provisional, and need additional votes for confirmation.",
        English_Changed:
          "The English version has changed, but the locale has not.",
      },
    };
  },
  created() {
    this.fetchData();
  },
  methods: {
    scrollto: function scrollto(event) {
      const whence = event.target.getAttribute("notification");
      if (this.data && this.data.notifications) {
        for (let n of this.data.notifications) {
          if (n.notification == whence) {
            n.hidden = false;
            const selector = `#notification-${whence}`;
            try {
              const scrollFix = event.target.offsetParent.offsetTop;
              document.querySelector(selector).scrollIntoView(true);
              // Now, scroll BACK because:
              // -1000: to fix the H-alignment
              // offsetTop stuff:  because of the huge 'header' bar , so that the category
              // is actually visible.
              //
              // It's an approximation.
              window.scrollBy(-1000, 0 - scrollFix);
            } catch (e) {
              console.error(
                `Error ${e} trying to scroll to ${selector} for ${whence}`
              );
            }
            return;
          }
        }
        console.error("Click on missing notitication " + whence);
      } else {
        console.error(
          "Click before data loaded on missing notitication " + whence
        );
      }
    },
    handleCoverageChanged(level) {
      console.log("Changing level: " + level);
      this.data = null;
      this.fetchData();
      return true;
    },
    fetchData() {
      const { cldrSurvey, locale, sessionId } = this.$cldrOpts;
      this.locale = locale;
      const level = cldrSurvey.getSurveyUserCov();
      this.level = level;
      console.log("Loading for " + level);
      const session = sessionId;
      if (!locale) {
        this.fetchErr = "Please choose a locale first.";
        return;
      }
      fetch(`api/summary/dashboard/${locale}/${level}?session=${session}`)
        .then((data) => data.json())
        .then((data) => {
          // calculate total counts
          for (let e in data.notifications) {
            const n = data.notifications[e];
            n.total = 0;
            for (let g in n.entries) {
              n.total += n.entries[g].entries.length;
            }
          }
          this.data = data;
        })
        .catch((err) => {
          this.fetchErr = err;
          this.list = [];
        });
    },
  },
};
</script>

<style scoped>
#dashboard {
  margin-top: 1em;
}

.scroller,
.scrollingwindow {
  border: 1px solid gray;
}

.scrollingwindow {
  height: 8em;
}

.scroller {
  /* height: 100%; */
  height: 60vh;
  /* width: 90%; */
  overflow: scroll;
}

.notification {
  font-size: x-large;
}

.header {
  font-size: medium;
}

.dashboardrow {
  height: 32%;
  padding: 0 12px;
  display: flex;
  align-items: center;
}

.h4 {
  font-weight: bold;
}

.previousEnglish {
  display: block;
  color: #900;
}

#components-popover-info-triggerType .ant-btn {
  margin-right: 8px;
}

.collapse-review {
  border-top: 2px solid gray;
  margin-top: 4px;
  padding-top: 5px;
}

.table-dashboard {
  table-layout: fixed;
}

.table-dashboard td {
  background-color: white;
}

.notificationgroup-info {
  font-size: large;
}
</style>
