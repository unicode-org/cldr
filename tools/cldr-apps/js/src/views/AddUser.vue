<template>
  <article>
    <a-spin v-if="loading" :delay="500" />
    <div v-if="!hasPermission">
      Please log in as a user with sufficient permissions.
    </div>
    <div v-if="errorsExist()">
      <span class="addUserErrors">Please correct the following error(s):</span>
      <ul>
        <li v-for="error in getErrors()" :key="error">{{ error }}</li>
      </ul>
    </div>
    <div v-if="hasPermission && !loading && !addedNewUser" class="adduser">
      <h2>Add User</h2>
      <table>
        <tr>
          <th><label for="new_org">Organization:</label></th>
          <td v-if="canChooseOrg">
            <a-select
              id="new_org"
              v-model:value="orgValueAndLabel"
              label-in-value
              show-search
              style="width: 100%"
              placeholder="Select an organization"
              :options="orgOptions"
              @change="getOrgLocalesAfterChoosingOrg"
            >
              <template #option="{ label }">
                {{ label }}
              </template>
            </a-select>
          </td>
          <td v-else>
            <input id="new_org" disabled="disabled" v-model="newUserOrg" />
          </td>
        </tr>
        <tr>
          <th><label for="new_name">Name:</label></th>
          <td>
            <a-input
              id="new_name"
              size="40"
              v-model:value="newUserName"
              placeholder="Enter a user name"
            />
          </td>
        </tr>
        <tr>
          <th><label for="new_email">E-mail:</label></th>
          <td>
            <a-input
              id="new_email"
              size="40"
              v-model:value="newUserEmail"
              type="email"
              placeholder="Enter an e-mail address"
              @blur="validateEmail(newUserEmail)"
            />
          </td>
        </tr>
        <tr>
          <th><label for="new_level">User level:</label></th>
          <td>
            <a-select
              id="new_level"
              v-model:value="newUserLevel"
              style="width: 100%"
              placeholder="Select a user level"
            >
              <a-select-option
                v-for="(v, number) in levelList"
                v-bind:value="number"
                :disabled="!v.canCreateOrSetLevelTo"
                :key="number"
              >
                {{ v.string }}
              </a-select-option>
            </a-select>
          </td>
        </tr>
        <template v-if="localesAreRequired()">
          <tr>
            <th><label for="radio_head"> </label></th>
            <td>
              <a-radio-group id="radio_head" v-model:value="allLocales">
                <a-radio :value="true">All locales</a-radio>
                <a-radio :value="false">Specific locales</a-radio>
              </a-radio-group>
            </td>
          </tr>
          <tr v-if="!allLocales">
            <th><label for="new_locales">Locales:</label></th>
            <td>
              <a-select
                id="new_locales"
                v-model:value="chosenLocales"
                mode="multiple"
                show-search
                style="width: 100%"
                placeholder="Select locale(s)"
                :options="localeOptions"
                :max-tag-count="10"
                @change="validateLocales"
              >
                <!-- This appears in the menu: -->
                <template #option="{ localeDescription }">
                  {{ localeDescription }}
                </template>
                <!-- This appears in the input box after choosing from menu: -->
                <template #tagRender="{ closable, onClose, option }">
                  <a-tag
                    :closable="closable"
                    style="margin-right: 3px"
                    @close="onClose"
                  >
                    <span :title="option.localeDescription || x">{{
                      option.value
                    }}</span>
                  </a-tag>
                </template>
              </a-select>
              <div v-if="Object.keys(locWarnings).length">
                <span class="locWarnings"
                  >The following locales will not be added due to
                  problems:</span
                >
                <ul>
                  <li v-bind:key="loc" v-for="loc in Object.keys(locWarnings)">
                    <code>{{ loc }}</code>
                    {{ getParenthesizedName(loc) }}
                    — {{ explainWarning(locWarnings[loc]) }}
                  </li>
                </ul>
              </div>
            </td>
          </tr>
        </template>

        <tr class="addButton">
          <td colspan="2">
            <button v-if="readyToAdd()" v-on:click="add()">Add</button>
          </td>
        </tr>
      </table>
    </div>

    <div v-if="addedNewUser">
      <h2>Added User</h2>
      <p>
        ✅ The new user was added. Name: <kbd>{{ newUserName }}</kbd> E-mail:
        <kbd>{{ newUserEmail }}</kbd> ID:
        <kbd>{{ userId }}</kbd>
      </p>
      <p>
        ⚠️ <em>The password is not sent to the user automatically!</em> You must
        click the <em>Manage this user</em> button and choose
        <em>Send password</em> from the menu.
      </p>
      <p>
        <button class="manageButton" v-on:click="manageThisUser()">
          Manage this user
        </button>
      </p>
      <hr />
      <p>
        <button v-on:click="startAddingAnotherUser()">Add another user</button>
      </p>
    </div>
  </article>
</template>

<script setup>
import * as cldrAddUser from "../esm/cldrAddUser.mjs";
import * as cldrText from "../esm/cldrText.mjs";

import { onMounted, ref, reactive } from "vue";

const DEBUG = false;

// Must be false for production! Test for locWarnings if the client and server disagree
const TEST_LOC_WARNINGS = false;

const VETTER_LEVEL_NUMBER = 5;

// Numbers
const userId = ref(0);

// Booleans
const loading = ref(true);
const hasPermission = ref(false);
const addedNewUser = ref(false);
const canChooseOrg = ref(false);
const allLocales = ref(false);
const justGotError = ref(false);

// Strings
const newUserEmail = ref("");
const newUserLevel = ref("");
const newUserName = ref("");
const newUserOrg = ref("");

// Arrays

// Often for arrays, reactive() seems to work better than ref(),
// but for a-select menus, ref() sometimes seems to be required.
// For documentation of a-select, see https://www.antdv.com/components/select/
const orgValueAndLabel = ref([]);
const orgOptions = ref([]);
const chosenLocales = ref([]);
const localeOptions = ref([]);

// Variables assigned with reactive() must be handled differently
// from those assigned with ref().
// For example, they are not referenced using .value
let locWarnings = reactive([]);
let levelList = reactive([]);

onMounted(mounted);

function mounted() {
  startAddingAnotherUser();
  hasPermission.value = Boolean(cldrAddUser.hasPermission());
}

function startAddingAnotherUser() {
  initializeData();
  cldrAddUser.viewMounted(setData);
}

function initializeData() {
  // Numbers
  userId.value = 0;

  // Booleans
  loading.value = true;
  addedNewUser.value = false;
  canChooseOrg.value = false;
  allLocales.value = false;

  // Strings
  newUserEmail.value = "";
  newUserLevel.value = "";
  newUserName.value = "";
  newUserOrg.value = "";

  // Arrays (see comment above about ref()/.value vs reactive())

  orgValueAndLabel.value = [];
  orgOptions.value = [];
  chosenLocales.value = [];
  localeOptions.value = [];

  locWarnings = reactive([]);
  levelList = reactive([]);
}

function setData(data) {
  if (data.levelList) {
    levelList = reactive(data.levelList);
  }
  if (data.orgObject) {
    setOrgData(data.orgObject);
  }
  if (data.orgName && data.orgLocales) {
    setOrgLocales(data.orgName, data.orgLocales);
  }
  if (data.error) {
    justGotError.value = true; // See comment in errorsExist()
  }
  if (data.validatedLocales) {
    setValidatedLocales(data.validatedLocales);
  }
  if (data.newUser) {
    setNewUserData(data.newUser);
  }
  areWeLoading();
}

function setNewUserData(newUser) {
  addedNewUser.value = true;
  userId.value = newUser.id;
  newUserEmail.value = newUser.email;
}

function setOrgData(orgObject) {
  // orgObject has two maps (displayToShort, shortToDisplay) and one array (sortedDisplayNames).
  // orgObject = { displayToShort, shortToDisplay, sortedDisplayNames }
  const array = [];
  for (let orgDisplayName of orgObject.sortedDisplayNames) {
    const orgShortName = orgObject.displayToShort[orgDisplayName];
    const item = {
      // The key must be "value" for the menu to work right.
      value: orgShortName,
      // The key must be "label" (not, e.g., "orgDescription") in order to use label-in-value,
      // which enables the displayed chosen value to include the display name.
      label: orgDisplayName + " = " + orgShortName,
    };
    array.push(item);
  }
  orgOptions.value = array;
  canChooseOrg.value = true;
  newUserOrg.value = "";
}

function setOrgLocales(orgName, orgLocales) {
  newUserOrg.value = orgName;
  if (!orgLocales) {
    console.error("No locales for organization " + newUserOrg.value);
  }
  const array = [];
  for (let localeId of orgLocales.split(" ")) {
    const localeName = cldrAddUser.getLocaleName(localeId);
    const item = {
      // A key other than "value" here, such as "localeIdValue", would be more descriptive,
      // but does not appear to work.
      value: localeId,
      localeDescription: localeName + " = " + localeId,
    };
    array.push(item);
  }
  if (TEST_LOC_WARNINGS) {
    array.push({
      value: "bogus",
      localeDescription: "Bogus International = bogus",
    });
    // Afar = aa is valid but missing from most organizations
    array.push({
      value: "aa",
      localeDescription: "Afar = aa",
    });
  }
  localeOptions.value = array;
}

/**
 * Replace the set of chosen locales with a validated set retrieved from the server.
 * This has the side effect of putting the locale IDs in alphabetical order.
 * However, it only happens if the server found and removed one or more invalid locales.
 *
 * @param validatedLocales the space-separated set of locale IDs
 */
function setValidatedLocales(validatedLocales) {
  locWarnings = reactive(validatedLocales.locWarnings);
  chosenLocales.value = validatedLocales.newUserLocales.split(" ");
}

function getOrgLocalesAfterChoosingOrg() {
  // First clear the chosen locales, to prevent problems if user choose an org, then chooses
  // locales, then chooses a different org that doesn't have those locales.
  chosenLocales.value = [];

  // label-in-value causes the selected org to be an object { value: ..., label: ... },
  // so we need to extract the value (short org name).
  // The "value" in newUserOrg.value is for Vue ref().
  // The first "value" in orgValueAndLabel.value.value is for Vue ref().
  // The second "value" in orgValueAndLabel.value.value is a key in { value: ..., label: ... }.
  newUserOrg.value = orgValueAndLabel.value.value;
  cldrAddUser.getOrgLocales(newUserOrg.value);
}

async function validateLocales() {
  if (allLocales.value === true) {
    return;
  }
  if (chosenLocales.value.length === 0) {
    return;
  }
  // Validation is almost superfluous now that locales are selected from a menu,
  // rather than input by typing.
  cldrAddUser.validateLocales(
    newUserOrg.value,
    joinChosenLocales(),
    newUserLevel.value,
    levelList
  );
}

function getParenthesizedName(loc) {
  const name = cldrAddUser.getLocaleName(loc);
  if (name && name !== loc) {
    return `(${name})`;
  }
  return "";
}

function explainWarning(reason) {
  return cldrText.get(`locale_rejection_${reason}`, reason);
}

/**
 * Check whether to show a spinner indicating that we're waiting for server responses
 * in order to load the page with data.
 * Set loading = true to show a spinner, loading = false otherwise.
 */
function areWeLoading() {
  // Note: given levelList = reactive([..., ...]), Vue does not support getting
  // levelList.length directly. Use Object.keys(levelList).length instead.
  loading.value = !(
    Object.keys(levelList).length &&
    (Object.keys(orgOptions).length || newUserOrg.value)
  );
  if (DEBUG) {
    console.log(
      "areWeLoading: loading.value = " +
        loading.value +
        "; Object.keys(levelList).length = " +
        Object.keys(levelList).length +
        "; Object.keys(orgOptions).length = " +
        Object.keys(orgOptions).length +
        "; newUserOrg.value = " +
        newUserOrg.value
    );
  }
}

function readyToAdd() {
  return (
    // It could make sense to require errorsExist() === false here; however, currently this
    // is not done, since some errors are detected on the back end, and the only way to
    // determine whether they are fixed is to submit with the Add button.
    newUserOrg.value &&
    newUserName.value &&
    newUserEmail.value &&
    newUserLevel.value &&
    (chosenLocales.value.length > 0 || allLocales.value === true)
  );
}

async function add() {
  validate();
  await validateLocales();
  if (!readyToAdd()) {
    if (DEBUG) {
      console.log("Early return from add() since readyToAdd() returned false");
    }
    return;
  }
  if (errorsExist()) {
    // Errors are cleared at the start of validate(), so if errorsExist() here, they
    // were detected on the front end, not the back end.
    if (DEBUG) {
      console.log("Early return from add() since errorsExist() returned true");
    }
    return;
  }
  const postData = {
    email: newUserEmail.value,
    level: newUserLevel.value,
    locales: joinChosenLocales(),
    name: newUserName.value,
    org: newUserOrg.value,
  };
  cldrAddUser.add(postData);
}

function joinChosenLocales() {
  return allLocales.value === true
    ? cldrAddUser.ALL_LOCALES
    : chosenLocales.value.join(" ");
}

function validate() {
  cldrAddUser.clearErrors();
  if (!newUserOrg.value) {
    addError("Organization required.");
  }
  if (!newUserName.value) {
    addError("Name required.");
  }
  if (!newUserEmail.value) {
    addError("E-mail required.");
  } else {
    validateEmail(newUserEmail.value);
  }
  if (!newUserLevel.value) {
    addError("Level required.");
  } else if (
    localesAreRequired() &&
    allLocales.value === false &&
    chosenLocales.value.length === 0
  ) {
    addError("Locales is required for this userlevel.");
  }
}

function addError(message) {
  cldrAddUser.addError(message);
}

function removeError(message) {
  cldrAddUser.removeError(message);
}

function errorsExist() {
  if (justGotError.value) {
    justGotError.value = false;
    // The dependency on justGotError seems needed to get some errors to display reactively, especially in
    // response to the Add button; otherwise the v-if="errorsExist()" condition doesn't always trigger an update.
    return true;
  }
  return cldrAddUser.errorsExist(); // boolean
}

function getErrors() {
  return cldrAddUser.getErrors();
}

function localesAreRequired() {
  return newUserLevel.value >= VETTER_LEVEL_NUMBER;
}

/**
 * Let the browser validate the e-mail address
 *
 * @emailAddress string value
 * @return true if the given e-mail address is valid
 *
 * Note: the Survey Tool back end may have different criteria.
 * For example (as of 2021-03), the back end requires a period, while
 * the browser may not. Also, the back end (Java) may normalize the e-mail
 * by Java trim() and toLowerCase().
 */
function validateEmail(emailAddress) {
  const message = "Valid e-mail required.";
  const el = document.createElement("input");
  el.type = "email";
  el.value = emailAddress;
  if (el.checkValidity()) {
    removeError(message);
  } else {
    addError(message);
  }
}

function manageThisUser() {
  cldrAddUser.manageThisUser(newUserEmail.value);
}
</script>

<style scoped>
.addUserErrors,
.locWarnings {
  font-weight: bold;
  font-size: large;
  color: red;
}

table {
  border: 1em solid #cdd;
  border-collapse: collapse;
  background-color: #cdd;
}

.adduser th {
  text-align: right;
  vertical-align: top;
  padding-bottom: 0.5em;
  padding-top: 0.5em;
}

.adduser td {
  vertical-align: top;
}

.adduser label {
  white-space: nowrap;
  vertical-align: top;
}

.adduser select {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

.addButton {
  font-size: x-large;
  text-align: right;
}

.manageButton {
  font-size: x-large;
}
</style>
