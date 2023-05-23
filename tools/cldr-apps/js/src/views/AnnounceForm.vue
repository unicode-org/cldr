<template>
  <header>Compose announcement</header>
  <a-form
    :model="formState"
    name="basic"
    autocomplete="off"
    @finish="onFinish"
    @finishFailed="onFinishFailed"
  >
    <a-form-item
      name="orgs"
      label="Organization(s)"
      class="formItems"
      v-if="formHasAllOrgs"
    >
      <a-radio-group v-model:value="formState.orgs">
        <a-radio value="Mine" title="Only your own organization">Mine</a-radio>
        <a-radio value="TC" title="All TC organizations">TC Orgs</a-radio>
        <a-radio value="All" title="All organizations">All</a-radio>
      </a-radio-group>
    </a-form-item>

    <a-form-item name="audience" label="Audience" class="formItems">
      <a-radio-group v-model:value="formState.audience">
        <a-radio value="TC" title="TC members and Admin">≥TC</a-radio>
        <a-radio value="Managers" title="Managers, TC members, and Admin"
          >≥Managers</a-radio
        >
        <a-radio
          value="Vetters"
          title="Vetters, Managers, TC members, and Admin (excludes Guests)"
          >≥Vetters</a-radio
        >
        <a-radio
          value="Everyone"
          title="All users with Survey Tool accounts (except locked)"
          >Everyone</a-radio
        >
      </a-radio-group>
    </a-form-item>

    <a-form-item class="formItems" label="Locales" name="locs">
      <a-input
        v-model:value="formState.locs"
        placeholder="Optional list of locales (like: aa fr zh) (fr implies fr_CA/etc.) (empty for all locales)"
        @blur="validateLocales()"
      />
    </a-form-item>
    <a-form-item
      class="formItems"
      label="Subject"
      name="subject"
      :rules="[{ required: true, message: 'Please enter a subject!' }]"
    >
      <a-input v-model:value="formState.subject" />
    </a-form-item>
    <a-form-item
      class="formItems"
      label="Body"
      name="body"
      :rules="[{ required: true, message: 'Please enter a message body!' }]"
    >
      <a-textarea
        v-model:value="formState.body"
        placeholder="Enter message (plain text) here..."
        :rows="4"
      />
    </a-form-item>
    <p class="guidance">
      {{ whatWillHappen() }}
    </p>
    <div class="buttons">
      <a-form-item>
        <a-button html-type="cancel" @click="onCancel">Cancel</a-button>
        &nbsp;
        <a-button type="primary" html-type="submit" @click="onPost"
          >Post</a-button
        >
      </a-form-item>
    </div>
  </a-form>
</template>

<script>
import * as cldrAnnounce from "../esm/cldrAnnounce.mjs";
import { defineComponent, reactive } from "vue";

export default defineComponent({
  props: ["formHasAllOrgs", "postOrCancel"],

  setup() {
    const formState = reactive({
      audience: "TC",
      body: "",
      locs: "",
      orgs: "Mine",
      subject: "",
    });

    const onFinish = (values) => {
      console.log("Successful validation:", values);
    };

    const onFinishFailed = (errorInfo) => {
      console.log("Failed validation:", errorInfo);
    };

    return {
      formState,
      onFinish,
      onFinishFailed,
    };
  },

  methods: {
    onCancel() {
      this.postOrCancel(null);
    },

    onPost() {
      // onFinish or onFinishFailed should be called for validation.
      // Double-check that subject and body aren't empty.
      if (this.formState.subject && this.formState.body) {
        this.postOrCancel(this.formState);
      }
    },

    whatWillHappen() {
      return (
        "You are about to post an announcement to " +
        this.describeAudience() +
        ", at " +
        this.describeOrgs() +
        ", in " +
        this.describeLocs() +
        "."
      );
    },

    describeAudience() {
      switch (this.formState.audience) {
        case "TC":
          return "TC members and Admin";
        case "Managers":
          return "Managers, TC members, and Admin";
        case "Vetters":
          return "Vetters, Managers, TC members, and Admin (excludes Guests)";
        case "Everyone":
          return "all users with Survey Tool accounts (except locked)";
        default:
          return "?";
      }
    },

    describeOrgs() {
      switch (this.formState.orgs) {
        case "All":
          return "all organizations";
        case "TC":
          return "TC organizations";
        case "Mine":
          return "your organization only";
        default:
          return "?";
      }
    },

    describeLocs() {
      return this.formState.locs === "" || this.formState.locs === "*"
        ? "all locales"
        : "the following locale(s): " + this.formState.locs;
    },

    validateLocales() {
      cldrAnnounce.combineAndValidateLocales(
        this.formState.locs,
        this.updateValidatedLocales
      );
    },

    updateValidatedLocales(locs, messages) {
      this.formState.locs = locs;
      if (messages) {
        for (let key of Object.keys(messages)) {
          console.log("Validating locales: " + key + " -- " + messages[key]);
        }
      }
    },
  },
});
</script>

<style scoped>
label {
  font-weight: normal;
  margin: 0; /* override bootcamp */
}

header {
  font-size: larger;
  font-weight: bold;
  margin-bottom: 1em;
}

.buttons {
  display: flex;
  justify-content: flex-end;
}

.formItems {
  margin: 1ex;
  padding: 0;
}

.guidance {
  font-style: italic;
  display: flex;
  justify-content: flex-end;
}
</style>
