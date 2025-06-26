<script setup>
import { onMounted, ref, reactive } from "vue";

import * as cldrVettingParticipation from "../esm/cldrVettingParticipation.mjs";

const STATUS = cldrVettingParticipation.Status;

let hasPermission = ref(false);
let message = ref("");
let percent = ref(0);
let status = ref(STATUS.INIT);
let table = null;

function mounted() {
  cldrVettingParticipation.viewMounted(setData);
  hasPermission.value = Boolean(cldrVettingParticipation.hasPermission());
}

onMounted(mounted);

function start() {
  if (hasPermission) {
    message.value = "";
    percent.value = 0;
    status.value = STATUS.WAITING;
    cldrVettingParticipation.start();
  }
}

function cancel() {
  cldrVettingParticipation.cancel();
  message.value = "";
  percent.value = 0;
  status.value = STATUS.STOPPED;
  table = null;
}

function canCancel() {
  return status.value === STATUS.WAITING || status.value === STATUS.PROCESSING;
}

function setData(data) {
  message.value = data.message;
  percent.value = data.percent;
  if (data.status) {
    status.value = data.status;
  }
  if (data.table) {
    table = reactive(data.table);
  }
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

function saveAsSheet() {
  return cldrVettingParticipation.saveAsSheet();
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
      <button v-else @click="start()">Generate Table Now</button>
    </p>
    <p class="progressBar">
      <a-progress :percent="percent" :status="progressBarStatus()" />
    </p>
    <p v-if="message">{{ message }}</p>
    <hr />
    <p class="buttons">
      <button v-if="table" @click="saveAsSheet">
        Save as Spreadsheet .xlsx
      </button>
    </p>
    <table v-if="table">
      <thead>
        <tr>
          <th v-for="cell of table[0]" :key="cell">
            {{ cell }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) of table" :key="row">
          <td v-if="index > 0" v-for="cell of row" :key="cell">
            {{ cell }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.buttons {
  margin: 1em;
}

.progressBar {
  width: 50%;
}

th,
td {
  padding: 0.5em;
  border: 1px solid black;
}

th {
  background-color: lightgray;
}
</style>
