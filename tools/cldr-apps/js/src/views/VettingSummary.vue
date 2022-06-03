<template>
  <div v-if="!canUseSummary">
    <p>{{ accessDenied }}</p>
  </div>
  <div v-else>
    <h1 v-if="heading">{{ heading }}</h1>
    <p v-if="whenReceived">Received: {{ whenReceived }}</p>
    <p v-if="helpMessage">{{ helpMessage }}</p>
    <span v-html="output"></span>
    <hr />
    <p v-if="status">Current Status: {{ status }}</p>
    <p v-if="message">
      <span v-html="message"></span>
    </p>
    <div v-if="percent" class="summaryPercent">
      <a-progress :percent="percent" />
    </div>
    <p>
      <button v-if="canStop()" @click="stop()">Stop</button>
      <button
        v-else
        title="Create a new Priority Items Summary (not a snapshot)"
        @click="start()"
      >
        Create New Summary
      </button>
    </p>
    <section v-if="snapshotsAreReady" class="snapSection">
      <h2 class="snapHeading">Snapshots</h2>
      <p>
        <button
          v-if="canCreateSnapshots"
          title="Create a new snapshot of the Priority Items Summary"
          @click="createSnapshot"
        >
          Create New Snapshot
        </button>
        <span v-if="snapshotArray">
          <span v-for="snapshotId of snapshotArray" :key="snapshotId">
            <button
              title="Show the indicated snapshot of the Priority Items Summary"
              @click="showSnapshot(snapshotId)"
            >
              {{ snapshotId }}
            </button>
          </span>
        </span>
      </p>
    </section>

    <hr />

    <section class="snapSection">
      <h2 class="snapHeading">Report Status</h2>
      <button @click="fetchReports">Load</button>
      <table class="reportTable" v-if="reports">
        <thead>
          <tr>
            <th>Locale</th>
            <th>Overall</th>
            <!--
              for per-report breakdown
              <th v-for="type of reports.types" :key="type">{{ humanizeReport(type) }}</th>
            -->
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="locale of Object.keys(reports.byLocale).sort()"
            :key="locale"
          >
            <td>
              <tt>{{ locale }}—{{ humanizeLocale(locale) }}</tt>
            </td>
            <td>
              <span
                class="reportEntry"
                v-for="[kind, count] of Object.entries(
                  reports.byLocale[locale]
                )"
                :key="kind"
              >
                <i v-if="count" :class="reportClass(kind)">&nbsp;</i>
                {{ kind }}={{ count }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script>
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrPriorityItems from "../esm/cldrPriorityItems.js";
import * as cldrText from "../esm/cldrText.js";
import * as cldrReport from "../esm/cldrReport.js";

export default {
  data() {
    return {
      accessDenied: null,
      canCreateSnapshots: false,
      canUseSnapshots: false,
      canUseSummary: false,
      heading: null,
      helpMessage: null,
      message: null,
      output: null,
      percent: 0,
      snapshotArray: null,
      snapshotsAreReady: false,
      status: null,
      whenReceived: null,
      reports: null,
    };
  },

  created() {
    cldrPriorityItems.viewCreated(this.setData, this.setSnapshots);
    this.canUseSummary = cldrPriorityItems.canUseSummary();
    this.canUseSnapshots = cldrPriorityItems.canUseSnapshots();
    this.canCreateSnapshots = cldrPriorityItems.canCreateSnapshots();
    if (!this.canUseSummary) {
      this.accessDenied = cldrText.get("summary_access_denied");
    }
  },

  methods: {
    start() {
      cldrPriorityItems.start();
    },

    stop() {
      cldrPriorityItems.stop();
    },

    showSnapshot(snapshotId) {
      cldrPriorityItems.showSnapshot(snapshotId);
    },

    createSnapshot() {
      cldrPriorityItems.createSnapshot();
    },

    setData(data) {
      this.message = data.message;
      this.percent = data.percent;
      if (data.status) {
        this.status = data.status;
      }
      if (data.output) {
        this.output = data.output;
        this.heading = this.makeHeading(data.snapshotId);
        this.helpMessage = this.makeHelp(data.snapshotId);
        this.whenReceived = this.makeWhenReceived(data.snapshotId);
        this.snapshotsAreReady =
          this.canUseSnapshots &&
          cldrPriorityItems.snapshotIdIsValid(data.snapshotId);
      }
    },

    makeWhenReceived(snapshotId) {
      if (!this.output) {
        return null;
      }
      if (cldrPriorityItems.snapshotIdIsValid(snapshotId)) {
        return null;
      } else {
        // only show "when received" if it's not a snapshot
        return new Date().toString();
      }
    },

    makeHeading(snapshotId) {
      if (!this.output) {
        return null;
      }
      if (cldrPriorityItems.snapshotIdIsValid(snapshotId)) {
        return "Snapshot " + snapshotId;
      } else if (this.canUseSnapshots) {
        return "Latest (not a snapshot)";
      } else {
        return "Latest";
      }
    },

    makeHelp(snapshotId) {
      if (!this.output) {
        return null;
      }
      let help = cldrText.get("summary_help") + " ";
      if (cldrPriorityItems.snapshotIdIsValid(snapshotId)) {
        help += cldrText.get("summary_coverage_neutral");
      } else {
        help += cldrText.get("summary_coverage_org_specific");
      }
      return help;
    },

    setSnapshots(snapshots) {
      this.snapshotArray = snapshots.array.sort().reverse();
      if (!this.output) {
        if (this.snapshotArray[0]) {
          // request this most recent snapshot from the back end
          // -- wait until get response to set snapshotsAreReady
          this.showSnapshot(this.snapshotArray[0]);
        } else {
          // no snapshots are available; we're ready to show the empty menu
          this.snapshotsAreReady = this.canUseSnapshots;
        }
      }
    },

    canStop() {
      return this.status === "WAITING" || this.status === "PROCESSING";
    },

    async fetchReports() {
      this.reports = await cldrReport.fetchAllReports();
    },

    humanizeReport(report) {
      return cldrReport.reportName(report);
    },

    humanizeLocale(locale) {
      return cldrLoad.getLocaleName(locale);
    },

    reportClass(kind) {
      if (kind === "unacceptable") {
        return cldrReport.reportClass(true, false);
      } else if (kind === "acceptable") {
        return cldrReport.reportClass(true, true);
      } else if (kind === "totalVoters") {
        return "totalVoters";
      } else {
        return cldrReport.reportClass(false, false);
      }
    },
  },
};
</script>

<style scoped>
button {
  margin: 1ex;
}

.snapHeading {
  margin-top: 4px;
}

.snapSection {
  border: 2px solid gray;
  padding: 4px;
  background-color: #ccdfff; /* light blue */
}

.summaryPercent {
  margin: 1ex;
}

.reportTable th,
.reportTable td {
  padding: 0.5em;
  border-right: 2px solid gray;
}

.reportEntry {
  border-right: 1px solid gray;
  padding-right: 0.5em;
  display: table-cell;
}
</style>
