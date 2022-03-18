<template>
  <div>
    <p v-if="status">Current Status: {{ status }}</p>
    <p>
      <button @click="stop()">Stop</button>
      <button @click="start()">Refresh</button>
    </p>
    <p v-if="message">
      <span v-html="message"></span>
    </p>
    <div v-if="percent" class="summaryPercent">
      <a-progress :percent="percent" />
    </div>
    <section v-if="canUseSnapshots" class="snapSection">
      <h2 class="snapHeading">Snapshots</h2>
      <p>
        <button
          v-if="canCreateSnapshots"
          title="Create a new snapshot of the Priority Items Summary now"
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

    <h1 v-if="heading">{{ heading }}</h1>
    <p v-if="whenReceived">Received: {{ whenReceived }}</p>
    <p v-if="helpMessage">{{ helpMessage }}</p>
    <span v-html="output"></span>
  </div>
</template>

<script>
import * as cldrPriorityItems from "../esm/cldrPriorityItems.js";
import * as cldrText from "../esm/cldrText.js";

export default {
  data() {
    return {
      status: "",
      message: null,
      percent: 0,
      output: null,
      canUseSnapshots: false,
      canCreateSnapshots: false,
      snapshotArray: null,
      heading: null,
      whenReceived: null,
      helpMessage: null,
    };
  },

  created() {
    cldrPriorityItems.viewCreated(this.setData, this.setSnapshots);
    this.canUseSnapshots = cldrPriorityItems.canUseSnapshots();
    this.canCreateSnapshots = cldrPriorityItems.canCreateSnapshots();
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
        this.heading = this.makeHeading(data?.snapshotId);
        this.helpMessage = makeHelp(data?.snapshotId);
        this.whenReceived = new Date().toString();
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
    },
  },
};
</script>

<style scoped>
h1,
h2,
h3 {
  margin-top: 1em;
}

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
