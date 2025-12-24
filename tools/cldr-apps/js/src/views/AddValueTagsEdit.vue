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
      <a-tag :closable="true" @close="deleteTag(index)" class="regular-tag">
        <a-tooltip>
          <template #title> {{ tagTooltip(tag) }} </template>
          {{ displayTag(tag) }}
        </a-tooltip>
      </a-tag>
    </template>
  </template>
</template>

<script setup>
import { onMounted, ref } from "vue";

import * as cldrChar from "../esm/cldrChar.mjs";

const DEBUG = true;

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
  const codePoint = cldrChar.firstCodePoint(tag);
  if (cldrChar.isWhiteSpace(codePoint)) {
    return cldrChar.name(codePoint);
  } else {
    return tag;
  }
}

function tagTooltip(tag) {
  const codePoint = cldrChar.firstCodePoint(tag);
  return cldrChar.uPlus(codePoint) + " " + cldrChar.name(codePoint);
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
