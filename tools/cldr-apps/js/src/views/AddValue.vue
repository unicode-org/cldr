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
    <div>
      <a-config-provider :direction="dir">
        <span class="show-as-text">
          <a-input
            v-model:value="newValue"
            placeholder="Add a translation"
            ref="inputToFocus"
            @keydown.enter="onSubmit"
            @change="handleTextChange"
          />
          <span v-if="tagMode === TAG_MODE_MENUS" class="show-as-text">
            &nbsp;
            <a-tooltip placement="bottom">
              <template #title>{{
                "Insert a special character at the insertion point"
              }}</template>
              <a-button @click="toggleInsertMenu">I</a-button>
            </a-tooltip>
          </span>
        </span>
        <span class="tag-area">
          <a-checkbox
            v-model:checked="useTags"
            @change="handleTagsCheckboxChange"
            >tags</a-checkbox
          >
          <template v-if="useTags">
            <template v-if="tagMode === TAG_MODE_MENUS">
              <component
                :is="AddValueTags"
                :key="componentKeyMenus"
                v-model="newValue"
                @change="handleTagsChange"
                ref="addValueTagsRef"
              />
            </template>
            <template v-else-if="tagMode === TAG_MODE_EDIT">
              <component
                :is="AddValueTagsEdit"
                :key="componentKeyEdit"
                v-model="newValue"
                @change="handleTagsChange"
              />
            </template>
            <template v-else-if="tagMode === TAG_MODE_BASIC">
              <component
                :is="AddValueTagsBasic"
                :key="componentKeyBasic"
                v-model="newValue"
              />
            </template>
          </template>
        </span>
      </a-config-provider>
    </div>

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
    <!-- This dialog is displayed only if user Shift-clicks on Cancel, to enable experimental tag features for testing/debugging -->
    <a-modal
      v-model:visible="formHasTagOptions"
      width="30ch"
      :closable="true"
      :footer="null"
    >
      <a-radio-group v-model:value="tagMode">
        <a-radio :value="TAG_MODE_NONE"> no tags </a-radio><br />
        <a-radio :value="TAG_MODE_BASIC"> basic tags </a-radio><br />
        <a-radio :value="TAG_MODE_MENUS"> tags with menus </a-radio><br />
        <a-radio :value="TAG_MODE_EDIT"> tags with editing </a-radio><br />
      </a-radio-group>
    </a-modal>
  </a-modal>
</template>

<script setup>
import { nextTick, ref } from "vue";

// Three kinds of "tags" are supported: basic (read-only), editable, and menu-editable (menu for special characters)
import AddValueTagsBasic from "./AddValueTagsBasic.vue";
import AddValueTagsEdit from "./AddValueTagsEdit.vue";
import AddValueTags from "./AddValueTags.vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrChar from "../esm/cldrChar.mjs";
import * as cldrConstants from "../esm/cldrConstants.mjs";
import * as cldrLoad from "../esm/cldrLoad.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

const DEBUG = false;

/**
 * If TEST_ALTERNATIVE_MODES is true and the user Shift-clicks on the Cancel button, a dialog
 * appears with radio buttons to choose a different tag mode. This is for development testing,
 * with the expectation that alternative modes may be appropriate for special kinds of path,
 * such as exemplars and annotations.
 */
const TEST_ALTERNATIVE_MODES = true;

const TAG_MODE_NONE = 0; // tags not displayed
const TAG_MODE_BASIC = 1; // basic tags
const TAG_MODE_MENUS = 2; // tags have menus to switch characters
const TAG_MODE_EDIT = 3; // tags can be edited

const tagMode = ref(TAG_MODE_NONE);

/**
 * If true, a tag view is available in addition to the normal text view
 */
const useTags = ref(false);

const xpstrid = ref(""); // xpath string id
const newValue = ref("");
const formLeft = ref(0);
const formTop = ref(0);
const formIsVisible = ref(false);
const inputToFocus = ref(null);
const formHasTagOptions = ref(false);
const componentKeyMenus = ref(0);
const componentKeyEdit = ref(0);
const componentKeyBasic = ref(0);

const addValueTagsRef = ref();

const showVoteForMissing = ref(
  cldrStatus.getPermissions()?.userCanVoteForMissing
);

const { dir } = defineProps(["dir"]);

function setXpathStringId(id) {
  xpstrid.value = id;
}

function showModal(event) {
  if (cldrStatus.getCurrentId() !== xpstrid.value) {
    cldrLoad.updateCurrentId(xpstrid.value);
  }

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
  handleTextChange();
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
  if (TEST_ALTERNATIVE_MODES && event.shiftKey) {
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
  componentKeyMenus.value++;
}

function handleTextChange() {
  componentKeyEdit.value++;
  componentKeyBasic.value++;
  componentKeyMenus.value++;
  // Automatically turn on the tags checkbox if there is a special character other than SP " " (U+0020)
  if (!useTags.value) {
    const charArray = cldrChar.split(newValue.value);
    for (let c of charArray) {
      if (c != " " && cldrChar.isSpecial(c)) {
        useTags.value = true;
        handleTagsCheckboxChange();
        break;
      }
    }
  }
}

function handleTagsCheckboxChange() {
  tagMode.value = useTags.value ? TAG_MODE_MENUS : TAG_MODE_NONE;
}

function toggleInsertMenu(event) {
  // Note: inputToFocus.value.input.selectionStart works for getting the offset (in
  // characters) of the insertion point (cursor) in the string the user is editing.
  // This was determined by experimentation; it's not clear whether it is a documented
  // feature of Ant Vue components.
  const insertionPoint = inputToFocus.value.input.selectionStart;
  if (DEBUG) {
    console.log(
      "AddValue.toggleInsertMenu: insertionPoint = " + insertionPoint
    );
  }
  addValueTagsRef.value.toggleInsertMenuVisibility(event, insertionPoint);
}

defineExpose({
  setXpathStringId,
});
</script>

<style scoped>
.button-container {
  display: flex;
  justify-content: space-between;
}

.plus {
  font-size: 118%;
  border-radius: 4px;
  padding: 6px 12px;
  color: #fff;
  background-color: #428bca;
  border: 1px solid #345578;
}

.show-as-text {
  display: flex;
  align-items: stretch;
  flex-wrap: nowrap;
}

.tag-area {
  display: flex;
  align-items: stretch;
  flex-wrap: wrap;
  margin: 1em 0 1em 0;
}
</style>
