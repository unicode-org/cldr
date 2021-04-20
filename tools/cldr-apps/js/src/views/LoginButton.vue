<template>
  <div class="loginwrap">
    <div v-if="!user" class="loginform">
      <a-input placeholder="Username" v-model:value="userName">
        <template #prefix>
          <span class="glyphicon glyphicon-user tip-log" />
        </template>
      </a-input>
      <a-input-password placeholder="Password" v-model:value="password">
      </a-input-password>
      <a-checkbox v-model:checked="remember">Stay Logged In</a-checkbox>
    </div>
    <a-button
      :disabled="!user && (!userName || !password)"
      v-on:click="loginout()"
      >{{ logText }}</a-button
    >
  </div>
</template>

<script>
import * as cldrStatus from "../esm/cldrStatus.js";
import { run } from "../esm/cldrGui.js";
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
        if (this.userName && this.password) {
          try {
            const response = await fetch(
              `api/auth/login?remember=${this.remember}`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                  Accept: "application/json",
                },
                body: JSON.stringify({
                  email: this.userName,
                  password: this.password,
                }),
              }
            );
            const logintoken = await (await response).json();
            console.dir(logintoken);
            if (logintoken.user && logintoken.sessionId) {
              // logged in OK.
              cldrStatus.setSessionId(logintoken.sessionId);
              run(); // Restart everything
            }
          } catch (e) {
            // TODO: popover
            throw e;
          }
        }
      }
    },
  },
  data() {
    return {
      // start empty
      user: null,
      logText: "",
      logLink: "",
      userName: "",
      password: "",
      remember: true,
    };
  },
};
</script>

<style scoped>
.loginwrap {
  display: inline;
}

.loginform {
  display: inline-block;
}

.loginform a-input {
  display: inline;
}
</style>
