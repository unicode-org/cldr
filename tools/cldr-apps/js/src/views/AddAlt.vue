<template>
  <section>
    <button
      v-if="!altMenu && !success"
      title="Add an alt path"
      @click="getMenu"
    >
      +alt
    </button>
    <div v-if="altMenu && altMenu.length">
      <label for="chosenAlt">alt=</label>
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
    <div>
      <span v-if="chosenAlt">
        <button title="Add alt path now" @click="reallyAdd">Add</button>
        &nbsp;
      </span>
      <span v-if="chosenAlt || errMessage">
        <button title="Do not add alt path" @click="reset">Cancel</button>
      </span>
    </div>
    <div v-if="errMessage">
      {{ errMessage }}
    </div>
    <div v-if="success">
      “alt” added.
      <button @click="clickLoad">Reload Page</button>
    </div>
  </section>
</template>

<script>
import * as cldrAddAlt from "../esm/cldrAddAlt.mjs";

export default {
  data() {
    return {
      xpstrid: null,
      altMenu: null,
      chosenAlt: "",
      errMessage: null,
      success: false,
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
        cldrAddAlt.addChosenAlt(
          this.xpstrid,
          this.chosenAlt,
          this.showResult /* callback */
        );
      }
    },

    showResult(errMessage) {
      this.reset();
      this.errMessage = errMessage; // null or empty for success
      if (!errMessage) {
        this.success = true;
      }
    },

    clickLoad() {
      this.reset();
      cldrAddAlt.reloadPage();
    },

    reset() {
      this.altMenu = null;
      this.chosenAlt = "";
      this.errMessage = null;
      this.success = false;
    },
  },
};
</script>

<style scoped>
button,
select {
  margin-top: 1ex;
}

section {
  clear: both;
  float: right;
}
</style>
