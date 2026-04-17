<template>
  <!-- https://www.antdv.com/components/select/ -->
  <a-select
    autofocus
    defaultOpen
    :showArrow="false"
    :dropdownMatchSelectWidth="false"
    :placeholder="null"
    v-model:value="chosenChar"
    class="tag-menu"
    @change="handleChooseCharacter"
    @dropdownVisibleChange="handleDropdownVisibleChange"
    @select="handleSelect"
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

import * as cldrChar from "../esm/cldrChar.mjs";
import * as cldrEscaper from "../esm/cldrEscaper.mjs";

const DEBUG = true;

const props = defineProps(["modelValue"]);
const emit = defineEmits([
  "change",
  "update:modelValue",
  "insertMenuIsVisible",
]);

onMounted(mounted);

function mounted() {
  chosenChar.value = props.modelValue;
  if (DEBUG) {
    if (!chosenChar.value) {
      console.log("AddValueCharMenu mounted: chosenChar.value is falsy");
    } else {
      console.log(
        "AddValueCharMenu mounted: chosenChar.value = [" +
          chosenChar.value +
          "]" +
          "; cldrChar.firstCodePoint(chosenChar.value) = " +
          cldrChar.firstCodePoint(chosenChar.value)
      );
    }
  }
  populateCharMenu();
}

function updateParent() {
  if (DEBUG) {
    console.log(
      "AddValueCharMenu updateParent: chosenChar.value = [" +
        chosenChar.value +
        "]" +
        "; cldrChar.firstCodePoint(chosenChar.value) = " +
        cldrChar.firstCodePoint(chosenChar.value)
    );
  }
  emit("change");
  emit("update:modelValue", chosenChar.value);
  emit("insertMenuIsVisible", insertMenuIsVisible.value);
}

const menuOptions = ref([]);
const chosenChar = ref(undefined);
const insertMenuIsVisible = ref(false);

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
  chosenChar.value = String.fromCodePoint(codePoint);
  if (DEBUG) {
    console.log(
      "handleChooseCharacter codePoint = " +
        codePoint +
        "; chosenChar.value = [" +
        chosenChar.value +
        "]" +
        "; cldrChar.firstCodePoint(chosenChar.value) = " +
        cldrChar.firstCodePoint(chosenChar.value)
    );
  }
  updateParent();
}

function handleSelect(codePoint) {
  chosenChar.value = String.fromCodePoint(codePoint);
  if (DEBUG) {
    console.log(
      "handleSelect codePoint = " +
        codePoint +
        "; chosenChar.value = [" +
        chosenChar.value +
        "]" +
        "; cldrChar.firstCodePoint(chosenChar.value) = " +
        cldrChar.firstCodePoint(chosenChar.value)
    );
  }
  updateParent();
}

function handleDropdownVisibleChange(isVisible) {
  insertMenuIsVisible.value = isVisible;
  if (DEBUG) {
    console.log("handleDropdownVisibleChange: isVisible = " + isVisible);
  }
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
/* Prevent the text to the right of the tag moving when the menu is opened */
.tag-menu {
  width: 0 !important;
}
</style>
