<script setup>
import * as cldrUserListExport from "../esm/cldrUserListExport.mjs";

import { ref } from "vue";

const message = ref("");
const percent = ref(0);

function canCancel() {
  return false;
}

function progressBarStatus() {
  return "normal";
}

function start() {}
</script>

<template>
  <p>
    This section will download a large .xlsx file with all of your user’s
    activity for this release.
  </p>

  <button v-if="canCancel()" @click="cancel()">Cancel</button>
  <button v-else @click="start()">Generate .xlsx</button>
  <br />
  <p class="progressBar">
    <a-progress :percent="percent" :status="progressBarStatus()" />
  </p>
  <p v-if="message">{{ message }}</p>
</template>
