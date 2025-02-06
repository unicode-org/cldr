<script setup>
import { nextTick, ref } from "vue";

import * as cldrAddValue from "../esm/cldrAddValue.mjs";

const xpstrid = ref(""); // xpath string id
const newValue = ref("");
const formLeft = ref(0);
const formTop = ref(0);
const formIsVisible = ref(false);
const inputToFocus = ref(null);

function setXpathStringId(id) {
  xpstrid.value = id;
}

function showModal(event) {
  // Use the coordinates of the button's top-left corner
  formLeft.value = event.clientX - event.offsetX;
  formTop.value = event.clientY - event.offsetY;
  newValue.value = "";
  formIsVisible.value = true;
  cldrAddValue.setFormIsVisible(true, xpstrid.value);
  nextTick(focusInput);
}

function focusInput() {
  if (inputToFocus.value) {
    inputToFocus.value.focus();
  }
}

function onEnglish() {
  newValue.value = cldrAddValue.getEnglish(xpstrid.value);
}

function onWinning() {
  newValue.value = cldrAddValue.getWinning(xpstrid.value);
}

function onCancel() {
  formIsVisible.value = false;
  cldrAddValue.setFormIsVisible(false, xpstrid.value);
}

function onSubmit() {
  formIsVisible.value = false;
  cldrAddValue.setFormIsVisible(false, xpstrid.value);
  if (newValue.value) {
    cldrAddValue.sendRequest(xpstrid.value, newValue.value);
  }
}

defineExpose({
  setXpathStringId,
});
</script>

<template>
  <div>
    <!-- If use a-button instead of button, form positioning fails -->
    <button class="plus" type="button" @click="showModal">
      ✚
      <!-- U+271A HEAVY GREEK CROSS -->
    </button>
    <a-modal
      v-model:visible="formIsVisible"
      :closable="false"
      :footer="null"
      :style="{
        position: 'sticky',
        left: formLeft + 'px',
        top: formTop + 'px',
      }"
      @ok="onSubmit"
    >
      <a-input
        v-model:value="newValue"
        placeholder="Add a translation"
        ref="inputToFocus"
        @keydown.enter="onSubmit"
      />
      <div class="button-container">
        <a-button @click="onEnglish">→English</a-button>
        <a-button @click="onWinning">→Winning</a-button>
        <a-button type="cancel" @click="onCancel">Cancel</a-button>
        <a-button type="primary" @click="onSubmit">Submit</a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.button-container {
  display: flex;
  justify-content: space-between;
  padding-top: 1em;
}

.plus {
  font-size: 118%;
  border-radius: 4px;
  padding: 6px 12px;
  color: #fff;
  background-color: #428bca;
  border: 1px solid #345578;
}
</style>
