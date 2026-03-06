<template>
  <template v-for="(tag, index) in tagArray" :key="index">
    <template v-if="tagIsClickable(tag)">
      <a-tag @click="handleClickTag($event, index)" class="regular-tag">
        <a-tooltip>
          <template #title> {{ tagTooltip(tag) }} </template>
          {{ displayTag(tag, index) }}
        </a-tooltip>
      </a-tag>
    </template>
    <template v-else> {{ tag }} </template>
  </template>
  <!-- Clicking a tag brings up a menu to change its value -->
  <a-modal
    v-model:visible="menuIsVisible"
    width="50ch"
    autofocus
    :closable="true"
    :footer="null"
    :style="{
      position: 'sticky',
      left: menuLeft + 'px',
      top: menuTop + 'px',
    }"
  >
    Current value: {{ charToReplace }}<br />
    <!-- https://www.antdv.com/components/select/ -->
    <a-select
      v-model:value="chosenChar"
      style="width: 40ch"
      :options="menuOptions"
      @change="handleChooseCharacter"
      placeholder="Select a replacement character"
    >
    </a-select>
  </a-modal>
</template>

<script setup>
import { onMounted, ref } from "vue";

import * as cldrChar from "../esm/cldrChar.mjs";

const DEBUG = false;

const props = defineProps({
  modelValue: String,
});

onMounted(mounted);

function mounted() {
  convertTextToTags(props.modelValue);
}

const emit = defineEmits(["change", "update:modelValue"]);

const tagArray = ref([]);

function updateParent() {
  emit("change");
  emit("update:modelValue", convertTagsToText());
}

/**
 * Update the text string to agree with the tags
 */
function convertTagsToText() {
  const text = tagArray.value.join("");
  if (DEBUG) {
    console.log(
      "AddValueTags.convertTagsToText: tags = " +
        tagArray.value +
        "; text = " +
        text
    );
  }
  return text;
}

/**
 * Update the tags to agree with the text string
 */
function convertTextToTags(text) {
  if (DEBUG) {
    console.log("AddValueTags.convertTextToTags: Hello!");
  }
  // Each whitespace character becomes a tag. Each sequence of other characters is combined into a tag.
  // Example: "abc xyz" becomes three tags: ["abc", " ", "xyz"].
  const tags = [];
  let combined = "";
  const charArray = cldrChar.split(text);
  for (let c of charArray) {
    if (cldrChar.isWhiteSpace(c)) {
      if (combined.length != 0) {
        tags.push(combined);
        combined = "";
      }
      tags.push(c);
    } else {
      combined += c;
    }
  }
  if (combined.length != 0) {
    tags.push(combined);
  }
  tagArray.value = tags;
  if (DEBUG) {
    console.log(
      "AddValueTags.convertTextToTags: text = " +
        text +
        "; tags = " +
        tags +
        "; tagArray.value = " +
        tagArray.value
    );
  }
}

function tagIsClickable(tag) {
  const c = cldrChar.firstChar(tag);
  return cldrChar.isWhiteSpace(c);
}

function displayTag(tag, index) {
  const c = cldrChar.firstChar(tag);
  const codePoint = cldrChar.firstCodePoint(tag);
  if (cldrChar.isWhiteSpace(c)) {
    if (DEBUG) {
      console.log(
        "AddValueTags.displayTag (index = " +
          index +
          "): whitespace " +
          cldrChar.name(codePoint)
      );
    }
    return cldrChar.name(codePoint);
  } else {
    console.log(
      "AddValueTags.displayTag (index = " + index + "): NOT whitespace " + tag
    );
    return tag;
  }
}

function tagTooltip(tag) {
  const codePoint = cldrChar.firstCodePoint(tag);
  return (
    cldrChar.name(codePoint) +
    " (" +
    cldrChar.uPlus(codePoint) +
    "): click to choose alternative"
  );
}

const menuIsVisible = ref(false);
const menuLeft = ref(0);
const menuTop = ref(0);
const menuOptions = ref([]);
const charToReplace = ref(undefined);
const chosenChar = ref(undefined);
const chosenIndex = ref(undefined);

/**
 * These whitespace characters, and no others (aside from tab/newline), currently (2026-01) occur
 * in common/main/*.xml and/or common/annotations/*.xml. Offer a choice between them.
 */
const whiteSpaceChars = [
  0x0020 /* U+0020 SPACE */, 0x00a0 /* U+00A0 NO-BREAK SPACE */,
  0x2009 /* U+2009 THIN SPACE */, 0x202f /* U+202F NARROW NO-BREAK SPACE */,
];

function handleClickTag(event, index) {
  const c = cldrChar.firstChar(tagArray.value[index]);
  if (!cldrChar.isWhiteSpace(c)) {
    return;
  }
  const codePointToReplace = cldrChar.firstCodePoint(c);
  chosenIndex.value = index;
  charToReplace.value = menuText(codePointToReplace);
  // Use the coordinates of the tag's top-left corner
  menuLeft.value = event.clientX - event.offsetX;
  menuTop.value = event.clientY - event.offsetY;
  if (menuOptions.value.length == 0) {
    for (let codePoint of whiteSpaceChars) {
      if (codePointToReplace != codePoint) {
        const item = {
          value: codePoint,
          label: menuText(codePoint),
        };
        menuOptions.value.push(item);
      }
    }
  }
  menuIsVisible.value = true;
}

function menuText(codePoint) {
  return cldrChar.uPlus(codePoint) + " " + cldrChar.name(codePoint);
}

function handleChooseCharacter(codePoint) {
  tagArray.value[chosenIndex.value] = String.fromCodePoint(codePoint);
  menuIsVisible.value = false;
  chosenChar.value = undefined; // ready for next time
  updateParent();
}
</script>

<style scoped>
.regular-tag {
  margin: 0 3px 0 3px;
}
</style>
