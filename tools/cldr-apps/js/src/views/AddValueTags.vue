<template>
  <template v-for="(tag, index) in tagArray" :key="index">
    <!-- Clicking a tag brings up a menu to change its value -->
    <!-- https://www.antdv.com/components/select/ -->
    <template v-if="index == chosenIndex">
      <a-select
        autofocus
        defaultOpen
        :showArrow="false"
        :dropdownMatchSelectWidth="false"
        :placeholder="null"
        v-if="menuIsVisible"
        v-model:value="chosenChar"
        class="tag-menu"
        @change="handleChooseCharacter"
        @dropdownVisibleChange="handleDropdownVisibleChange"
      >
        <a-select-option
          v-for="option in menuOptions"
          :key="option.codePoint"
          :value="option.codePoint"
        >
          <a-tooltip
            ><template #title> {{ option.hover }} </template>
            {{ option.label }}
          </a-tooltip>
        </a-select-option>
      </a-select>
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
  <a-select
    autofocus
    defaultOpen
    :showArrow="false"
    :dropdownMatchSelectWidth="false"
    :placeholder="null"
    v-if="insertMenuIsVisible"
    v-model:value="chosenChar"
    class="tag-menu"
    @change="handleChooseCharacter"
    @dropdownVisibleChange="handleDropdownVisibleChange"
  >
    <a-select-option
      v-for="option in menuOptions"
      :key="option.codePoint"
      :value="option.codePoint"
    >
      <a-tooltip
        ><template #title> {{ option.hover }} </template>
        {{ option.label }}
      </a-tooltip>
    </a-select-option>
  </a-select>
</template>

<script setup>
import { onMounted, ref } from "vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrChar from "../esm/cldrChar.mjs";
import * as cldrEscaper from "../esm/cldrEscaper.mjs";

const DEBUG = true;

const props = defineProps({
  modelValue: String,
});

onMounted(mounted);

function mounted() {
  tagArray.value = cldrAddValue.convertTextToTags(props.modelValue);
}

const emit = defineEmits(["change", "update:modelValue"]);

const tagArray = ref([]);

function updateParent() {
  emit("change");
  emit("update:modelValue", cldrAddValue.convertTagsToText(tagArray.value));
}

function makeTagId(index) {
  return "cldr-tag-" + index;
}

function tagIsClickable(tag) {
  const c = cldrChar.firstChar(tag);
  return cldrChar.isSpecial(c);
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

const menuIsVisible = ref(false);
const insertMenuIsVisible = ref(false);
const insertionPoint = ref(0);
const menuLeft = ref(0);
const menuTop = ref(0);
const menuOptions = ref([]);
const chosenChar = ref(undefined);
const chosenIndex = ref(undefined);

function handleClickTag(event, index) {
  const c = cldrChar.firstChar(tagArray.value[index]);
  if (!cldrChar.isSpecial(c)) {
    return;
  }
  chosenIndex.value = index;
  // Use the coordinates of the tag's top-left corner
  menuLeft.value = event.clientX - event.offsetX;
  menuTop.value = event.clientY - event.offsetY;
  populateCharMenu();
  menuIsVisible.value = true;
  if (DEBUG) {
    console.log("handleClickTag: menuIsVisible.value = true");
  }
}

function populateCharMenu() {
  if (menuOptions.value.length == 0) {
    const map = cldrEscaper.getAllNames();
    for (let key of Object.keys(map).sort()) {
      const info = map[key];
      if (info) {
        // info.name, info.shortName, info.description
        const codePoint = cldrChar.firstCodePoint(key);
        const label = makeLabel(codePoint, info);
        const hover = makeHover(codePoint, info);
        const item = {
          codePoint: codePoint,
          label: label,
          hover: hover,
        };
        menuOptions.value.push(item);
      }
    }
  }
}

function makeLabel(codePoint, info) {
  const name = info?.name || info?.shortName;
  const usv = cldrChar.uPlus(codePoint);
  return name + " (" + info.shortName + " " + usv + ")";
}

function makeHover(codePoint, info) {
  const name = info?.name || info?.shortName;
  const usv = cldrChar.uPlus(codePoint);
  return (
    name + " (" + info.shortName + " " + usv + ": " + info.description + ")"
  );
}

function handleChooseCharacter(codePoint) {
  const c = String.fromCodePoint(codePoint);
  if (insertMenuIsVisible.value) {
    // Insert new character and tag
    if (DEBUG) {
      console.log(
        "handleChooseCharacter: Inserting new char; insertionPoint.value = " +
          insertionPoint.value
      );
    }
    const textBefore = cldrAddValue.convertTagsToText(tagArray.value);
    const textAfter =
      textBefore.slice(0, insertionPoint.value) +
      c +
      textBefore.slice(insertionPoint.value);
    tagArray.value = cldrAddValue.convertTextToTags(textAfter);
    if (DEBUG) {
      console.log(
        "cancelInsertMenu: after slice, tagArray = " + tagArray.value
      );
    }
  } else {
    // Change the character for the existing tag
    if (DEBUG) {
      console.log(
        "handleChooseCharacter: Replacing tag at chosenIndex; chosenIndex.value = " +
          chosenIndex +
          "; before replacement, tagArray = " +
          tagArray.value.value
      );
      tagArray.value[chosenIndex.value] = c;
    }
  }
  menuIsVisible.value = insertMenuIsVisible.value = false;
  if (DEBUG) {
    console.log("handleChooseCharacter: menuIsVisible.value = false");
  }
  updateParent();
  chosenChar.value = undefined; // ready for next time
}

function handleDropdownVisibleChange(isVisible) {
  menuIsVisible.value = isVisible;
  if (DEBUG) {
    console.log(
      "handleDropdownVisibleChange: menuIsVisible.value = " +
        menuIsVisible.value +
        "; isVisible = " +
        isVisible
    );
  }
}

function toggleInsertMenuVisibility(event, selStart) {
  if (DEBUG) {
    console.log(
      "toggleInsertMenuVisibility: selStart = " +
        selStart +
        "; before toggle, menuIsVisible.value = " +
        menuIsVisible.value +
        "; insertMenuIsVisible.value = " +
        insertMenuIsVisible.value
    );
  }
  insertMenuIsVisible.value = !insertMenuIsVisible.value;
  menuIsVisible.value = insertMenuIsVisible.value;
  if (DEBUG) {
    console.log(
      "toggleInsertMenuVisibility: menuIsVisible.value = " + menuIsVisible.value
    );
  }
  insertionPoint.value = selStart;
  if (menuIsVisible.value) {
    openInsertMenu(event);
  } else {
    cancelInsertMenu();
  }
}

function openInsertMenu(event) {
  // Use the coordinates of the insert button's top-left corner
  menuLeft.value = event.clientX - event.offsetX;
  menuTop.value = event.clientY - event.offsetY;
  populateCharMenu();
  menuIsVisible.value = true;
  if (DEBUG) {
    console.log("openInsertMenu: menuIsVisible.value = true");
  }
}

function cancelInsertMenu() {
  updateParent(); // TODO: maybe not needed??
}

defineExpose({
  toggleInsertMenuVisibility,
});
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
