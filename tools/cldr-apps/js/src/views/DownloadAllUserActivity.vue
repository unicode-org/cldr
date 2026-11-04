<script setup>
import * as cldrUserListExport from "../esm/cldrUserListExport.mjs";
import * as cldrVettingParticipation from "../esm/cldrVettingParticipation.mjs";
import * as cldrNotify from "../esm/cldrNotify.mjs";

import { ref } from "vue";

const message = ref("");
const percent = ref(0);
const inProgress = ref(false);

function canCancel() {
  return false;
}

function progressBarStatus() {
  return inProgress.value ? "active" : "normal";
}

async function start() {
  inProgress.value = true;
  message.value = `Loading…`;
  const list = await cldrVettingParticipation.getVettingParticipationList();
  message.value = `Loaded ${list.users.length} users`;

  try {
    await cldrUserListExport.downloadAllUserActivity(list.users, (m, p) => {
      message.value = m;
      percent.value = p;
    });
  } catch (e) {
    message.value = `Error: ${e}`;
    cldrNotify.exception(e, `downloadAllUserActivity`);
  }

  inProgress.value = false;
}
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
