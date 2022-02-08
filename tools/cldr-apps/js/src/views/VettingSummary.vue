<template>
  <div>
    <p>Current Status: {{ statusData.status }}</p>
    <p>
      <button @click="stop()">Stop</button>
      <button @click="start()">Refresh</button>
    </p>
    <p v-if="statusData?.ret">
      Message:
      <span v-html="statusData.ret"></span>
    </p>

    <section v-if="canUseSnapshots" class="snapSection">
      <h2 class="snapHeading">Snapshots</h2>
      <p>
        <button
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
    <span v-html="statusData.output"></span>
  </div>
</template>

<script>
import * as cldrPriorityItems from "../esm/cldrPriorityItems.js";

export default {
  data() {
    return {
      statusData: {
        status: "Loading…",
        ret: null,
        output: null,
      },
      canUseSnapshots: false,
      snapshotArray: null,
      heading: null,
      whenReceived: null,
    };
  },
  created() {
    cldrPriorityItems.viewCreated(this.setData, this.setSnapshots);
    this.canUseSnapshots = cldrPriorityItems.canUseSnapshots();
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
      // can't just set this.statusData = data; data.ret may be updated with a description
      // of a task in progress, while data.output may be undefined until we get READY
      this.statusData.ret = data.ret;
      if (data.status) {
        this.statusData.status = data.status;
      }
      if (data.output) {
        this.statusData.output = data.output;
        this.heading = this.makeHeading(data?.snapshotId);
        this.whenReceived = new Date().toString();
      }
    },

    makeHeading(snapshotId) {
      if (!this.statusData?.output) {
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
</style>
