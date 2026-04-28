<template>
  <!-- for now, just a welcome Markdown -->
  <div>
    <div id="welcomeHtml" v-html="welcomeHtml" />
    <hr />
    <a-button @click="showSidebar">Show Locales…</a-button>
  </div>
</template>

<script setup>
// Note: SignCla.vue uses a similar pattern.
// We could consider a common component for both.
import { marked } from "../esm/cldrMarked.mjs";
import * as cldrEvent from "../esm/cldrEvent.mjs";

// Load Markdown source
import welcomeMd from "../md/welcome.md";
// … render it
const welcomeHtml = marked(welcomeMd);

function showSidebar() {
  cldrEvent.forceSidebar();
}
</script>

<style scoped>
div#welcomeHtml {
  /* left margin so the sidebar doesn't override */
  margin-left: 30%;
  margin-right: 8%;
}
</style>
