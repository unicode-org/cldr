<template>
  <template v-for="(tag, index) in tagArray" :key="index">
    <!-- Clicking a tag brings up a menu to change its value -->
    <template v-if="index == chosenIndex">
      <AddValueCharMenu
        v-if="menuIsVisible"
        :key="componentKeyInsert"
        v-model="chosenChar"
        @change="handleChooseCharacter"
        @isVisible="menuIsVisible"
        ref="addValueCharMenuRef"
      />
    </template>
    <template v-if="tagIsClickable(tag)">
      <a-tag
        @click="handleClickTag($event, index)"
        class="regular-tag"
        :id="makeTagId(index)"
      >
        <a-tooltip>
          <template #title> {{ tagTooltip(tag) }} </template>
          {{ displayTag(tag) }}
        </a-tooltip>
      </a-tag>
    </template>
    <template v-else> {{ tag }} </template>
  </template>
</template>

<script setup>
import { onMounted, ref } from "vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrChar from "../esm/cldrChar.mjs";
import * as cldrEscaper from "../esm/cldrEscaper.mjs";

import AddValueCharMenu from "./AddValueCharMenu.vue";

const DEBUG = true;

const SPACES_HAVE_TAGS = false;

const props = defineProps({
  modelValue: String,
});

const emit = defineEmits(["change", "update:modelValue"]);

onMounted(mounted);

function mounted() {
  tagArray.value = cldrAddValue.convertTextToTags(props.modelValue);
}

const tagArray = ref([]);
const menuIsVisible = ref(false);
const chosenChar = ref(null);
const chosenIndex = ref(undefined);
const componentKeyInsert = ref(0);

function updateParent() {
  emit("change");
  emit("update:modelValue", cldrAddValue.convertTagsToText(tagArray.value));
}

function makeTagId(index) {
  return "cldr-tag-" + index;
}

function tagIsClickable(tag) {
  const c = cldrChar.firstChar(tag);
  if (SPACES_HAVE_TAGS) {
    return cldrChar.isSpecial(c);
  } else {
    return c !== " " && cldrChar.isSpecial(c);
  }
}

function displayTag(tag) {
  const c = cldrChar.firstChar(tag);
  const shortName = cldrEscaper.getShortName(c);
  if (shortName) {
    return shortName;
  } else {
    return tag;
  }
}

function tagTooltip(tag) {
  const c = cldrChar.firstChar(tag);
  const codePoint = cldrChar.firstCodePoint(tag);
  return (
    cldrEscaper.getShortName(c) +
    " (" +
    cldrChar.uPlus(codePoint) +
    "): click to choose alternative"
  );
}

function handleClickTag(event, index) {
  const c = cldrChar.firstChar(tagArray.value[index]);
  if (!cldrChar.isSpecial(c)) {
    return;
  }
  chosenChar.value = c;
  chosenIndex.value = index;
  menuIsVisible.value = true;
  if (DEBUG) {
    console.log("handleClickTag: menuIsVisible.value = true");
  }
}

function handleChooseCharacter() {
  // Change the character for the existing tag
  tagArray.value[chosenIndex.value] = chosenChar.value;
  menuIsVisible.value = false;
  if (DEBUG) {
    console.log(
      "AddValueTags.handleChooseCharacter: Replacing tag at chosenIndex; chosenIndex.value = " +
        chosenIndex
    );
  }
  componentKeyInsert.value++;
  updateParent();
}
</script>

<style>
/* The div with "ant-select-selector" normally contains the menu arrow and placeholder.
   Even if showArrow=false and placeholder=null, an empty div is displayed as an
   obnoxious little rectangle overlapping the tag to which the menu is attached,
   unless this style is overridden, and it can't be "style scoped". (Note that
  "display: none" would hide the entire menu.) */
.ant-select-selector {
  width: 0 !important;
  height: 0 !important;
  border: 0 !important;
}
</style>

<style scoped>
.regular-tag {
  margin: 0 3px 0 3px;
}

/* Prevent the text to the right of the tag moving when the menu is opened */
.tag-menu {
  width: 0 !important;
}
</style>
