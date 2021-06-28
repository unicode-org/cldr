<template>
  <p v-if="email">{{ emailLabel }}: {{ email }}</p>

  <p v-if="helpMessage" class="helpHtml">
    {{ helpMessage }}
  </p>

  <section v-if="email && !finished">
    <p class="lockCarefully" v-html="cautionMessage"></p>
    <p v-html="instructionMessage"></p>
    <label for="sumBox">{{ sumLabel }}:&nbsp;</label
    ><input id="sumBox" v-model="sumEntered" size="10" />
    <br />
    <label for="emailBox">{{ emailLabel }}:&nbsp;</label
    ><input id="emailBox" v-model="emailEntered" type="email" size="60" />
    <br />
    <label for="lockReason">{{ reasonLabel }}:</label>
    <br />
    <textarea id="lockReason" v-model="reasonEntered" />
    <p v-if="errMessage">
      <span class="errHtml">
        {{ errMessage }}
      </span>
    </p>
    <br />
    <button
      class="cldr-nav-btn lockButton"
      @click="lockAccount"
      :disabled="!(sumEntered && reasonEntered && emailEntered)"
    >
      {{ buttonLabel }}
    </button>
    <a-spin v-if="loading" :delay="500" />
  </section>
</template>

<script>
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as cldrText from "../esm/cldrText.js";

export default {
  data() {
    return {
      loading: false,
      finished: false,
      email: null,
      helpMessage: null,
      errMessage: null,
      cautionMessage: null,
      summandA: null,
      summandB: null,
      sumCorrect: null,
      sumEntered: null,
      emailEntered: null,
      reasonEntered: null,
      sumLabel: null,
      emailLabel: null,
      reasonLabel: null,
      buttonLabel: null,
    };
  },

  created() {
    const user = cldrStatus.getSurveyUser();
    if (!user) {
      this.helpMessage = cldrText.get("lock_account_login");
    } else if (user.userlevelName === "ADMIN") {
      this.helpMessage = cldrText.get("lock_account_admin");
    } else {
      this.email = user.email;
    }
    this.summandA = Math.floor(Math.random() * 11);
    this.summandB = Math.floor(Math.random() * 11);
    this.sumCorrect = this.summandA + this.summandB;

    this.cautionMessage = cldrText.get("lock_account_caution");
    this.instructionMessage = cldrText.get("lock_account_instruction");

    this.sumLabel = cldrText.sub("lock_account_sum", [
      this.summandA,
      this.summandB,
    ]);
    this.emailLabel = cldrText.get("lock_account_email");
    this.reasonLabel = cldrText.get("lock_account_reason");
    this.buttonLabel = cldrText.get("lock_account_button");
  },

  methods: {
    lockAccount() {
      this.errMessage = null;
      if (this.sumCorrect !== parseInt(this.sumEntered)) {
        this.errMessage = cldrText.get("lock_account_err_math");
      } else if (this.email !== this.emailEntered) {
        this.errMessage = cldrText.get("lock_account_err_email");
      } else if (
        !this.reasonEntered ||
        !(this.reasonEntered = this.reasonEntered.trim())
      ) {
        this.errMessage = cldrText.get("lock_account_err_reason");
      } else {
        this.reallyLockAccount();
      }
    },

    reallyLockAccount() {
      this.loading = true;
      this.errMessage = null;
      const lockPostData = {
        email: this.emailEntered,
        reason: this.reasonEntered,
        session: cldrStatus.getSessionId(),
      };
      const xhrArgs = {
        url: cldrAjax.makeApiUrl("auth/lock", null),
        postData: lockPostData,
        handleAs: "json",
        load: (json) => this.loadResult(json),
        error: (err) => this.handleError(err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    handleError(err) {
      this.loading = false;
      this.errMessage = err;
    },

    loadResult(json) {
      this.loading = false;
      this.finished = true;
      const successMessage = cldrText.get("lock_account_success");
      this.helpMessage = successMessage;
      cldrStatus.setIsDisconnected(true);
      cldrStatus.setSurveyUser(null);
      cldrStatus.setPermissions(null);
      cldrStatus.setSessionId(null);
      // The success message will be displayed in the main window, replacing the form,
      // as soon as Vue renders it. However, it may quickly be replaced with something else
      // as a result of the disconnection, since the user has been logged out on the back end.
      // To ensure the user has a chance to read the message, also open an alert box with the
      // same message, then redirect to the default page. Ideally (though not necessarily) the
      // alert should open after Vue has done the rendering, and before other messages start
      // showing up about being disconnected, for which ten milliseconds seems to work OK.
      setTimeout(function () {
        alert(successMessage);
        window.location.replace("");
      }, 10);
    },
  },
};
</script>

<style scoped>
.lockCarefully {
  background: yellow;
  font-weight: bold;
  font-size: 200%;
}

.helpHtml {
  margin: 1em;
}

.errHtml {
  border: 3px solid red;
  font-weight: bold;
}

.lockButton:disabled {
  color: #888;
}

textarea {
  margin: 1em;
  resize: both;
}
</style>
