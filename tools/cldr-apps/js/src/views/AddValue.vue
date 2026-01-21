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
          v-if="useTags && !canEditTags && (inputModeIsTags || !showRadio)"
        >
          <!-- show as basic tags -->
          <component
            :is="AddValueTagsBasic"
            :key="componentKeyBasic"
            v-model="newValue"
            ref="tagsBasicRef"
          />
        </template>
        <template v-if="canEditTags && (inputModeIsTags || !showRadio)">
          <!-- show as editable tags -->
          <component
            :is="AddValueTagsEdit"
            :key="componentKeyEdit"
            v-model="newValue"
            ref="tagsEditRef"
            @change="handleTagsChange"
          />
        </template>
        <p v-if="!showRadio" class="vertical-spacer" />
        <template v-if="!(showRadio && inputModeIsTags)">
          <!-- show as text -->
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

    <p v-if="showRadio">
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
      <a-tooltip placement="bottom">
        <template #title>{{ "Input a copy of the English value" }}</template>
        <a-button @click="onEnglish">→English</a-button>
      </a-tooltip>
      &nbsp;
      <a-tooltip placement="bottom">
        <template #title>{{ "Input a copy of the winning value" }}</template>
        <a-button @click="onWinning">→Winning</a-button>
      </a-tooltip>
      &nbsp;
      <a-tooltip placement="bottom">
        <template #title>{{ "Vote for missing" }}</template>
        <a-button @click="voteForMissing" v-if="showVoteForMissing">
          {{ cldrConstants.VOTE_FOR_MISSING }}
        </a-button>
      </a-tooltip>
      &nbsp;
      <a-button type="cancel" @click="onCancel">Cancel</a-button>
      &nbsp;
      <a-button type="primary" @click="onSubmit">Submit</a-button>
    </div>
    <!-- Checkboxes are displayed only if user Shift-clicks on Cancel, to enable experimental tag features for testing/debugging -->
    <a-modal
      v-model:visible="formHasTagOptions"
      width="20ch"
      :closable="false"
      :footer="null"
    >
      <a-checkbox v-model:checked="useTags" @change="handleCheckboxChange"
        >use tags</a-checkbox
      ><br />
      <a-checkbox
        v-model:checked="canEditTags"
        @change="handleCheckboxChange"
        :disabled="!useTags"
        >can edit tags</a-checkbox
      ><br />
      <a-checkbox
        v-model:checked="showRadio"
        @change="handleCheckboxChange"
        :disabled="!useTags"
        >show radio</a-checkbox
      >
    </a-modal>
  </a-modal>
</template>

<script setup>
import { nextTick, ref } from "vue";

// Two kinds of "tags" are supported: basic (read-only) and editable
import AddValueTagsBasic from "./AddValueTagsBasic.vue";
import AddValueTagsEdit from "./AddValueTagsEdit.vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrConstants from "../esm/cldrConstants.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

const DEBUG = true;

/**
 * If true, a tag view is available in addition to the normal text view
 */
const useTags = ref(false);

/**
 * If true, tags can be edited
 */
const canEditTags = ref(false);

/**
 * If true, a pair of radio buttons enables switching between text and tag views; otherwise, if using tags,
 * tags and text are shown at the same time.
 */
const showRadio = ref(false);

/**
 * textHelp and tagHelp are only displayed if showRadio is true
 */
const textHelp =
  "Show the value as a text string, which can be edited using the ordinary editing features of the web browser";

const tagHelp =
  "Show the value as a sequence of tags, each tag representing a character";

const xpstrid = ref(""); // xpath string id
const newValue = ref("");
const formLeft = ref(0);
const formTop = ref(0);
const formIsVisible = ref(false);
const inputToFocus = ref(null);
const inputModeIsTags = ref(false);
const formHasTagOptions = ref(false);
const componentKeyEdit = ref(0);
const componentKeyBasic = ref(0);
const tagsEditRef = ref();
const tagsBasicRef = ref();

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
  if (!showRadio.value) {
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

function onCancel(event) {
  if (DEBUG && event.shiftKey) {
    formHasTagOptions.value = true;
    return;
  }
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
  if (!showRadio.value) {
    componentKeyEdit.value++;
    componentKeyBasic.value++;
  }
}

function handleCheckboxChange() {
  if (!useTags.value) {
    canEditTags.value = showRadio.value = false;
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
