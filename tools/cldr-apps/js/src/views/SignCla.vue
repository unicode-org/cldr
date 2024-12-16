<template>
  <div class="clapage">
    <a-spin v-if="loading" :delay="500" />

    <!-- try GitHub first -->
    <template
      v-if="
        !loading && needCla && githubLoginUrl && !skipGithub && githubSessionId
      "
    >
      <p>
        .. we see you are github user @{{ githubSessionId }} .. BUT did not
        detect CLA
      </p>
      <p>
        You can Sign the CLA by
        <a href="https://cla-assistant.io/unicode-org/.github">Clicking here</a>
      </p>
      <p>
        Don't have a GitHub account or want to sign the CLA manually?
        <a-button @click="doSkipGitHub">Sign Manually</a-button>
      </p>
      <p>
        If this is the wrong account, you can
        <a :href="githubReloginUrl">Login w/ Github</a> to login with a
        different id.
      </p>
    </template>
    <template v-else-if="!loading && needCla && githubLoginUrl && !skipGithub">
      <p>
        ... offer to sign w/ github ..
        <a :href="githubLoginUrl">Login w/ Github</a>
      </p>
      <p>If you dont have a github login you can:</p>
      <ol>
        <li>
          Sign up at
          <a href="https://github.com/signup">https://github.com/signup</a>
        </li>
        <li>
          Sign the CLA by
          <a href="https://cla-assistant.io/unicode-org/.github"
            >Clicking here</a
          >
        </li>
        <li>It will take us some time</li>
      </ol>
    </template>

    <!-- "manual" sign -->
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
        message="Your organization has signed the CLA, it may not be modified."
        show-icon
      />
      <a-alert
        v-else-if="userGithubSign"
        type="success"
        :message="'The CLA was signed as the GitHub user @' + userGithubSign"
        show-icon
      />
      <a-alert
        v-else
        type="success"
        message="The CLA has been signed"
        show-icon
      />

      <!-- CLA text -->
      <div class="cla" v-html="claHtml" />

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

        <a-form-item>
          <a-radio-group :disabled="!needCla" v-model:value="userSign">
            <a-radio :style="radioStyle" :value="2">
              I am contributing as an individual because I am self-employed or
              unemployed. I have read and agree to the foregoing terms.
            </a-radio>
            <a-radio :style="radioStyle" :value="3">
              I am contributing as an individual because, even though I am
              employed, my employer has no rights and claims no rights to my
              contributions. I have read and agree to the foregoing terms.
            </a-radio>
            <a-radio :style="radioStyle" :value="4">
              I am employed and my employer has or may have rights in my
              contributions under my employment agreement and/or the work for
              hire doctrine or similar legal principles.
            </a-radio>
            <a-radio v-if="false" :style="radioStyle" :value="1">
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
              userSign != 0 &&
              userSign != 4 &&
              userName &&
              userEmail &&
              userEmployer &&
              !readonlyCla
            "
          >
            Sign
          </button>
          <div v-else-if="userSign == 4">
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
      <hr />
      <div v-if="false && !needCla">
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
  userSign.value = corporate ? 1 : 2;
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
