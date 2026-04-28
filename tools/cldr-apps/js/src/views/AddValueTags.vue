<template>
  <span class="tag-array">
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
      <template v-if="shouldDisplayAsTag(tag)">
        <a-tag @click="handleClickTag($event, index)" class="regular-tag">
          <a-tooltip>
            <template #title> {{ tagTooltip(tag) }} </template>
            {{ displayTag(tag) }}
          </a-tooltip>
        </a-tag>
      </template>
      <template v-else> {{ tag }} </template>
    </template>
  </span>
</template>

<script setup>
import { onMounted, ref } from "vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";
import * as cldrChar from "../esm/cldrChar.mjs";

import AddValueCharMenu from "./AddValueCharMenu.vue";

const DEBUG = false;

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

function shouldDisplayAsTag(tag) {
  return cldrAddValue.shouldDisplayAsTag(tag);
}

function displayTag(tag) {
  return cldrAddValue.textForTag(tag);
}

function tagTooltip(tag) {
  return cldrAddValue.tagTooltipPlusClick(tag);
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

<style scoped>
.regular-tag {
  margin: 1px;
  padding: 1px;
}

/* Prevent the text to the right of the menu moving when the menu is opened */
.tag-menu {
  width: 0 !important;
}

.tag-array {
  font-weight: bold;
  color: black;
  margin: 1px;
  padding: 1px;
  display: unset;
}
</style>
