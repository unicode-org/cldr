<template>
  <section id="ProgressMeters" v-if="!hide">
    <a-progress
      :percent="sectionMeter.getPercent()"
      :title="sectionMeter.getTitle()"
      strokeColor="blue"
      type="circle"
      :width="32"
    />
    &nbsp;
    <a-progress
      :percent="voterMeter.getPercent()"
      :title="voterMeter.getTitle()"
      strokeColor="orange"
      type="circle"
      :width="32"
    />
    &nbsp;
    <a-progress
      :percent="localeMeter.getPercent()"
      :title="localeMeter.getTitle()"
      strokeColor="green"
      type="circle"
      :width="32"
    />
  </section>
</template>

<script>
import * as cldrProgress from "../esm/cldrProgress.js";

export default {
  props: [],
  data() {
    return {
      hide: true,
      sectionMeter: new cldrProgress.MeterData("", 0, 0, 0),
      voterMeter: new cldrProgress.MeterData("", 0, 0, 0),
      localeMeter: new cldrProgress.MeterData("", 0, 0, 0),
    };
  },

  methods: {
    setHidden(hidden) {
      this.hide = hidden;
    },

    updateSectionMeter(meterData) {
      this.sectionMeter = meterData;
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
