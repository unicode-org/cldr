<template>
  <template v-if="!surveyUser">
    <a-input
      :disabled="sent"
      placeholder="Username"
      type="email"
      v-model:value="userName"
    >
      <template #prefix>
        <span class="glyphicon glyphicon-user tip-log" />
      </template>
    </a-input>

    <a-button v-on:click="send()" v-show="!sent" v-if="userName">Send</a-button>

    <hr />
    <p v-show="!sent">Enter your email address and click Send.</p>

    <a-alert
      v-show="sent"
      type="info"
      message="Sent"
      description="If the address matches
            an account on file, you will be sent a link to login.
            Check your junk/spam mailboxes."
    >
    </a-alert>
  </template>
  <a-alert
    v-else
    closable="true"
    v-on:close="stay()"
    type="warning"
    message="Already logged in!"
    description="You are already logged in successfully."
  >
  </a-alert>
</template>

<script setup>
import { ref } from "vue";

import * as cldrStatus from "../esm/cldrStatus.mjs";
import * as cldrLoad from "../esm/cldrLoad.mjs";

const { surveyUser, sessionId } = cldrStatus.refs;
const sent = ref(false);

const userName = ref(null);

async function send() {
  cldrLoad.sendResetHash(userName.value);
  sent.value = true;
}

function stay() {
  // just reload at the top level
  window.location.replace("v#");
}
</script>
