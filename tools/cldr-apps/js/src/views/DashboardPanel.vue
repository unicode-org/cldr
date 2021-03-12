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
          <span> {{ n.notification }} ({{ n.total }}) </span>
          &nbsp;&nbsp;
        </span>
      </h4>
      <!-- tables -->
      <div
        v-for="n in data.notifications"
        :key="n.notification"
        class="notificationcontainer"
      >
        <h3 v-on:click="n.hidden = !n.hidden" class="collapse-review">
          <i v-if="!n.hidden" class="glyphicon glyphicon-chevron-down" />
          <i v-if="n.hidden" class="glyphicon glyphicon-chevron-right" />

          {{ n.notification }} ({{ n.total }})
        </h3>
        <div class="notificationgroup" v-if="!n.hidden">
          <table
            v-for="g in n.entries"
            :key="g.header"
            class="table table-responsive table-fixed-header table-review"
          >
            <thead>
              <tr class="info">
                <td colspan="5">
                  <b>{{ g.section }} — {{ g.page }}</b>
                  :
                  {{ g.header }}
                </td>
              </tr>
              <tr>
                <th>Code</th>
                <th>English</th>
                <th>Baseline</th>
                <th>Winning {{ cldrOpts.cldrStatus.getNewVersion() }}</th>
                <th>Action</th>
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
                  {{ e.english }}
                </td>
                <td>
                  <cldr-value
                    v-bind:value="e.old"
                    v-bind:dir="cldrOpts.localeDir"
                  />
                </td>
                <td>
                  <cldr-value
                    v-bind:value="e.winning"
                    v-bind:dir="cldrOpts.localeDir"
                  />
                </td>
                <td class="button-review">
                  <Popover v-if="e.comment" title="Information" trigger="click">
                    <template #content>
                      <p v-html="e.comment" />
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
</template>

<script>
export default {
  props: ["specialPage", "cldrOpts"],
  data() {
    return {
      data: null,
      fetchErr: null,
      locale: null,
      level: null,
    };
  },
  created() {
    this.fetchData();
  },
  methods: {
    handleCoverageChanged(level) {
      console.log("Changing level: " + level);
      this.data = null;
      this.fetchData();
      return true;
    },
    fetchData() {
      const { cldrSurvey, locale, sessionId } = this.cldrOpts;
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

#components-popover-info-triggerType .ant-btn {
  margin-right: 8px;
}

.collapse-review {
  border-top: 2px solid gray;
  margin-top: 4px;
  padding-top: 5px;
}
</style>
