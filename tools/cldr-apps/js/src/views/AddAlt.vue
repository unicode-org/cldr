<template>
  <section>
    <button v-if="!altMenu" title="Add an alt path" @click="getMenu">
      +alt
    </button>
    <div v-if="altMenu && altMenu.length">
      <label for="chosenAlt">Alt: </label>
      <select
        id="chosenAlt"
        name="chosenAlt"
        v-model="chosenAlt"
        title="Choose an alt attribute"
      >
        <option disabled value="">Please Select</option>
        <option :key="alt" v-for="alt in altMenu">{{ alt }}</option>
      </select>
    </div>
    <div v-if="chosenAlt">
      <button title="Add alt path now" @click="reallyAdd">Add</button>
      &nbsp;
      <button title="Do not add alt path" @click="cancel">Cancel</button>
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
      chosenAlt: "",
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

    // callback
    setAlts(json) {
      this.altMenu = json.alt;
    },

    reallyAdd() {
      if (this.xpstrid && this.chosenAlt) {
        cldrAddAlt.addChosenAlt(this.xpstrid, this.chosenAlt);
      }
    },

    cancel() {
      this.altMenu = null;
      this.chosenAlt = "";
    },
  },
};
</script>

<style scoped>
button, select {
  margin-top: 1ex;
}
</style>
