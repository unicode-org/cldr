<script setup>
import { onMounted, ref } from "vue";

import * as cldrFlagged from "../esm/cldrFlagged.mjs";
import * as cldrText from "../esm/cldrText.mjs";

const STATUS = cldrFlagged.Status;

let hasPermission = ref(false);
let message = ref("");
let percent = ref(0);
let status = ref(STATUS.INIT);
let tableBody = null;
let tableHeader = null;
let tableComments = null;
let localeIdColumnIndex = ref(-1);
let pathColumnIndex = ref(-1);
let guidance = cldrText.get("flaggedGuidance");
let totalCountText = cldrText.get("flaggedTotalCount");
let gotTable = ref(false);

function mounted() {
  cldrFlagged.viewMounted(setData);
  hasPermission.value = Boolean(cldrFlagged.hasPermission());
  if (cldrFlagged.FLAGGED_RESPONSE_IS_FAST) {
    start();
  }
}

onMounted(mounted);

function start() {
  if (hasPermission) {
    message.value = "";
    percent.value = 0;
    status.value = STATUS.WAITING;
    cldrFlagged.start();
  }
}

function cancel() {
  cldrFlagged.cancel();
  message.value = "";
  percent.value = 0;
  status.value = STATUS.STOPPED;
  tableBody = tableHeader = tableComments = null;
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
  if (data.tableHeader && data.tableBody && data.tableComments) {
    tableHeader = ref(data.tableHeader);
    tableBody = ref(data.tableBody);
    tableComments = ref(data.tableComments);
    localeIdColumnIndex.value = data.localeIdColumnIndex;
    pathColumnIndex.value = data.pathColumnIndex;
    gotTable.value = true;
  } else {
    gotTable.value = false;
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

function localeLink(localeId) {
  return "#/" + localeId;
}

function pathLink(row, pathHeader) {
  const localeId = row[localeIdColumnIndex.value];
  return cldrFlagged.xpathLinkFromLocaleAndHeader(localeId, pathHeader);
}

function saveAsSheet() {
  return cldrFlagged.saveAsSheet();
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
    <p>
      <i>{{ guidance }}</i>
    </p>
    <div v-if="!cldrFlagged.FLAGGED_RESPONSE_IS_FAST">
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
    </div>
    <div class="special_flagged" v-if="gotTable">
      <h3>{{ totalCountText + tableBody.length }}</h3>
      <p class="buttons">
        <button @click="saveAsSheet">Save as Spreadsheet .xlsx</button>
      </p>
      <table>
        <thead>
          <tr>
            <th v-for="(cell, index) of tableHeader" :key="cell">
              <a-tooltip :title="tableComments[index]">
                {{ cell }}
              </a-tooltip>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row of tableBody" :key="row">
            <td v-for="(cell, index) of row" :key="cell">
              <div v-if="localeIdColumnIndex == index">
                <a-tooltip title="Show locale ↗">
                  <a :href="localeLink(cell)" target="_blank">{{ cell }}</a>
                </a-tooltip>
              </div>
              <div v-else-if="pathColumnIndex == index">
                <a-tooltip title="Show path ↗">
                  <a :href="pathLink(row, cell)" target="_blank">{{ cell }}</a>
                </a-tooltip>
              </div>
              <div v-else>
                {{ cell }}
              </div>
            </td>
          </tr>
        </tbody>
      </table>
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

th,
td {
  padding: 0.5em;
  border: 1px solid black;
}

th {
  background-color: lightgray;
  position: sticky;
  top: 0;
}
</style>
