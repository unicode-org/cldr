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
          <span class="show-as-text">
            &nbsp;
            <a-tooltip placement="bottomLeft">
              <template #title>{{ "Insert special characters" }}</template>
              <a-button size="small" shape="circle" @click="openInsertMenu"
                >⎀</a-button
              >
            </a-tooltip>
          </span>
          <AddValueCharMenu
            v-if="insertMenuIsVisible"
            :key="componentKeyInsert"
            v-model="chosenChar"
            @change="insertMenuOnChange"
            @isVisible="insertMenuIsVisible"
            ref="addValueCharMenuRef"
          />
        </span>
      </a-config-provider>
      <span class="tag-area" v-show="useTags">
        <a-config-provider :direction="dir">
          <div v-show="tagMode === TAG_MODE_MENUS">
            <component
              :is="AddValueTags"
              :key="componentKeyMenus"
              v-model="newValue"
              @change="handleTagsChange"
              ref="addValueTagsRef"
            />
          </div>
          <template v-if="tagMode === TAG_MODE_EDIT">
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
        </a-config-provider>
      </span>
    </div>

    <div class="button-container">
      <a-tooltip placement="bottom">
        <template #title>{{ "Show hidden" }}</template>
        <span @click="toggleTags">
          <eye-outlined v-if="useTags" class="eyeIcon" />
          <eye-invisible-outlined v-else class="eyeIcon"
        /></span>
      </a-tooltip>
      &nbsp;
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

// https://www.antdv.com/components/icon
import { EyeInvisibleOutlined } from "@ant-design/icons-vue";
import { EyeOutlined } from "@ant-design/icons-vue";

// Three kinds of "tags" are supported: basic (read-only), editable, and menu-editable (menu for special characters)
import AddValueTagsBasic from "./AddValueTagsBasic.vue";
import AddValueTagsEdit from "./AddValueTagsEdit.vue";
import AddValueTags from "./AddValueTags.vue";
import AddValueCharMenu from "./AddValueCharMenu.vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrChar from "../esm/cldrChar.mjs";
import * as cldrConstants from "../esm/cldrConstants.mjs";
import * as cldrLoad from "../esm/cldrLoad.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

const DEBUG = true;

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
const userTurnedOffTags = ref(false);

const xpstrid = ref(""); // xpath string id
const newValue = ref("");
const formLeft = ref(0);
const formTop = ref(0);
const formIsVisible = ref(false);
const insertMenuIsVisible = ref(false);
const inputToFocus = ref(null);
const formHasTagOptions = ref(false);
const componentKeyInsert = ref(0);
const componentKeyMenus = ref(0);
const componentKeyEdit = ref(0);
const componentKeyBasic = ref(0);
const chosenChar = ref(null);

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
  showHiddenIfSpecial();
}

/**
 * Turn on "show hidden" (useTags) if there is a special character other than SP " " (U+0020),
 * but not if the user manually turned if off previously.
 */
function showHiddenIfSpecial() {
  if (!userTurnedOffTags.value && !useTags.value) {
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

function toggleTags() {
  useTags.value = !useTags.value;
  if (!useTags.value) {
    userTurnedOffTags.value = true;
  }
  handleTagsCheckboxChange();
}

function handleTagsCheckboxChange() {
  tagMode.value = useTags.value ? TAG_MODE_MENUS : TAG_MODE_NONE;
}

function openInsertMenu(event) {
  console.log(
    "openInsertMenu: insertMenuIsVisible.value = " +
      insertMenuIsVisible.value +
      "; setting to true"
  );
  insertMenuIsVisible.value = true;
  componentKeyInsert.value++;
}

function insertMenuOnChange() {
  if (!chosenChar.value) {
    if (DEBUG) {
      console.log(
        "AddValue.insertMenuOnChange: doing nothing since chosenChar.value = " +
          chosenChar.value
      );
    }
    return;
  }
  // Note: inputToFocus.value.input.selectionStart works for getting the offset (in
  // characters) of the insertion point (cursor) in the string the user is editing.
  // This was determined by experimentation; it's not clear whether it is a documented
  // feature of Ant Vue components.
  const insertionPoint = inputToFocus.value.input.selectionStart;
  const textBefore = newValue.value;
  const textAfter =
    textBefore.slice(0, insertionPoint) +
    chosenChar.value +
    textBefore.slice(insertionPoint);
  setValue(textAfter);
  if (DEBUG) {
    console.log(
      "AddValue.insertMenuOnChange: insertionPoint = " +
        insertionPoint +
        "; insertMenuIsVisible = " +
        insertMenuIsVisible.value +
        ": chosenChar.value = " +
        chosenChar.value
    );
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
  margin-top: 1em;
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
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  padding: 4px 11px;
}

.eyeIcon {
  color: black;
  font-size: larger;
}
</style>
