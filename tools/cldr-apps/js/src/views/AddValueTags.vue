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
      <a-tag :closable="true" @close="deleteTag(index)">
        <a-tooltip>
          <template #title> {{ tagTooltip(tag) }} </template>
          {{ tag }}
        </a-tooltip>
      </a-tag>
    </template>
  </template>
</template>

<script setup>
import { defineEmits, onMounted, ref } from "vue";
import { unicodeName } from "unicode-name";

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
  // To support codepoints greater than U+FFFF, use text.split(/(?:)/u), not split("")
  const tags = text.split(/(?:)/u);
  tags.push(END_TAG);
  tagArray.value = tags;
  if (DEBUG) {
    console.log("convertTextToTags: text = " + text + "; tags = " + tags);
  }
}

function processInput(s) {
  if (s?.startsWith("U+")) {
    const usv = parseInt(s.slice(2), 16); // Unicode scalar value
    if (
      usv > 0 &&
      usv < 0x10ffff &&
      (usv & 0xffff) < 0xfffe &&
      (usv < 0xd800 || usv > 0xdfff)
    ) {
      // To support codepoints greater than U+FFFF, use fromCodePoint, not fromCharCode
      return String.fromCodePoint(usv);
    }
  }
  return s;
}

function tagTooltip(tag) {
  // To support codepoints greater than U+FFFF, use codePointAt, NOT charCodeAt.
  const firstChar = tag.codePointAt(0);
  const usv = "U+" + firstChar.toString(16).toUpperCase().padStart(4, "0");
  const name = unicodeName(firstChar);
  return usv + " " + name;
}
</script>

<style scoped>
.new-tag-input {
  width: 78px;
  margin: 0 8px 0 0;
}

.new-tag-plus {
  background: #fff;
  border-style: dashed;
}
</style>
