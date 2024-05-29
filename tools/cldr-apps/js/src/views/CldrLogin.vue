<template>
  <!-- if the user closes the alert, just go back to the ST homepage-->
  <a-alert
    closable="true"
    v-on:close="stay()"
    v-show="loginMessage"
    type="info"
    :message="loginMessage"
  />

  <!-- show user login -->
  <a-card hoverable v-if="jwtGood">
    <cldr-user v-if="jwtGood && loginId" v-bind:uid="loginId" />
    <p>You are now logged in via the email link.</p>
    <a-button v-on:click="stay()">Keep me logged in.</a-button>
  </a-card>
</template>

<script setup>
/**
 * This page is for extended login requirements, such as
 * logging in from an emailed jwt link.
 */
import { ref, onMounted } from "vue";

import * as cldrLoad from "../esm/cldrLoad.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

const { surveyUser, sessionId, currentPieces } = cldrStatus.refs;

const jwtHash = ref(cldrStatus.getCurrentPieces()[3]); // get the JWT hash which was stored from cldrLoad

const loginMessage = ref("");
const jwtGood = ref(false);

const loginName = ref(null);
const loginEmail = ref(null);
const loginId = ref(0);

onMounted(async () => {
  if (jwtHash.value) {
    loginMessage.value = "Logging in..";
    await doLogin();
  } else {
    loginMessage.value =
      "No login hash found. Please make sure to click on the login link in email.";
  }
});

async function doLogin() {
  jwtGood.value = false;
  loginMessage.value = `Logging in to ${jwtHash.value}`;
  const { sessionId, user, name, email, id } = await cldrLoad.loginWithJwt(
    jwtHash.value
  );
  // OK, we are now logged in.
  if (!user || !sessionId) {
    loginMessage.value = "Could not log in with that link.";
    return;
  }
  jwtGood.value = true; // Give the users their next options.
  loginName.value = name;
  loginEmail.value = email;
  loginId.value = id;
  loginMessage.value = "Logged in!";
  // do NOT set any properties here, or the page will reload.
}

function stay() {
  // just reload at the top level
  window.location.replace("v#");
}
</script>
