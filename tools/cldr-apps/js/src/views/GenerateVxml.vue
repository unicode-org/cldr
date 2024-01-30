<template>
  <div v-if="!hasPermission">Please log in as Admin to use this feature.</div>
  <div v-else>
    <p v-if="status">Current Status: {{ status }}</p>
    <p v-if="message">
      <span v-html="message"></span>
    </p>
    <hr />
    <p>
      <button v-if="canStop()" @click="stop()">Stop</button>
      <button v-else title="Really Generate VXML" @click="start()">
        Generate VXML Now
      </button>
    </p>
  </div>
  <div v-if="errMessage">
    {{ errMessage }}
  </div>
  <span v-html="output"></span>
</template>

<script>
import * as cldrGenerateVxml from "../esm/cldrGenerateVxml.mjs";

export default {
  data() {
    return {
      hasPermission: false,
      errMessage: null,
      inProgress: false,
      message: null,
      output: null,
      status: "READY",
    };
  },

  created() {
    cldrGenerateVxml.viewCreated(this.setData);
    this.hasPermission = cldrGenerateVxml.canGenerateVxml();
  },

  methods: {
    start() {
      if (this.hasPermission) {
        cldrGenerateVxml.start();
        this.status = "WAITING";
      }
    },

    stop() {
      cldrGenerateVxml.stop();
      this.status = "READY";
    },

    canStop() {
      return this.status === "WAITING" || this.status === "PROCESSING";
    },

    setData(data) {
      this.message = data.message;
      this.percent = data.percent;
      if (data.status) {
        this.status = data.status;
      }
      if (data.output) {
        this.output = data.output;
        this.status = "READY";
      }
    },
  },
};
</script>

<style scoped>
button,
select {
  margin-top: 1ex;
}
</style>
