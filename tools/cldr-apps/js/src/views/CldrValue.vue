<template>
  <span
    v-bind:dir="dir"
    v-bind:lang="lang"
    v-bind:title="title"
    :class="myClass"
    ><slot
  /></span>
</template>

<script>
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrStatus from "../esm/cldrStatus.js";
export default {
  computed: {
    myClass() {
      return "cldrValue " + (this.class || "");
    },
  },
  props: {
    class: {
      type: String,
      // any additional classes
      default: "",
    },
    title: {
      type: String,
      // any additional classes
      default: null,
    },
    dir: {
      type: String,
      // defaults to current locale's dir
      default: () => cldrLoad.getLocaleDir(cldrStatus.getCurrentLocale()),
    },
    lang: {
      type: String,
      // defaults to current locale's lang
      default: () => cldrStatus.getCurrentLocale(),
    },
  },
};
</script>

<style scoped>
/* Note: cldrValue is defined in surveytool.css */
</style>
