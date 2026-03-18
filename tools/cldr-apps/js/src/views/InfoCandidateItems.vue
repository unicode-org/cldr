<template>
  <div v-if="rowFlagged">
    <img src="flag.png" :title="flagDescription" /><br />
    <div class="helpContent">{{ rowFlaggedMessage }}</div>
  </div>
  <div v-if="overrideMessage" class="helpContent">{{ overrideMessage }}</div>
  <div class="info-candidate-item" v-for="item in candidateItems" :key="item">
    <a-popover overlayClassName="overlay-style">
      <template #content
        ><div>
          {{ valueTooltip }}
        </div></template
      >
      <span :class="getItemClass(item)" :lang="language" :dir="direction">{{
        item.displayValue
      }}</span>
    </a-popover>
    <span v-if="item.isBaselineValue" class="baseline-value">
      <a-popover overlayClassName="overlay-style">
        <template #content
          ><div>
            {{ baselineHover }}
            <a :href="baselineLinkUrl" target="_blank">{{
              baselineLinkText
            }}</a>
          </div></template
        >
        <img src="star.png" />
      </a-popover>
    </span>
    <div v-if="item.elaboration">{{ item.elaboration }}</div>
    <Suspense>
      <InfoVoteTable
        :totalVoteCount="item.voteTableData.totalVoteCount"
        :voteRows="getVoteRows(item)"
        :baileyClass="getBaileyClass(item)"
      />
    </Suspense>
  </div>
  <div>
    <a class="show-transcript" @click="toggleTranscript">
      <span
        class="glyphicon glyphicon-info-sign show-transcript"
        title="Explain vote counts"
      ></span
    ></a>
  </div>
  <div v-if="transcriptVisible" class="transcript-container visible">
    <pre class="transcript-text"> {{ transcript }}</pre>
  </div>
  <div v-else><br /></div>

  <p v-if="isLocked" class="flag-or-lock-alert">
    {{ valueIsLockedMessage }}
  </p>
  <p v-if="explainFlag" class="flag-or-lock-alert">
    {{ flagExplanationStart }} <a :href="flagURL"> {{ flagLinkText }}</a>
    {{ flagExplanationEnd }}
  </p>
  <div v-if="reqVoteMessage" class="requirement-style">
    {{ reqVoteMessage }}
  </div>
</template>

<script setup>
import { ref } from "vue";

import InfoVoteTable from "./InfoVoteTable.vue";

import * as cldrText from "../esm/cldrText.mjs";

const valueTooltip = ref(cldrText.get("voteInfo_candidate_item_desc"));
const baselineHover = ref(cldrText.get("voteInfo_baseline_short_desc"));
const baselineLinkText = ref(cldrText.get("voteInfo_baseline_link_text"));
const baselineLinkUrl = ref(cldrText.get("voteInfo_baseline_link_url"));
const flagDescription = ref(cldrText.get("flag_desc"));
const valueIsLockedMessage = ref(cldrText.get("valueIsLocked"));
const flagExplanationStart = ref(cldrText.get("flagExplanationStart"));
const flagURL = ref(cldrText.get("flagURL"));
const flagLinkText = ref(cldrText.get("flagLinkText"));
const flagExplanationEnd = ref(cldrText.get("flagExplanationEnd"));

const candidateItems = ref(null);
const reqVoteMessage = ref(null);
const language = ref(null);
const direction = ref(null);
const transcriptVisible = ref(false);
const transcript = ref(null);
const overrideMessage = ref(null);
const explainFlag = ref(false);
const rowFlagged = ref(false);
const rowFlaggedMessage = ref(null);
const isLocked = ref(false);

function setData(data) {
  candidateItems.value = data.candidateItems;
  reqVoteMessage.value = data.reqVoteMessage;
  transcript.value = data.transcript;
  language.value = data.language;
  direction.value = data.direction;
  assertValidDirection(direction.value);
  overrideMessage.value = data.overrideMessage;
  explainFlag.value = Boolean(data.explainFlag);
  rowFlagged.value = Boolean(data.rowFlagged);
  rowFlaggedMessage.value = rowFlagged.value ? cldrText.get("flag_desc") : "";
  isLocked.value = Boolean(data.isLocked);
}

function assertValidDirection(d) {
  if (d && d != "rtl" && d != "ltr") {
    throw Error(`${d} should be either ltr or rtl`);
  }
}

function getVoteRows(item) {
  if (!item?.voteTableData?.totalVoteCount) {
    return null;
  }
  return item.voteTableData.voteRows;
}

function getItemClass(item) {
  if (item.status) {
    return "display-value " + item.status;
  }
  return "display-value";
}

function getBaileyClass(item) {
  return item.voteTableData?.baileyClass || "";
}

function toggleTranscript() {
  transcriptVisible.value = !transcriptVisible.value;
}

defineExpose({
  setData,
});
</script>

<style>
/* Global (not scoped!) style for the overlay */
.overlay-style {
  max-width: 260px;
  font-size: smaller;
}
</style>

<style scoped>
.display-value {
  white-space: pre-wrap;
  font-family: "Noto Sans", "Noto Sans Symbols", sans-serif;
  font-size: 16px;
}

.baseline-value {
  padding: 8px;
  width: 16px;
  height: 16px;
  vertical-align: top;
}

/* Compare info-selected-item in InfoSelectedItem.vue */
.info-candidate-item {
  margin: 4px 4px 6px 2px;
  padding: 4px 4px 6px 4px;
  border: 2px solid white;
}

.info-candidate-item:hover {
  border: 2px solid #bce8f1;
}

.flag-or-lock-alert {
  background-color: #fcf8e3;
  border-color: #fbeed5;
  color: #c09853;
  padding: 15px;
  border: 1px solid transparent;
  border-radius: 4px;
}

.requirement-style {
  background-color: #fcf8e3;
  border-color: #fbeed5;
  color: #c09853;
  padding: 15px;
  border-radius: 4px;
}
</style>
