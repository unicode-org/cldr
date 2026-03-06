<template>
  <template v-for="(tag, _index) in tagArray" :key="_index">
    <a-tag class="small-margin">
      <a-tooltip>
        <template #title> {{ tagTooltip(tag) }} </template>
        {{ displayTag(tag) }}
      </a-tooltip>
    </a-tag>
  </template>
</template>

<script setup>
import { onMounted, ref } from "vue";

import * as cldrChar from "../esm/cldrChar.mjs";

const DEBUG = true;

const props = defineProps({
  modelValue: String,
});

onMounted(mounted);

function mounted() {
  convertTextToTags(props.modelValue);
}

const tagArray = ref([]);

function convertTextToTags(text) {
  // Currently each character becomes a tag.
  // In the future some sequences of characters might be combined into single tags.
  const tags = cldrChar.split(text);
  tagArray.value = tags;
  if (DEBUG) {
    console.log("convertTextToTags: text = " + text + "; tags = " + tags);
  }
}

function displayTag(tag) {
  const c = cldrChar.firstChar(tag);
  if (cldrChar.isWhiteSpace(c)) {
    const codePoint = cldrChar.firstCodePoint(c);
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
.small-margin {
  margin: 0 3px 0 0;
}
</style>
