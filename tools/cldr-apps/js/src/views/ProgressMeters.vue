<template>
  <section id="ProgressMeters" v-if="!hide">
    <a-tooltip v-if="pageMeter.getPresent()" :title="pageMeter.getTitle()">
      <a-progress
        :percent="pageMeter.getPercent()"
        strokeColor="blue"
        type="circle"
        :width="32"
      />
    </a-tooltip>
    &nbsp;
    <a-tooltip v-if="voterMeter.getPresent()" :title="voterMeter.getTitle()">
      <a-progress
        :percent="voterMeter.getPercent()"
        strokeColor="orange"
        type="circle"
        :width="32"
      />
    </a-tooltip>
    &nbsp;
    <a-tooltip v-if="localeMeter.getPresent()" :title="localeMeter.getTitle()">
      <a-progress
        :percent="localeMeter.getPercent()"
        strokeColor="green"
        type="circle"
        :width="32"
      />
    </a-tooltip>
  </section>
</template>

<script>
import * as cldrProgress from "../esm/cldrProgress.js";

export default {
  props: [],
  data() {
    return {
      hide: true,
      pageMeter: new cldrProgress.MeterData(),
      voterMeter: new cldrProgress.MeterData(),
      localeMeter: new cldrProgress.MeterData(),
    };
  },

  methods: {
    setHidden(hidden) {
      this.hide = hidden;
    },

    updatePageMeter(meterData) {
      this.pageMeter = meterData;
    },

    updateVoterMeter(meterData) {
      this.voterMeter = meterData;
    },

    updateLocaleMeter(meterData) {
      this.localeMeter = meterData;
    },
  },
};
</script>

<style scoped>
#ProgressMeters {
  margin-left: 1em;
  margin-right: 1em;
}
</style>
