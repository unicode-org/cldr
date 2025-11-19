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
        <template
          v-if="
            USE_TAGS &&
            !CAN_EDIT_TAGS &&
            (SHOW_TAGS_AND_TEXT_TOGETHER || inputModeIsTags)
          "
        >
          <component
            :is="AddValueTagsReadOnly"
            :key="componentKeyReadOnly"
            v-model="newValue"
            ref="tagsReadonlyRef"
          />
        </template>
        <template
          v-if="
            CAN_EDIT_TAGS && (SHOW_TAGS_AND_TEXT_TOGETHER || inputModeIsTags)
          "
        >
          <component
            :is="AddValueTagsEdit"
            :key="componentKeyEdit"
            v-model="newValue"
            ref="tagsRef"
            @change="handleTagsChange"
          />
        </template>
        <p v-if="SHOW_TAGS_AND_TEXT_TOGETHER" class="vertical-spacer" />
        <template v-if="SHOW_TAGS_AND_TEXT_TOGETHER || !inputModeIsTags">
          <!-- input mode is Text -->
          <a-input
            v-model:value="newValue"
            placeholder="Add a translation"
            ref="inputToFocus"
            @keydown.enter="onSubmit"
            @change="handleTextChange"
          />
        </template>
      </a-config-provider>
    </p>

    <p v-if="SHOW_RADIO">
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

import AddValueTagsEdit from "./AddValueTagsEdit.vue";
import AddValueTagsReadOnly from "./AddValueTagsReadOnly.vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrConstants from "../esm/cldrConstants.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

/**
 * If true, a tag view is available in addition to the normal text view
 */
const USE_TAGS = true;

/**
 * If true, tags can be edited
 */
const CAN_EDIT_TAGS = USE_TAGS && true;

/**
 * If true, the tag view is shown at the same time as the text view
 */
const SHOW_TAGS_AND_TEXT_TOGETHER = USE_TAGS && true;

/**
 * If true, a pair of radio buttons enables switching between text and tag views. This is
 * necessary if USE_TAGS is true and SHOW_TAGS_AND_TEXT_TOGETHER is false. It is probably
 * not appropriate unless CAN_EDIT_TAGS is true.
 */
const SHOW_RADIO = USE_TAGS && !SHOW_TAGS_AND_TEXT_TOGETHER;

/**
 * textHelp and tagHelp are only displayed if SHOW_RADIO is true
 */
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
const componentKeyEdit = ref(0);
const componentKeyReadOnly = ref(0);
const tagsRef = ref();
const tagsReadonlyRef = ref();

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
  if (SHOW_TAGS_AND_TEXT_TOGETHER) {
    handleTextChange();
  }
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
  // Incrementing the componentKeyEdit forces re-rendering. Otherwise reactive update
  // fails for unknown reasons under some circumstances. For example, when a tag is
  // deleted, if re-rendering is not forced, sometimes two adjacent [+] controls are
  // displayed, which should not be the case. Reference:
  // https://michaelnthiessen.com/force-re-render/#better-way-you-can-use-forceupdate
  componentKeyEdit.value++;
}

function handleTextChange() {
  if (SHOW_TAGS_AND_TEXT_TOGETHER) {
    componentKeyEdit.value++;
    componentKeyReadOnly.value++;
  }
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

.vertical-spacer {
  margin: 1em 0 0 0;
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
