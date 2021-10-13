<template>
  <div>
    <a-spin size="large" v-if="loading && !participationData">
      <p>This will take a long time.</p>
    </a-spin>
    <button @click="fetchData" v-if="!loading && !participationData">
      Load Data (slow)
    </button>
    <div v-if="localeTxt">
      <h2>Locales.txt (generated)</h2>
      <textarea rows="66" cols="160" v-model="localeTxt" />
    </div>
  </div>
</template>

<script>
import { ref } from "vue";
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as localesTxtGenerator from "../esm/localesTxtGenerator.mjs";

export default {
  setup() {
    return {
      loading: ref(null),
      localeTxt: ref(null),
      participationData: ref(null),
    };
  },
  // Note: note loading fetchData()  on create() due to slowness
  methods: {
    async fetchData() {
      try {
        this.loading = true;
        const raw = await cldrAjax.doFetch("api/voting/participation");
        this.participationData = await raw.json();
        this.localeTxt = localesTxtGenerator
          .generateLocalesTxt(this.participationData)
          .join("\n");
      } catch (e) {
        this.localeTxt = "Error " + e;
      }
    },
  },
};
</script>

<style></style>
