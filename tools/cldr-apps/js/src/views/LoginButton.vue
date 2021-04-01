<template>
  <div>
    <a v-bind:href="logLink">{{ logText }}</a>
  </div>
</template>

<script>
import * as cldrStatus from "../esm/cldrStatus.js";

export default {
  created: function () {
    // listen for updates
    cldrStatus.on(
      "surveyUser",
      (() => {
        this.update();
      }).bind(this)
    );
    this.update(); // fetch initial data (probably missing)
  },
  methods: {
    update() {
      this.user = cldrStatus.getSurveyUser();
      const loggedIn = !!this.user;
      this.loggedIn = loggedIn;

      if (loggedIn) {
        this.logText = "Log Out";
        this.logLink = "./survey?do=logout";
      } else {
        this.logText = "Log In";
        this.logLink = "./login.jsp";
      }
    },
  },
  data() {
    return {
      // start empty
      user: null,
      logText: "",
      logLink: "",
    };
  },
};
</script>

<style></style>
