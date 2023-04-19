<template>
  <div>
    <section id="InfoPanelSection">
      <header class="sidebyside-column-top">
        <a-button
          shape="circle"
          class="cldr-nav-btn info-panel-closebox"
          title="Close"
          @click="closeInfoPanel"
        >
          ✕
        </a-button>
        <span class="i-am-info-panel">Info Panel</span>
        <a-button
          shape="circle"
          class="cldr-nav-btn"
          title="Reload"
          @click="reloadInfoPanel"
        >
          ↻
        </a-button>
        <a-button
          shape="circle"
          class="cldr-nav-btn"
          title="Reload"
          @click="explain"
        >
          ↑
        </a-button>
      </header>
    </section>
    <InheritanceExplainer ref="inheritanceExplainer" />
  </div>
</template>

<script>
import { ref } from "vue";
import * as cldrInfo from "../esm/cldrInfo.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import InheritanceExplainer from "./InheritanceExplainer.vue";

export default {
  setup() {
    let inheritanceExplainer = ref(null);
    return {
      inheritanceExplainer,
    };
  },
  components: {
    InheritanceExplainer,
  },
  methods: {
    closeInfoPanel() {
      cldrInfo.closePanel();
    },

    reloadInfoPanel() {
      cldrInfo.clearCachesAndReload();
    },

    explain() {
      this.inheritanceExplainer.explain(
        cldrStatus.getCurrentLocale(),
        cldrStatus.getCurrentId()
      );
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
  flex-grow: 1;
}
</style>
