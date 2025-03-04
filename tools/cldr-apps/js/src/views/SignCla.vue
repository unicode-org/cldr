<template>
  <div class="clapage">
    <a-spin v-if="loading" :delay="500" />

    <a-card>
      Hello. We need your permission to use the data and information that you
      submit to the Unicode CLDR Survey Tool. To do this, we need a contributor
      license agreement (CLA) on file for you or your employer so that Unicode
      has the required permissions to use your contributions in our products and
      services. If you are seeing this message, it means we do not have such a
      CLA on file. If you believe you are already under a Unicode CLA (either
      your own or your employer’s) and are seeing this message in error, or if
      you have further questions, please contact
      <a href="mailto:cldr-cla@unicode.org">cldr-cla@unicode.org</a>
    </a-card>

    <!-- First, an exception case: logged in with GitHub but the CLA isn't signed! -->
    <template
      v-if="
        !loading && needCla && githubLoginUrl && !skipGithub && githubSessionId
      "
    >
      <h3>CLA Signature Not Found</h3>

      <p>
        Your GitHub account, @{{ githubSessionId }}, was not detected as signing
        the
        <a href="https://cla-assistant.io/unicode-org/.github">Unicode CLA</a>.
      </p>

      <p>
        Note that it is possible that you have signed, but that our records are
        out of date.
      </p>
      <a-row>
        <a-col :span="12">
          <a-button :href="githubReloginUrl"
            >Sign in with a different GitHub account</a-button
          >
        </a-col>
        <a-col :span="12">
          <a-button href="https://cla-assistant.io/unicode-org/.github"
            >Sign the Unicode CLA</a-button
          >
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-button href="mailto:cldr-cla@unicode.org">Contact Us</a-button>
        </a-col>
        <a-col :span="12">
          <a-button @click="doSkipGitHub"
            >Sign the Survey Tool-only CLA</a-button
          >
        </a-col>
      </a-row>
    </template>

    <!-- try GitHub first: This is the FIRST panel people will see -->
    <template v-else-if="!loading && needCla && githubLoginUrl && !skipGithub">
      <a-row>
        <a-col :span="12">
          <p>
            Have you already
            <a href="https://cla-assistant.io/unicode-org/.github"
              >signed the Unicode CLA</a
            >
            with your GitHub account? <br />
            If so, simply click this button so we can check it:
          </p>
          <a-button :href="githubLoginUrl"> Log in with GitHub</a-button>
        </a-col>
        <a-col :span="12">
          <p>
            If you haven't signed the Unicode CLA,
            <br />Click this button instead:
          </p>
          <a-button @click="doSkipGitHub">Sign Survey Tool only CLA</a-button>
        </a-col>
      </a-row>
    </template>

    <!-- "Manual" sign (or successful sign) - this is the "details" page. -->
    <template v-else-if="!loading">
      <a-alert
        v-if="needCla"
        type="warning"
        message="Please read and sign the below CLA to begin contributing."
        show-icon
      />
      <a-alert
        v-else-if="readonlyCla"
        type="info"
        message="Your organization has signed a Unicode Corporate CLA."
        show-icon
      />
      <a-alert
        v-else-if="userGithubSign"
        type="success"
        :message="
          'The Unicode CLA was signed as the GitHub user @' + userGithubSign
        "
        show-icon
      />
      <a-alert
        v-else
        type="success"
        message="The CLA has been signed"
        show-icon
      />

      <!-- CLA text. Hide it if a github or corp cla -->
      <div
        v-if="!userGithubSign && !readonlyCla"
        class="cla"
        v-html="claHtml"
      />

      <br />

      <a-form
        name="claform"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 19 }"
        autocomplete="off"
      >
        <a-form-item label="Your Name">
          <a-input :disabled="!needCla" v-model:value="userName">
            <template #prefix>
              <i class="glyphicon glyphicon-user" />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item label="Your Email">
          <a-input :disabled="!needCla" v-model:value="userEmail">
            <template #prefix>
              <i class="glyphicon glyphicon-envelope" />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item label="Your Employer (or 'none')">
          <a-input :disabled="!needCla" v-model:value="userEmployer">
            <template #prefix>
              <i class="glyphicon glyphicon-briefcase" />
            </template>
          </a-input>
        </a-form-item>

        <!-- non form item -->
        <i class="pleaseChoose" v-if="needCla && userSign == 0"
          >Please check the applicable box below (only one) to indicate your
          agreement.</i
        >

        <p v-if="userGithubSign">
          To view the CLA signed via GitHub, click
          <a href="https://cla-assistant.io/unicode-org/.github">here</a> and
          choose the Login with GitHub button at the bottom.
        </p>

        <!-- don't show the radio button for github or corp -->
        <a-form-item v-if="!userGithubSign && !readonlyCla">
          <a-radio-group :disabled="!needCla" v-model:value="userSign">
            <a-radio :style="radioStyle" :value="RADIO_ASSERT_INDIVIDUAL">
              I am contributing as an individual because I am self-employed or
              unemployed. I have read and agree to the foregoing terms.
            </a-radio>
            <a-radio
              :style="radioStyle"
              :value="RADIO_ASSERT_EMPLOYER_NORIGHTS"
            >
              I am contributing as an individual because, even though I am
              employed, my employer has no rights and claims no rights to my
              contributions. I have read and agree to the foregoing terms.
            </a-radio>
            <a-radio :style="radioStyle" :value="RADIO_ASSERT_EMPLOYER_RIGHTS">
              I am employed and my employer has or may have rights in my
              contributions under my employment agreement and/or the work for
              hire doctrine or similar legal principles.
            </a-radio>
            <a-radio
              v-if="false"
              :style="radioStyle"
              :value="RADIO_ASSERT_CORP"
            >
              My employer has already signed the CLA and is listed on Unicode’s
              <a href="https://www.unicode.org/policies/corporate-cla-list/"
                >List of Corporate CLAs</a
              >.
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <button
            @click="sign"
            v-if="
              needCla &&
              userSign != RADIO_UNSET &&
              userSign != RADIO_ASSERT_EMPLOYER_RIGHTS &&
              userName &&
              userEmail &&
              userEmployer &&
              !readonlyCla
            "
          >
            Sign
          </button>
          <div v-else-if="userSign == RADIO_ASSERT_EMPLOYER_RIGHTS">
            <a-alert
              type="error"
              message="Please request that your employer sign the Unicode Corporate CLA."
            />
            &nbsp;<a
              href="https://www.unicode.org/policies/licensing_policy.html#signing"
              >How to sign the Corporate CLA…</a
            >
          </div>
          <a-alert
            type="info"
            v-else-if="needCla"
            message="Please fill in the above fields."
          />
        </a-form-item>
      </a-form>
      <div v-if="false && !needCla">
        <hr />
        <!-- revocation is not allowed at present -->
        <a-alert
          v-if="!readonlyCla"
          type="warning"
          message="If the CLA is revoked, you will not be able to continue contributing to CLDR. You will have an opportunity to re-sign if corrections are needed."
        />
        <button v-if="!readonlyCla" @click="revoke">Revoke</button>
      </div>
      <a-button v-if="skipGithub" @click="doUnskipGitHub"
        >Try logging in with GitHub instead</a-button
      >
    </template>

    <hr />
    <p>
      If you would like further information regarding contributing to Unicode,
      please see our
      <a
        target="_blank"
        href="https://www.unicode.org/policies/licensing_policy.html"
        >IP Policies</a
      >.
    </p>
  </div>
</template>

<script setup>
import * as cldrAuth from "../esm/cldrAuth.mjs";
import * as cldrCla from "../esm/cldrCla.mjs";
import { marked } from "../esm/cldrMarked.mjs";
import * as cldrNotify from "../esm/cldrNotify.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";

import { ref } from "vue";
import claMd from "../md/cla.md";

const RADIO_UNSET = 0;
const RADIO_ASSERT_CORP = 1;
const RADIO_ASSERT_EMPLOYER_RIGHTS = 4; // not allowed for signing.
const RADIO_ASSERT_EMPLOYER_NORIGHTS = 3;
const RADIO_ASSERT_INDIVIDUAL = 2;

const claHtml = marked(claMd);

const user = cldrStatus.getSurveyUser();

if (!user) window.location.replace("#///"); // need a user

let readonlyCla = ref(false);
let needCla = ref(!user?.claSigned);
let loading = ref(true);
const userName = ref(user?.name);
const userEmail = ref(user?.email);
let userEmployer = ref(user?.org);
let userSign = ref(0);
/** if set, the Github ID of a successful CLA sign */
let userGithubSign = ref(null);
/** if set, the URL to login with Github. */
let githubLoginUrl = ref(null);
/** if set, URL to re-login with Github */
let githubReloginUrl = ref(null);
/** if set, the Github ID of the current session ID (may not be on CLA list) */
let githubSessionId = ref(null);
/** if true, user has clicked "manual sign" */
let skipGithub = ref(false);

const radioStyle = {
  display: "flex",
  "padding-bottom": "1em",
};

async function loadData() {
  if (!user) {
    loading.value = false;
    return;
  }
  const githubLoginLoad = cldrAuth.getLoginUrl({
    intent: "cla",
    service: "github",
  });
  const githubReloginLoad = cldrAuth.getLoginUrl({
    intent: "cla",
    service: "github",
    relogin: true,
  });
  const githubSessionLoad = cldrAuth.getGithubIdFromSession();
  const {
    corporate,
    noRights,
    email,
    employer,
    name,
    readonly,
    unauthorized,
    signed,
    github,
  } = await cldrCla.getCla();

  try {
    githubLoginUrl.value = await githubLoginLoad;
    githubReloginUrl.value = await githubReloginLoad;
  } catch (e) {
    console.error("could not getLoginUrl", e);
  }
  try {
    githubSessionId.value = await githubSessionLoad;
  } catch (e) {
    console.error("could not getGithubIdFromSession", e);
  }

  loading.value = false;
  if (unauthorized || !signed) return;
  readonlyCla.value = readonly;
  userName.value = name;
  userEmail.value = email;
  userEmployer.value = employer;
  if (corporate) {
    userSign.value = RADIO_ASSERT_CORP;
  } else if (noRights) {
    userSign.value = RADIO_ASSERT_NORIGHTS;
  } else {
    userSign.value = RADIO_ASSERT_INDIVIDUAL;
  }
  userGithubSign = github;
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
      corporate: userSign == RADIO_ASSERT_CORP,
      noRights: userSign == RADIO_ASSERT_EMPLOYER_NORIGHTS,
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

function doSkipGitHub() {
  skipGithub.value = true;
}
function doUnskipGitHub() {
  skipGithub.value = false;
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

.clapage {
  padding-left: 2em;
  padding-right: 2em;
}
</style>
