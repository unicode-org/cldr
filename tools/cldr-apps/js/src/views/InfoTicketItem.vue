<template>
  <div v-if="linkUrl && linkText">
    <a-tooltip :title="hover">
      <div class="link-style">
        <span class="glyphicon glyphicon-list-alt"></span>&nbsp;&nbsp;
        <a :href="linkUrl">{{ linkText }}</a>
      </div>
    </a-tooltip>
  </div>
</template>

<script setup>
import { ref } from "vue";

import * as cldrText from "../esm/cldrText.mjs";

const linkUrl = ref(null);
const linkText = ref(null);
const hover = cldrText.get("file_ticket_hover");

function setLink(url, text) {
  linkUrl.value = url;
  linkText.value = text;
}

defineExpose({
  setLink,
});
</script>

<style scoped>
.link-style {
  background: #bce8f1;
  color: #1890ff;
  margin-top: 4px;
  padding: 4px;
}
</style>
