<script setup>
import { onMounted, ref, reactive } from "vue";

import * as cldrGenerateVxml from "../esm/cldrGenerateVxml.mjs";

const STATUS = cldrGenerateVxml.Status;

let hasPermission = ref(false);
let message = ref("");
let directory = ref("");
let localeId = ref("");
let localesDone = ref(0);
let localesTotal = ref(0);
let percent = ref(0);
let status = ref(STATUS.INIT);
let verificationStatus = ref("");
let verificationFailures = null; /* array of strings */
let verificationWarnings = null; /* array of strings */

function mounted() {
  cldrGenerateVxml.viewMounted(setData);
  hasPermission.value = Boolean(cldrGenerateVxml.canGenerateVxml());
}

onMounted(mounted);

function start() {
  if (hasPermission) {
    message = ref("");
    directory = ref("");
    localeId = ref("");
    localesDone = ref(0);
    localesTotal = ref(0);
    percent = ref(0);
    status.value = STATUS.WAITING;
    verificationStatus = ref("");
    verificationFailures = verificationWarnings = null;
    cldrGenerateVxml.start();
  }
}

function cancel() {
  cldrGenerateVxml.cancel();
  status.value = STATUS.STOPPED;
}

function download() {
  cldrGenerateVxml.download(directory.value);
}

function canCancel() {
  return status.value === STATUS.WAITING || status.value === STATUS.PROCESSING;
}

function setData(data) {
  message.value = data.message;
  percent.value = data.percent;
  status.value = data.status;
  directory.value = data.directory;
  localeId.value = data.localeId;
  localesDone.value = data.localesDone;
  localesTotal.value = data.localesTotal;
  verificationStatus.value = data.verificationStatus;
  verificationFailures = reactive(data.verificationFailures); // array
  verificationWarnings = reactive(data.verificationWarnings); // array
}

function copyDirectory() {
  navigator.clipboard.writeText(directory.value);
}

function progressBarStatus() {
  // Reference: https://ant.design/components/progress#api
  if (status.value === STATUS.STOPPED) {
    return "exception";
  } else if (status.value === STATUS.SUCCEEDED) {
    return "success";
  } else if (percent.value > 0 && percent.value < 100) {
    return "active";
  } else {
    return "normal";
  }
}

defineExpose({
  setData,
});
</script>

<template>
  <div v-if="!hasPermission">
    Please log in as a user with sufficient permissions.
  </div>
  <div v-else>
    <p v-if="status != STATUS.INIT">Generation Status: {{ status }}</p>
    <p class="buttons">
      <button v-if="canCancel()" @click="cancel()">Cancel</button>
      <button v-else @click="start()">Generate VXML Now</button>
    </p>
    <p class="progressBar">
      <a-progress :percent="percent" :status="progressBarStatus()" />
    </p>
    <p v-if="directory">
      <span>Directory created: {{ directory }}</span>
      &nbsp;
      <button @click="copyDirectory()">Copy</button>
      &nbsp;
      <!-- Allow downloading even if STATUS.STOPPED. If generation or verification failed,
        downloading may still help with diagnosing partial/problematic output. -->
      <button
        v-if="status == STATUS.SUCCEEDED || status == STATUS.STOPPED"
        @click="download()"
      >
        Download
      </button>
    </p>
    <p v-if="message">{{ message }}</p>
    <p v-if="localeId">
      Wrote locale: {{ localeId }} ({{ localesDone }} / {{ localesTotal }})
    </p>
    <p v-if="verificationStatus">
      Verification Status: {{ verificationStatus }}
    </p>
    <div v-if="verificationFailures?.length">
      <h2 class="sectionHeader">Verification Failures</h2>
      <p v-for="msg of verificationFailures" :key="msg">{{ msg }}</p>
    </div>
    <div v-if="verificationWarnings?.length">
      <h2 class="sectionHeader">Verification Warnings</h2>
      <p v-for="msg of verificationWarnings" :key="msg">{{ msg }}</p>
    </div>
  </div>
</template>

<style scoped>
.buttons {
  margin: 1em;
}

.progressBar {
  width: 50%;
}

.sectionHeader {
  font-weight: bold;
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-size: 20px;
}
</style>
