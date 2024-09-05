<script setup>
import { onMounted, ref } from "vue";

import * as cldrGenerateVxml from "../esm/cldrGenerateVxml.mjs";

const STATUS = cldrGenerateVxml.Status;

let hasPermission = ref(false);
let message = ref("");
let output = ref("");
let percent = ref(0);
let status = ref(STATUS.INIT);

function mounted() {
  cldrGenerateVxml.viewMounted(setData);
  hasPermission.value = Boolean(cldrGenerateVxml.canGenerateVxml());
}

onMounted(mounted);

function start() {
  if (hasPermission) {
    cldrGenerateVxml.start();
    status.value = STATUS.WAITING;
  }
}

function stop() {
  cldrGenerateVxml.stop();
  status.value = STATUS.STOPPED;
}

function canStop() {
  return status.value === STATUS.WAITING || status.value === STATUS.PROCESSING;
}

function setData(data) {
  message.value = data.message;
  percent.value = data.percent;
  status.value = data.status;
  output.value = data.output;
}

defineExpose({
  setData,
});
</script>

<template>
  <div v-if="!hasPermission">Please log in as Admin to use this feature.</div>
  <div v-else>
    <p v-if="status != STATUS.INIT">Current Status: {{ status }}</p>
    <p v-if="message">
      <span v-html="message"></span>
    </p>
    <p class="buttons">
      <button v-if="canStop()" @click="stop()">Stop</button>
      <button v-else @click="start()">Generate VXML Now</button>
    </p>
    <p class="progressBar">
      <a-progress :percent="percent" />
    </p>
    <p v-html="output"></p>
  </div>
</template>

<style scoped>
.buttons {
  margin: 1em;
}

.progressBar {
  width: 50%;
}
</style>
