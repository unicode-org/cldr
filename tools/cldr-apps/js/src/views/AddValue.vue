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
        <!-- show as text -->
        <a-input
          v-model:value="newValue"
          placeholder="Add a translation"
          ref="inputToFocus"
          @keydown.enter="onSubmit"
          @change="handleTextChange"
        />
        <p class="vertical-spacer" />
        <a-checkbox v-model:checked="useTags" @change="handleTagsCheckboxChange"
          >tags</a-checkbox
        >
        <template v-if="useTags && !(canEditTags || tagsHaveMenus)">
          <!-- show as basic tags -->
          <component
            :is="AddValueTagsBasic"
            :key="componentKeyBasic"
            v-model="newValue"
          />
        </template>
        <template v-if="canEditTags">
          <!-- show as editable tags -->
          <component
            :is="AddValueTagsEdit"
            :key="componentKeyEdit"
            v-model="newValue"
            @change="handleTagsChange"
          />
        </template>
        <template v-if="tagsHaveMenus">
          <!-- show as tags with menus -->
          <component
            :is="AddValueTags"
            :key="componentKeyMenus"
            v-model="newValue"
            @change="handleTagsChange"
          />
        </template>
      </a-config-provider>
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
      width="30ch"
      :closable="true"
      :footer="null"
    >
      <a-checkbox v-model:checked="useTags" @change="handleCheckboxChange"
        >use tags</a-checkbox
      ><br />
      <a-checkbox
        v-model:checked="tagsHaveMenus"
        @change="handleCheckboxChange"
        :disabled="!useTags"
        >tags have menus</a-checkbox
      ><br />
      <a-checkbox
        v-model:checked="canEditTags"
        @change="handleCheckboxChange"
        :disabled="!useTags"
        >can edit tags</a-checkbox
      ><br />
    </a-modal>
  </a-modal>
</template>

<script setup>
import { nextTick, ref } from "vue";

// Three kinds of "tags" are supported: basic (read-only), editable, and semi-editable (menu for whitespace)
import AddValueTagsBasic from "./AddValueTagsBasic.vue";
import AddValueTagsEdit from "./AddValueTagsEdit.vue";
import AddValueTags from "./AddValueTags.vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrChar from "../esm/cldrChar.mjs";
import * as cldrConstants from "../esm/cldrConstants.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

const DEBUG = false;

/**
 * If true, a tag view is available in addition to the normal text view
 */
const useTags = ref(false);

/**
 * If true, some tags (whitespace) can be clicked on to obtain a menu for switching characters
 * (such as between ordinary space and thin space)
 */
const tagsHaveMenus = ref(false);

/**
 * If true, tags can be edited
 */
const canEditTags = ref(false);

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
  componentKeyMenus.value++;
}

function handleTextChange() {
  componentKeyEdit.value++;
  componentKeyBasic.value++;
  componentKeyMenus.value++;
  // Automatically turn on the tags checkbox if there is a whitespace character other than " " (U+0020)
  if (!useTags.value) {
    const charArray = cldrChar.split(newValue.value);
    for (let c of charArray) {
      if (c != " " && cldrChar.isWhiteSpace(c)) {
        useTags.value = true;
        handleTagsCheckboxChange();
        break;
      }
    }
  }
}

function handleTagsCheckboxChange() {
  if (useTags.value) {
    tagsHaveMenus.value = true;
    canEditTags.value = false;
  } else {
    tagsHaveMenus.value = canEditTags.value = false;
  }
}

function handleCheckboxChange() {
  if (!useTags.value) {
    tagsHaveMenus.value = canEditTags.value = false;
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
