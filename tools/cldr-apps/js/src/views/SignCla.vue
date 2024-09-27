<template>
  <div>
    <a-alert
      v-if="needCla"
      type="warning"
      message="Please read and sign the below CLA to begin contributing."
      show-icon
    />
    <a-alert
      v-else-if="readonlyCla"
      type="info"
      message="Your organization has signed the CLA, it may not be modified."
      show-icon
    />
    <a-alert
      v-else
      type="success"
      message="The CLA has been signed"
      show-icon
    />

    <!-- CLA text -->
    <a-spin v-if="loading" />
    <div class="cla" v-html="claHtml" />
    <hr />
    <div>
      Your Name:
      <a-input :disabled="!needCla" v-model:value="userName">
        <template #prefix>
          <i class="glyphicon glyphicon-user" />
        </template>
      </a-input>
      Your Email:
      <a-input :disabled="!needCla" v-model:value="userEmail">
        <template #prefix>
          <i class="glyphicon glyphicon-envelope" />
        </template>
      </a-input>
      Your Employer (or 'none'):<a-input
        :disabled="!needCla"
        v-model:value="userEmployer"
      >
        <template #prefix>
          <i class="glyphicon glyphicon-briefcase" />
        </template>
      </a-input>
      <i class="pleaseChoose" v-if="needCla && userSign == 0"
        >Please choose one:</i
      ><br />
      <a-radio-group :disabled="!needCla" v-model:value="userSign">
        <a-radio :style="radioStyle" :value="1">
          My employer has already signed the CLA and is listed on Unicode’s
          <a href="https://www.unicode.org/policies/corporate-cla-list/"
            >List of Corporate CLAs</a
          >.
        </a-radio>
        <a-radio :style="radioStyle" :value="2">
          I am a self-employed or unemployed individual and I have read and
          agree to the CLA.
        </a-radio>
      </a-radio-group>
      <br />
      <button
        @click="sign"
        v-if="
          needCla &&
          userSign != 0 &&
          userName &&
          userEmail &&
          userEmployer &&
          !readonlyCla
        "
      >
        Sign
      </button>
      <a-alert
        type="info"
        v-else-if="needCla"
        message="Please fill in the above fields."
      />
      <hr />
    </div>
    <div v-if="!needCla">
      <a-alert
        v-if="!readonlyCla"
        type="warning"
        message="If the CLA is revoked, you will not be able to continue contributing to CLDR. You will have an opportunity to re-sign if corrections are needed."
      />
      <button v-if="!readonlyCla" @click="revoke">Revoke</button>
    </div>
    <a-spin v-if="loading" />
  </div>
</template>

<script setup>
import * as cldrCla from "../esm/cldrCla.mjs";
import { marked } from "../esm/cldrMarked.mjs";
import * as cldrNotify from "../esm/cldrNotify.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

import { ref } from "vue";
import claMd from "../md/cla.md";

const claHtml = marked(claMd);

const user = cldrStatus.getSurveyUser();

if (!user) window.location.replace("#///"); // need a user

let readonlyCla = ref(false);
let needCla = ref(!user?.claSigned);
let loading = ref(false);
const userName = ref(user?.name);
const userEmail = ref(user?.email);
let userEmployer = ref(user?.org);
let userSign = ref(0);
const radioStyle = {
  display: "flex",
};

async function loadData() {
  if (!user) return;
  loading.value = true;
  const { corporate, email, employer, name, readonly, unauthorized, signed } =
    await cldrCla.getCla();
  loading.value = false;
  if (unauthorized || !signed) return;
  readonlyCla.value = readonly;
  userName.value = name;
  userEmail.value = email;
  userEmployer.value = employer;
  userSign.value = corporate ? 1 : 2;
}

// load the existing signing data
loadData().then(
  () => {},
  (err) => cldrNotify.exception(err, "Loading prior CLA data")
);

async function sign() {
  loading.value = true;
  try {
    await cldrCla.signCla({
      email: userEmail.value, // unwrap refs
      name: userName.value,
      employer: userEmployer.value,
      corporate: userSign == 1,
    });
    user.claSigned = true; // update global user obj
    needCla.value = false;
    cldrNotify.open(
      `CLA Signed`,
      `The CLA has been signed. You may now contribute to the SurveyTool!`,
      () => (window.location.hash = "#///")
    );
  } catch (e) {
    if (e.statusCode === 423) {
      cldrNotify.error(
        "CLA Sign error",
        "Your organization has already signed the CLA, it cannot be modified."
      );
    } else if (e.statusCode == 406) {
      cldrNotify.error(
        "CLA Error",
        `Double check your input fields, the CLA could not be signed.`
      );
    } else {
      cldrNotify.exception(
        e,
        `${e.statusCode || "error"} trying to sign the CLA.`
      );
    }
  }
  loading.value = false;
}

async function revoke() {
  loading.value = true;
  try {
    await cldrCla.revokeCla();
    needCla.value = true;
    user.claSigned = false; // update global user obj
    cldrNotify.open(`CLA Revoked`, `The CLA has been revoked.`);
  } catch (e) {
    if (e.statusCode === 423) {
      cldrNotify.error(
        "CLA Revoke error",
        "Your organization signed the CLA, it cannot be modified."
      );
    } else if (e.statusCode === 404) {
      cldrNotify.error("CLA Revoke error", "You have not signed the CLA.");
      needCla.value = true;
      user.claSigned = false; // update global user obj
    } else {
      cldrNotify.exception(
        e,
        `${e.statusCode || "error"} trying to revoke a CLA.`
      );
    }
  }
  loading.value = false;
}
</script>

<style scoped>
.cla {
  padding: 1em;
  border: 1px solid gray;
  margin: 0.5em;
  background-color: bisque;
  font-size: small;
}
</style>
