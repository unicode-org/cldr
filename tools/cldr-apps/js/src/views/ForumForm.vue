<script setup>
import { onMounted, reactive, ref } from "vue";

import * as cldrForum from "../esm/cldrForum.mjs";
import * as cldrForumType from "../esm/cldrForumType.mjs";
import * as cldrText from "../esm/cldrText.mjs";

const DEBUG_FORUM_FORM = false;

const formRef = ref();

const props = defineProps({
  pi: Object,
  reminder: String,
});

const formState = reactive({
  body: cldrForum.prefillPostText(props.pi),
});

const rules = {
  body: [
    {
      validator: validateBody,
      trigger: "change",
    },
  ],
};

const emit = defineEmits(["submit-or-cancel"]);

const textAreaToFocus = ref(null);

function focusInput() {
  if (textAreaToFocus.value) {
    textAreaToFocus.value.focus();
  }
}

onMounted(focusInput);

async function validateBody(_rule, bodyText) {
  if (!bodyText.trim()) {
    return Promise.reject("Please enter a message body");
  } else if (props.pi.postType === cldrForumType.REQUEST) {
    const prefill = cldrText.sub("forum_prefill_request", [props.pi.value]);
    if (bodyText.trim() === prefill.trim()) {
      return Promise.reject("Please edit the message body");
    }
  }
  return Promise.resolve();
}

function onCancel() {
  emit("submit-or-cancel", null);
}

function onSubmit() {
  formRef.value
    .validate()
    .then(() => {
      emit("submit-or-cancel", formState);
    })
    .catch((error) => {
      if (DEBUG_FORUM_FORM) {
        console.log("onSubmit validation error", error);
      }
    });
}

function formatParent(parentPost) {
  const div = cldrForum.parseContent([parentPost], "parent");
  return new XMLSerializer().serializeToString(div);
}
</script>

<template>
  <header>Compose forum post</header>
  <a-form
    ref="formRef"
    :model="formState"
    :rules="rules"
    autocomplete="off"
    autofocus
  >
    <p class="subject">{{ pi.subject }}</p>
    <p class="reminder">{{ reminder }}</p>
    <p class="postType">{{ pi.postType }}</p>
    <a-form-item class="formItems" name="body" has-feedback>
      <a-textarea
        v-model:value="formState.body"
        placeholder="Write your post (plain text) here..."
        ref="textAreaToFocus"
        :rows="4"
      />
    </a-form-item>
    <div class="buttons">
      <a-form-item>
        <a-button html-type="cancel" @click="onCancel">Cancel</a-button>
        &nbsp;
        <a-button type="primary" html-type="submit" @click="onSubmit"
          >Submit</a-button
        >
      </a-form-item>
    </div>
    <div v-if="pi.parentPost" v-html="formatParent(pi.parentPost)"></div>
  </a-form>
</template>

<style scoped>
header {
  font-size: larger;
  font-weight: bold;
  margin-bottom: 1em;
}

.buttons {
  display: flex;
  justify-content: flex-end;
}

.formItems {
  margin: 1ex;
  padding: 0;
}

.subject {
  font-weight: bold;
  margin-bottom: 1em;
}

.reminder {
  margin-bottom: 1em;
}

.postType {
  text-align: right;
  color: red;
}
</style>
