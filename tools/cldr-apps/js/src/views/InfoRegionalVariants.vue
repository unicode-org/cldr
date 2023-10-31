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
    };
  },

  methods: {
    setLoading() {
      if (!this.loadingMessage) {
        this.loadingMessage = cldrText.get("sideways_loading1");
      }
      this.loading = true;
      this.label = this.items = this.chosenLocale = this.curLocale = null;
    },

    setData(localeId, d) {
      this.chosenLocale = this.curLocale = localeId;
      this.label = d?.label || null;
      this.items = d?.items || null;
      this.loading = false;
    },

    goToLocale() {
      if (this.chosenLocale !== this.curLocale) {
        cldrSideways.goToLocale(this.chosenLocale);
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
