<template>
  <section id="ProgressMeters" v-if="!hide">
    <a-progress
      v-if="sectionMeter.getPresent()"
      :percent="sectionMeter.getPercent()"
      :title="sectionMeter.getTitle()"
      strokeColor="blue"
      type="circle"
      :width="32"
    />
    &nbsp;
    <a-progress
      v-if="voterMeter.getPresent()"
      :percent="voterMeter.getPercent()"
      :title="voterMeter.getTitle()"
      strokeColor="orange"
      type="circle"
      :width="32"
    />
    &nbsp;
    <a-progress
      v-if="localeMeter.getPresent()"
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
      sectionMeter: new cldrProgress.MeterData(),
      voterMeter: new cldrProgress.MeterData(),
      localeMeter: new cldrProgress.MeterData(),
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
