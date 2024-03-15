<template>
  <div>
    <div v-if="altText" class="secondary">
      <i>{{ altLang }}: </i
      ><cldr-value @lang="altLang"> {{ altText }} </cldr-value>
    </div>
    <div>
      <a-input placeholder="fr" v-model:value="altLang">
        <template #prefix> Other Lang: </template>
      </a-input>
    </div>
    <section id="InfoPanelSection">
      <header class="sidebyside-column-top">
        <button
          class="cldr-nav-btn info-panel-closebox"
          title="Close the Info Panel"
          @click="closeInfoPanel"
        >
          ✕
        </button>
        <button
          class="cldr-nav-btn"
          title="Reload the Info Panel"
          @click="reloadInfoPanel"
        >
          ↻
        </button>
        <span class="i-am-info-panel">Info Panel</span>
        <span class="explainer">
          <a-button
            v-if="locale && id"
            shape="circle"
            class="cldr-nav-btn"
            title="Explain inheritance for this item"
            @click="explain"
          >
            ↓
          </a-button>
          <InheritanceExplainer ref="inheritanceExplainer" />
        </span>
      </header>
    </section>
  </div>
</template>

<script>
import { ref } from "vue";
import * as cldrInfo from "../esm/cldrInfo.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";
import * as cldrClient from "../esm/cldrClient.mjs";
import InheritanceExplainer from "./InheritanceExplainer.vue";
import CldrValue from "./CldrValue.vue";

const altText = ref("");
const altLang = ref("");

export default {
  setup() {
    let inheritanceExplainer = ref(null);
    return {
      inheritanceExplainer,
      locale: cldrStatus.refs.currentLocale,
      id: cldrStatus.refs.currentId,
      altLang,
      altText,
    };
  },
  watch: {
    async id(xpstrid) {
      if (!altLang.value) {
        altText.value = "";
        return;
      }
      const client = await cldrClient.getClient();
      const locale = altLang.value;
      const { body } = await client.apis.voting.getRow({ xpstrid, locale });
      const { page } = body;
      const { rows } = page;
      const row = Object.entries(rows)[0][1];
      const { winningValue } = row;
      // ↑↑↑
      altText.value = winningValue || "";
    },
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

.explainer {
  margin-left: auto !important;
  margin-right: 1ex;
}
</style>
