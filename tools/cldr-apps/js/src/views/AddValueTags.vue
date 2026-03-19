<template>
  <template v-for="(tag, index) in tagArray" :key="index">
    <template v-if="tagIsClickable(tag)">
      <a-tag @click="handleClickTag($event, index)" class="regular-tag">
        <a-tooltip>
          <template #title> {{ tagTooltip(tag) }} </template>
          {{ displayTag(tag) }}
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
    {{ menuTitle }}<br />
    <!-- https://www.antdv.com/components/select/ -->
    <a-select
      v-model:value="chosenChar"
      style="width: 40ch"
      @change="handleChooseCharacter"
      placeholder="Select a character"
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
  </a-modal>
</template>

<script setup>
import { onMounted, ref } from "vue";

import * as cldrChar from "../esm/cldrChar.mjs";
import * as cldrEscaper from "../esm/cldrEscaper.mjs";

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
  // Each special character becomes a tag. Each sequence of other characters is combined into a tag.
  // Example: "abc xyz" becomes three tags: ["abc", " ", "xyz"].
  const tags = [];
  let combined = "";
  const charArray = cldrChar.split(text);
  for (let c of charArray) {
    if (cldrChar.isSpecial(c)) {
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
const menuTitle = ref("");
const charToReplace = ref(undefined);
const chosenChar = ref(undefined);
const chosenIndex = ref(undefined);

function handleClickTag(event, index) {
  const c = cldrChar.firstChar(tagArray.value[index]);
  if (!cldrChar.isSpecial(c)) {
    return;
  }
  const codePointToReplace = cldrChar.firstCodePoint(c);
  chosenIndex.value = index;
  charToReplace.value = describeCharToReplace(codePointToReplace);
  menuTitle.value = "Character to replace: " + charToReplace.value;
  // Use the coordinates of the tag's top-left corner
  menuLeft.value = event.clientX - event.offsetX;
  menuTop.value = event.clientY - event.offsetY;
  populateCharMenu();
  menuIsVisible.value = true;
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

function describeCharToReplace(codePoint) {
  return cldrChar.uPlus(codePoint) + " " + cldrChar.name(codePoint);
}

function handleChooseCharacter(codePoint) {
  const c = String.fromCodePoint(codePoint);
  if (insertMenuIsVisible.value) {
    if (DEBUG) {
      console.log(
        "handleChooseCharacter: c = " +
          c +
          "; Inserting new tag at insertion point; insertionPoint.value = " +
          insertionPoint.value +
          "; before splice, tagArray = " +
          tagArray.value
      );
    }
    const textBefore = convertTagsToText(tagArray.value);
    const textAfter =
      textBefore.slice(0, insertionPoint.value) +
      c +
      textBefore.slice(insertionPoint.value);
    convertTextToTags(textAfter);
    if (DEBUG) {
      console.log(
        "handleChooseCharacter: after slice, tagArray = " + tagArray.value
      );
    }
  } else {
    if (DEBUG) {
      console.log(
        "handleChooseCharacter: Replacing tag at chosenIndex; chosenIndex.value = " +
          chosenIndex +
          "; before replacement, tagArray = " +
          tagArray.value.value
      );
    }
    tagArray.value[chosenIndex.value] = c;
  }
  menuIsVisible.value = insertMenuIsVisible.value = false;
  chosenChar.value = undefined; // ready for next time
  updateParent();
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
  insertionPoint.value = selStart;
  if (menuIsVisible.value) {
    menuTitle.value = "Character will be inserted at the cursor";
    // Use the coordinates of the button's top-left corner
    menuLeft.value = event.clientX - event.offsetX;
    menuTop.value = event.clientY - event.offsetY;
    populateCharMenu();
  }
}

defineExpose({
  toggleInsertMenuVisibility,
});
</script>

<style scoped>
.regular-tag {
  margin: 0 3px 0 3px;
}
</style>
