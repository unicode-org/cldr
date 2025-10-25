<template>
  <!-- If use a-button instead of button, form positioning fails -->
  <button class="plus" type="button" @click="showModal">
    ✚
    <!-- U+271A HEAVY GREEK CROSS -->
  </button>
  <a-modal
    v-model:visible="formIsVisible"
    :closable="false"
    :footer="null"
    class="dialog"
    :style="{
      position: 'sticky',
      left: formLeft + 'px',
      top: formTop + 'px',
    }"
    @ok="onSubmit"
  >
    <p>
      <a-config-provider :direction="dir">
        <template v-if="inputModeIsTags">
          <component
            :is="AddValueTags"
            :key="componentKey"
            v-model="newValue"
            ref="tagsRef"
            @change="handleTagsChange"
          />
        </template>
        <template v-else>
          <!-- input mode is Text -->
          <a-input
            v-model:value="newValue"
            placeholder="Add a translation"
            ref="inputToFocus"
            @keydown.enter="onSubmit"
          />
        </template>
      </a-config-provider>
    </p>

    <p>
      <label for="radio_mode">Input mode:&nbsp;&nbsp;</label>
      <a-radio-group id="radio_mode" v-model:value="inputModeIsTags">
        <a-tooltip placement="bottom">
          <template #title>{{ textHelp }}</template>
          <a-radio :value="false">Text</a-radio>
        </a-tooltip>
        <a-tooltip placement="bottom">
          <template #title>{{ tagHelp }}</template>
          <a-radio :value="true">Tags</a-radio>
        </a-tooltip>
      </a-radio-group>
    </p>

    <div class="button-container">
      <a-button @click="onEnglish">→English</a-button>
      <a-button @click="onWinning">→Winning</a-button>
      <button
        class="plus"
        type="button"
        @click="voteForMissing"
        v-if="showVoteForMissing"
      >
        {{ cldrConstants.VOTE_FOR_MISSING }} - vote for missing
      </button>
      <a-button type="cancel" @click="onCancel">Cancel</a-button>
      <a-button type="primary" @click="onSubmit">Submit</a-button>
    </div>
  </a-modal>
</template>

<script setup>
import { nextTick, ref } from "vue";

import AddValueTags from "./AddValueTags.vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrConstants from "../esm/cldrConstants.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

const textHelp =
  "Show the value as a text string, which can be edited using the ordinary editing features of the web browser";

const tagHelp =
  "Show the value as a sequence of tags, each tag representing a character, with special features for viewing/editing tags";

const xpstrid = ref(""); // xpath string id
const newValue = ref("");
const formLeft = ref(0);
const formTop = ref(0);
const formIsVisible = ref(false);
const inputToFocus = ref(null);
const inputModeIsTags = ref(false);
const componentKey = ref(0);
const tagsRef = ref();

const showVoteForMissing = ref(
  cldrStatus.getPermissions()?.userCanVoteForMissing
);

const { dir } = defineProps(["dir"]);

function setXpathStringId(id) {
  xpstrid.value = id;
}

function showModal(event) {
  // Use the coordinates of the button's top-left corner
  formLeft.value = event.clientX - event.offsetX;
  formTop.value = event.clientY - event.offsetY;
  setValue("");
  formIsVisible.value = true;
  cldrAddValue.setFormIsVisible(true, xpstrid.value);
  nextTick(focusInput);
}

function setValue(s) {
  newValue.value = s;
}

function focusInput() {
  if (inputToFocus.value) {
    inputToFocus.value.focus();
  }
}

function onEnglish() {
  setValue(cldrAddValue.getEnglish(xpstrid.value));
}

function onWinning() {
  setValue(cldrAddValue.getWinning(xpstrid.value));
}

function onCancel() {
  formIsVisible.value = false;
  cldrAddValue.setFormIsVisible(false, xpstrid.value);
}

function onSubmit() {
  formIsVisible.value = false;
  cldrAddValue.setFormIsVisible(false, xpstrid.value);
  if (newValue.value) {
    cldrAddValue.sendRequest(xpstrid.value, newValue.value);
  }
}

function voteForMissing() {
  setValue(cldrConstants.VOTE_FOR_MISSING);
}

function handleTagsChange() {
  // Incrementing the componentKey forces re-rendering. Otherwise reactive update
  // fails for unknown reasons under some circumstances. For example, when a tag is
  // deleted, if re-rendering is not forced, sometimes two adjacent [+] controls are
  // displayed, which should not be the case. Reference:
  // https://michaelnthiessen.com/force-re-render/#better-way-you-can-use-forceupdate
  componentKey.value++;
}

defineExpose({
  setXpathStringId,
});
</script>

<style scoped>
.button-container {
  display: flex;
  justify-content: space-between;
  padding-top: 1em;
}

.plus {
  font-size: 118%;
  border-radius: 4px;
  padding: 6px 12px;
  color: #fff;
  background-color: #428bca;
  border: 1px solid #345578;
}
</style>
