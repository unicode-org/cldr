<template>
  <button v-on:click="loginout()">{{ logText }}</button>
</template>

<script>
import { reload } from "../esm/cldrForum.js";
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
      } else {
        this.logText = "Log In";
      }
    },
    async loginout() {
      if (this.loggedIn) {
        await fetch(`api/auth/logout?session=${cldrStatus.getSessionId()}`);
        // now reload this page now that we've logged out
        await window.location.reload();
      } else {
        window.location.replace("./login.jsp");
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
