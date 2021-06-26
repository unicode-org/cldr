<template>
  <a-popover
    v-model:visible="loginShown"
    title="Enter your SurveyTool credentials:"
    placement="leftBottom"
    trigger="none"
  >
    <template #content>
      <a-input placeholder="Username" v-model:value="userName">
        <template #prefix>
          <span class="glyphicon glyphicon-user tip-log" />
        </template>
      </a-input>
      <a-input-password placeholder="Password" v-model:value="password">
      </a-input-password>
      <a-button
        class="cldr-nav-btn"
        :disabled="!user && (!userName || !password)"
        v-on:click="login()"
        >Log In</a-button
      >
      <a-checkbox v-model:checked="remember">Stay Logged In</a-checkbox>
      &nbsp;
      <a-alert
        v-if="loginErrorMessage"
        type="error"
        v-model:message="loginErrorMessage"
      />
    </template>
    <a-button class="cldr-nav-btn" v-on:click="loginout()">{{
      logText
    }}</a-button>
  </a-popover>
</template>

<script>
import { notification } from "ant-design-vue";
import * as cldrStatus from "../esm/cldrStatus.js";
import { run } from "../esm/cldrGui.js";
import { ref } from "vue";
export default {
  setup() {
    const loginShown = ref(false);
    const loginErrorMessage = ref(null);
    return {
      loginShown,
      loginErrorMessage,
    };
  },
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

      if (this.loginShown.value) {
        this.logText = "Cancel";
      } else if (loggedIn) {
        this.logText = "Log Out";
      } else {
        this.logText = "Log In";
      }
    },
    /**
     * Log out
     */
    async logout() {
      await fetch(`api/auth/logout?session=${cldrStatus.getSessionId()}`);
      // now reload this page now that we've logged out
      await window.location.reload();
    },
    /**
     * Log in (from header button)
     */
    async login() {
      function errBox(message) {
        console.error("LoginButton.vue: " + message);
        notification.error({
          message,
          placement: "topLeft",
        });
      }

      this.loginErrorMessage = null;
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
          if (!response.ok) {
            if (response.status == 403) {
              return errBox("Unauthorized:\nCheck the username and password.");
            } else {
              return errBox(`Login failed: HTTP ${response.status}`);
            }
            return;
          }
          const logintoken = await (await response).json();
          if (logintoken.user && logintoken.sessionId) {
            // logged in OK.
            cldrStatus.setSessionId(logintoken.sessionId);
            this.loginShown = false;
            notification.success({
              placement: "topLeft",
              message: "Logged in as " + this.userName,
            });
            run(); // Restart everything
          } else if (logintoken.sessionId) {
            return errBox(
              "The server returned a session ID but was not able to login."
            );
          } else {
            return errBox("The server did not return a session ID.");
          }
        } catch (e) {
          errBox("Error:" + e.toString());
        }
      }
    },
    /**
     * Login or out (the header button)
     */
    async loginout() {
      if (this.loginShown) {
        // cancel
        this.loginShown = false;
        this.logText = "Log In";
      } else if (!this.loggedIn) {
        // Log In (show popup)
        this.loginShown = true;
        this.logText = "Cancel";
      } else {
        await this.logout();
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
