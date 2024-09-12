<script setup>
import { ref, reactive } from "vue";

import * as cldrForum from "../esm/cldrForum.mjs";
import ForumForm from "./ForumForm.vue";

let pi = null; /* PostInfo object */
let label = ref(null);
let formIsVisible = ref(false);
let reminder = ref("");
let disabled = ref(false);

/**
 * Copy of pi.willFlag; see comment in setPostInfo
 */
let willFlag = ref(false);

/**
 * Set the PostInfo
 *
 * @param {PostInfo} xpi
 */
function setPostInfo(xpi) {
  pi = reactive(xpi);
  // Directly referencing pi?.willFlag in the template does not work,
  // so we make a copy of willFlag to use in the template.
  // https://vuejs.org/guide/essentials/class-and-style
  willFlag.value = xpi.willFlag;
}

function setLabel(xlabel) {
  label.value = xlabel;
}

function setReminder(xreminder) {
  reminder.value = xreminder;
}

function setDisabled() {
  disabled.value = true;
}

function openForm() {
  formIsVisible.value = true;
  cldrForum.setFormIsVisible(true);
}

function handleSubmitOrCancel(formState) {
  formIsVisible.value = false;
  cldrForum.setFormIsVisible(false);
  if (formState?.body) {
    cldrForum.sendPostRequest(pi, formState.body);
  }
}

defineExpose({
  setDisabled,
  setLabel,
  setPostInfo,
  setReminder,
});
</script>

<template>
  <a-button
    :disabled="disabled"
    :class="{ forumNewPostFlagButton: willFlag }"
    @click="openForm"
  >
    {{ label }}
  </a-button>
  <template v-if="formIsVisible">
    <div ref="popover" class="popoverForm">
      <ForumForm
        :pi="pi"
        :reminder="reminder"
        @submit-or-cancel="handleSubmitOrCancel"
      />
    </div>
  </template>
</template>

<style scoped>
body {
  overflow-x: hidden;
}

button {
  margin: 0.5em;
}

.popoverForm {
  display: block;
  top: 10%;
  left: 10%;
  width: 80%;
  position: absolute;
  padding: 20px 20px;
  z-index: 1200;
  background-color: #f5f5f5;
  border: 1px solid #e3e3e3;
  border-radius: 3px;
}
</style>
