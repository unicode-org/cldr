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

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrChar from "../esm/cldrChar.mjs";

const DEBUG = false;

const props = defineProps({
  modelValue: String,
});

onMounted(mounted);

function mounted() {
  convertTextToTags(props.modelValue);
}

const tagArray = ref([]);

function convertTextToTags(text) {
  // Each character becomes a tag (in this "basic" version)
  const tags = cldrChar.split(text);
  tagArray.value = tags;
  if (DEBUG) {
    console.log("convertTextToTags: text = " + text + "; tags = " + tags);
  }
}

function displayTag(tag) {
  return cldrAddValue.textForTag(tag);
}

function tagTooltip(tag) {
  return cldrAddValue.tagTooltip(tag);
}
</script>

<style scoped>
.small-margin {
  margin: 0 3px 0 0;
}
</style>
