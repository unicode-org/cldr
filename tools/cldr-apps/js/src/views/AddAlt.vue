<template>
  <section id="AddAltSection">
    <button class="cldr-nav-btn" title="Add Alt Path" @click="getMenu">
      Ⓐ
    </button>
    <div v-if="altMenu && altMenu.length">
      <label for="chosenAlt">Alt value: </label>
      <select
        id="chosenAlt"
        name="chosenAlt"
        v-model="chosenAlt"
        v-on:change="setChosenAlt()"
        title="Add an alt path"
      >
        <option :key="n" v-for="n in altMenu">{{ n }}</option>
      </select>
    </div>
    <div v-if="chosenAlt">
      <label>Chosen:</label>
      {{ chosenAlt }}
    </div>
  </section>
</template>

<script>
import * as cldrAddAlt from "../esm/cldrAddAlt.js";

export default {
  data() {
    return {
      xpstrid: null,
      altMenu: null,
      chosenAlt: null,
    };
  },

  methods: {
    setXpathStringId(xpstrid) {
      this.xpstrid = xpstrid;
    },

    getMenu() {
      if (this.xpstrid) {
        cldrAddAlt.getAlts(this.xpstrid, this.setAlts /* callback */);
      }
    },

    setAlts(json) {
      this.altMenu = json.alt;
    },

    setChosenAlt() {
      // this.chosenAlt;
    },
  },
};
</script>

<style scoped>
button {
  background-color: rgb(235, 184, 114);
  font-size: 1em;
}
</style>
