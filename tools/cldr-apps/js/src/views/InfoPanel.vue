<template>
  <section id="InfoPanelSection">
    <header class="sidebyside-column-top">
      <button
        class="cldr-nav-btn info-panel-closebox"
        title="Close"
        @click="closeInfoPanel"
      >
        ✕
      </button>
      <span class="i-am-info-panel">Info Panel</span>
    </header>
  </section>
</template>

<script>
import * as cldrInfo from "../esm/cldrInfo.js";

export default {
  methods: {
    closeInfoPanel() {
      cldrInfo.closePanel();
    },
  },
};
</script>

<style scoped>
#InfoPanelSection {
  border-top: 1px solid #cfeaf8;
  display: flex;
  flex: none;
  flex-direction: row;
  font-size: small;
  overflow: hidden;
}

header {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  padding: 1ex 0;
  background-color: white;
  background-image: linear-gradient(white, #e7f7ff);
}

.info-panel-closebox {
  margin-left: 1ex;
}

.i-am-info-panel {
  font-weight: bold;
  margin-right: 1ex;
}
</style>
