<template>
  <div class="info-regional-variants">
    <div v-if="loading">
      <p>
        <a-spin size="small" />
        {{ loadingMessage }}
      </p>
    </div>
    <div v-else-if="label && items">
      <label for="regional-variants-menu">{{ label }}</label>
      <br />
      <select
        id="regional-variants-menu"
        v-model="chosenLocale"
        :title="label"
        @:change="goToLocale()"
      >
        <option
          v-for="item in items"
          :key="item.value"
          :value="item.value"
          :disabled="item.disabled"
        >
          {{ item.str }}
        </option>
      </select>
    </div>
    <div v-else-if="clickToLoad">
      <button @click="clickClickToLoad">Check Regional Variants…</button>
    </div>
  </div>
</template>

<script>
import * as cldrSideways from "../esm/cldrSideways.mjs";
import * as cldrText from "../esm/cldrText.mjs";

export default {
  data() {
    return {
      chosenLocale: null,
      curLocale: null,
      items: null,
      label: null,
      loading: false,
      loadingMessage: null,
      clickToLoad: null,
    };
  },

  methods: {
    clear() {
      this.label = this.items = this.chosenLocale = this.curLocale = null;
      this.loading = false;
    },

    setLoading() {
      this.clear();
      this.clickToLoad = null;
      if (!this.loadingMessage) {
        this.loadingMessage = cldrText.get("sideways_loading1");
      }
      this.loading = true;
    },

    setData(localeId, d) {
      this.clickToLoad = null;
      this.chosenLocale = this.curLocale = localeId;
      this.label = d?.label || null;
      this.items = d?.items || null;
      this.loading = false;
    },

    goToLocale() {
      this.clickToLoad = null;
      if (this.chosenLocale !== this.curLocale) {
        cldrSideways.goToLocale(this.chosenLocale);
      }
    },

    setClickToLoad(fn) {
      this.clear();
      this.clickToLoad = fn;
    },

    clickClickToLoad() {
      if (this.clickToLoad) {
        this.clickToLoad();
      }
    },
  },
};
</script>

<style scoped>
.info-regional-variants {
  margin: 12px 4px 6px 4px;
}

#regional-variants-menu {
  font-size: smaller;
  margin-left: 8px;
}
</style>
