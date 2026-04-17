<template>
  <template v-for="(tag, index) in tagArray" :key="index">
    <a-input
      v-if="visibleInputIndex === index"
      ref="tagInputRef"
      v-model:value="newTagText"
      type="text"
      size="small"
      class="new-tag-input"
      v-focus
      @blur="handleInputConfirm(index)"
      @keyup.enter="handleInputConfirm(index)"
    />
    <a-tag v-else class="new-tag-plus" @click="visibleInputIndex = index">
      +
    </a-tag>
    <template v-if="index + 1 < tagArray.length">
      <a-tag
        :closable="true"
        @close="deleteTag(index)"
        @click="handleClickTag($event, index)"
        class="regular-tag"
      >
        <a-tooltip>
          <template #title> {{ tagTooltip(tag) }} </template>
          {{ displayTag(tag) }}
        </a-tooltip>
      </a-tag>
    </template>
  </template>
  <!-- Clicking a tag brings up a menu to change its value -->
  <a-modal
    v-model:visible="menuIsVisible"
    width="50ch"
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
import * as cldrEscaper from "../esm/cldrEscaper.mjs";

const DEBUG = false;

// The END_TAG is not displayed; it serves only for [+] adding a new tag at the end
const END_TAG = "end";

const props = defineProps({
  modelValue: String,
});

onMounted(mounted);

function mounted() {
  convertTextToTags(props.modelValue);
}

const emit = defineEmits(["change", "update:modelValue"]);

const tagArray = ref([END_TAG]);
const newTagText = ref("");
const visibleInputIndex = ref(-1);
const tagInputRef = ref(null);

// Enable any element with v-focus to become focused automatically. In particular, the
// element with ref=tagInputRef gets the blinking cursor without needing the user to click in it.
const vFocus = {
  mounted: (el) => el.focus(),
};

function deleteTag(index) {
  tagArray.value.splice(index, 1);
  if (DEBUG) {
    console.log("deleteTag: index = " + index + "; tags = " + tagArray.value);
  }
  updateParent();
}

function handleInputConfirm(index) {
  const s = processInput(newTagText.value);
  if (s) {
    tagArray.value.splice(index, 0, s);
    if (DEBUG) {
      console.log(
        "handleInputConfirm index = " + index + "; tags = " + tagArray.value
      );
    }
  }
  visibleInputIndex.value = -1;
  newTagText.value = "";
  updateParent();
}

function updateParent() {
  emit("change");
  emit("update:modelValue", convertTagsToText());
}

/**
 * Whenever the tags change, update the text string value, in case the user
 * switches to Text mode or presses the Submit button. (Alternatively, we
 * could wait for one of those events to occur, however that might need tighter
 * coupling with the parent component.)
 */
function convertTagsToText() {
  // Omit END_TAG, hence "-1"
  const text = tagArray.value.slice(0, -1).join("");
  if (DEBUG) {
    console.log(
      "convertTagsToText: tags = " + tagArray.value + "; text = " + text
    );
  }
  return text;
}

function convertTextToTags(text) {
  // Currently each character becomes a tag.
  // In the future some sequences of characters might be combined into single tags.
  const tags = cldrChar.split(text);
  tags.push(END_TAG);
  tagArray.value = tags;
  if (DEBUG) {
    console.log("convertTextToTags: text = " + text + "; tags = " + tags);
  }
}

function processInput(s) {
  const c = cldrChar.fromUPlus(s);
  if (c) {
    return c;
  }
  return s;
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
  const codePoint = cldrChar.firstCodePoint(tag);
  return cldrChar.uPlus(codePoint) + " " + cldrChar.name(codePoint);
}

const menuIsVisible = ref(false);
const menuLeft = ref(0);
const menuTop = ref(0);
const menuOptions = ref([]);
const charToReplace = ref(undefined);
const chosenChar = ref(undefined);
const chosenIndex = ref(undefined);

function handleClickTag(event, index) {
  const c = cldrChar.firstChar(tagArray.value[index]);
  if (!cldrChar.isSpecial(c)) {
    return;
  }
  chosenIndex.value = index;
  const codePointToReplace = cldrChar.firstCodePoint(c);
  charToReplace.value = menuText(codePointToReplace);
  // Use the coordinates of the tag's top-left corner
  menuLeft.value = event.clientX - event.offsetX;
  menuTop.value = event.clientY - event.offsetY;
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
  menuIsVisible.value = true;
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
  margin: 0 3px 0 0;
}

.new-tag-input {
  width: 78px;
  margin: 0 3px 0 0;
}

.new-tag-plus {
  background: #fff;
  border-style: dashed;
  margin: 0 3px 0 0;
}
</style>
