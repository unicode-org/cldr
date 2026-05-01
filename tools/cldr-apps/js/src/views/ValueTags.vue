<!-- This is displayed in a cell (English/Winning/Others) of the main vetting table.
    It resembles part of AddValueTags.vue, which is displayed in an Add-Value dialog box.
    There are some style and user-interface differences between those two contexts, and
    also similarities that should be preserved for consistency. -->
<template>
  <span class="tag-area" title="Special characters, click ⓘ for details.">
    <span class="left-side">
      <template v-for="(tag, index) in tagArray" :key="index">
        <template v-if="shouldDisplayAsTag(tag)">
          <a-tag class="regular-tag">
            <a-tooltip>
              <template #title> {{ tagTooltip(tag) }} </template>
              {{ displayTag(tag) }}
            </a-tooltip>
          </a-tag>
        </template>
        <template v-else> {{ tag }} </template>
      </template>
    </span>
    <span class="right-side">
      <a
        href="https://cldr.unicode.org/translation/getting-started/guide#special-characters"
        >ⓘ</a
      >
    </span>
  </span>
</template>

<script setup>
import { ref } from "vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";

const tagArray = ref([]);

function setValue(s) {
  tagArray.value = cldrAddValue.convertTextToTags(s);
}

function shouldDisplayAsTag(tag) {
  return cldrAddValue.shouldDisplayAsTag(tag);
}

function displayTag(tag) {
  return cldrAddValue.textForTag(tag);
}

function tagTooltip(tag) {
  return cldrAddValue.tagTooltip(tag);
}

defineExpose({
  setValue,
});
</script>

<style scoped>
.regular-tag {
  margin: 0 1px; /* top right (bottom left) */
  padding: 0 1px;
}

.tag-area {
  user-select: none;
  display: flex;
  flex-wrap: wrap;
  margin: 1px;
  padding: 1px;
  background-color: #dff;
  border: 1px solid #0ff;
  border-radius: 2px; /* like Ant a-input */
  font-weight: bold;
}

/* Make the ⓘ (right side) right-justified by adding this style to the left side */
.left-side {
  flex-grow: 1;
}

.right-side {
  color: black;
}

/* Make the ⓘ (right side) more salient when hovering on the tag area, less salient otherwise */
.tag-area:hover > .right-side {
  opacity: 1;
}

.tag-area > .right-side {
  opacity: 0.1;
}
</style>
