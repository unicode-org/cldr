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
    <section v-if="canUseSnapshots" class="snapSection">
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
  </div>
</template>

<script>
import * as cldrPriorityItems from "../esm/cldrPriorityItems.js";
import * as cldrText from "../esm/cldrText.js";

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
      status: null,
      whenReceived: null,
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
      }
    },

    makeWhenReceived(snapshotId) {
      if (!this.output) {
        return null;
      }
      if (
        snapshotId &&
        snapshotId !== cldrPriorityItems.SNAPID_NOT_APPLICABLE
      ) {
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
      if (
        snapshotId &&
        snapshotId !== cldrPriorityItems.SNAPID_NOT_APPLICABLE
      ) {
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
      if (
        snapshotId &&
        snapshotId !== cldrPriorityItems.SNAPID_NOT_APPLICABLE
      ) {
        help += cldrText.get("summary_coverage_neutral");
      } else {
        help += cldrText.get("summary_coverage_org_specific");
      }
      return help;
    },

    setSnapshots(snapshots) {
      this.snapshotArray = snapshots.array.sort().reverse();
      if (!this.output && this.snapshotArray[0]) {
        this.showSnapshot(this.snapshotArray[0]);
      }
    },

    canStop() {
      return this.status === "WAITING" || this.status === "PROCESSING";
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
</style>
