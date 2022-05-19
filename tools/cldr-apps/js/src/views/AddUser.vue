<template>
  <article>
    <a-spin v-if="loading" :delay="500" />
    <div v-if="errors.length">
      <span class="addUserErrors">Please correct the following error(s):</span>
      <ul>
        <li v-for="error in errors">{{ error }}</li>
      </ul>
    </div>
    <div v-if="!loading && !addedNewUser" class="adduser">
      <h2>Add User</h2>
      <table>
        <tr>
          <th><label for="new_name">Name:</label></th>
          <td>
            <input
              size="40"
              id="new_name"
              name="new_name"
              v-model="newUser.name"
            />
          </td>
        </tr>
        <tr>
          <th><label for="new_email">E-mail:</label></th>
          <td>
            <input
              size="40"
              id="new_email"
              name="new_email"
              v-model="newUser.email"
              type="email"
            />
          </td>
        </tr>
        <tr>
          <th><label for="new_org">Organization:</label></th>
          <td v-if="canChooseOrg">
            <select id="new_org" name="new_org" v-model="newUser.org">
              <option disabled value="">Please select one</option>
              <option v-for="org in orgList">{{ org }}</option>
            </select>
          </td>
          <td v-else>
            <input id="new_org" disabled="disabled" v-model="newUser.org" />
          </td>
        </tr>
        <tr>
          <th><label for="new_level">User level:</label></th>
          <td>
            <select id="new_level" name="new_level" v-model="newUser.level">
              <option disabled value="">Please select one</option>
              <option
                v-for="(v, number) in levelList"
                v-bind:value="number"
                :disabled="!v.canCreateOrSetLevelTo"
              >
                {{ v.string }}
              </option>
            </select>
          </td>
        </tr>
        <tr>
          <th><label for="new_locales">Languages responsible:</label></th>
          <td>
            <input
              id="new_locales"
              name="new_locales"
              v-model="newUser.locales"
              @change="validateLocales"
              placeholder="en de de_CH fr zh_Hant"
            />
            <button v-on:click="setAllLocales()">All Locales</button><br />
            (Space separated. Use the All Locales button to grant access to all
            locales. )<br />

            <div v-if="locWarnings">
              <span class="locWarnings"
                >The following locales will not be added due to problems:</span
              >
              <ul>
                <li v-bind:key="loc" v-for="loc in Object.keys(locWarnings)">
                  <tt>{{ loc }}</tt>
                  {{ getParenthesizedName(loc) }}
                  — {{ explainWarning(locWarnings[loc]) }}
                </li>
              </ul>
            </div>
          </td>
        </tr>
        <tr class="addButton">
          <td colspan="2">
            <button v-on:click="add()">Add</button>
          </td>
        </tr>
      </table>
    </div>

    <div v-if="addedNewUser">
      <h2>Added User</h2>
      <p>
        ✅ The new user was added. Name: <kbd>{{ newUser.name }}</kbd> E-mail:
        <kbd>{{ newUser.email }}</kbd> ID:
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
      <p><button v-on:click="initializeData()">Add another user</button></p>
    </div>
  </article>
</template>

<script>
import * as cldrAccount from "../esm/cldrAccount.js";
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrStatus from "../esm/cldrStatus.js";
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrText from "../esm/cldrText.js";

export default {
  data() {
    return {
      addedNewUser: false,
      canChooseOrg: null,
      errors: [],
      locWarnings: null,
      levelList: null,
      loading: false,
      newUser: {
        email: null,
        level: null,
        locales: null,
        name: null,
        org: null,
      },
      orgList: null,
      userId: null,
    };
  },

  created() {
    this.initializeData();
  },

  methods: {
    initializeData() {
      this.addedNewUser = false;
      this.userId = null;
      this.newUser.name = "";
      this.newUser.email = "";
      this.newUser.level = "";
      this.newUser.locales = "und";
      this.getLevelList();
      if (cldrStatus.getPermissions().userIsAdmin) {
        this.canChooseOrg = true;
        this.newUser.org = "";
        this.getOrgList();
      } else {
        this.canChooseOrg = false;
        this.newUser.org = cldrStatus.getOrganizationName();
        this.orgList = "";
      }
    },

    async validateLocales() {
      await cldrAjax
        .doFetch(
          "./api/locales/normalize?" +
            new URLSearchParams({
              locs: this.newUser.locales,
              org: this.newUser.org,
            })
        )
        .then(cldrAjax.handleFetchErrors)
        .then((r) => r.json())
        .then(({ messages, normalized }) => {
          if (this.newUser.locales != normalized) {
            // only update the warnings if the normalized value changes
            this.newUser.locales = normalized;
            this.locWarnings = messages;
          }
        })
        .catch((e) => this.errors.push(`Error: ${e} validating locale`));
    },

    getLevelList() {
      this.levelList = cldrAccount.getLevelList();
      if (this.levelList) {
        return;
      }
      this.loading = true;
      const xhrArgs = {
        url: this.getLevelsUrl(),
        handleAs: "json",
        load: (json) => this.loadLevelList(json),
        error: (err) => this.errors.push(err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    loadLevelList(json) {
      if (!json.levels) {
        this.errors.push("Level list not received from server");
        this.loading = false;
      } else {
        this.levelList = json.levels;
        if (this.orgList || this.newUser.org) {
          this.loading = false;
        }
      }
    },

    getLocaleName(loc) {
      if (!loc) return null;
      return cldrLoad.getTheLocaleMap()?.getLocaleName(loc);
    },

    getParenthesizedName(loc) {
      const name = this.getLocaleName(loc);
      if (name && name !== loc) {
        return `(${name})`;
      }
      return "";
    },

    explainWarning(reason) {
      return cldrText.get(`locale_rejection_${reason}`, reason);
    },

    getOrgList() {
      this.orgList = cldrAccount.getOrgList();
      if (this.orgList) {
        return;
      }
      this.loading = true;
      const xhrArgs = {
        url: cldrAjax.makeApiUrl("organizations", null),
        handleAs: "json",
        load: (json) => this.loadOrgList(json),
        error: (err) => this.errors.push(err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    loadOrgList(json) {
      if (!json.list) {
        this.errors.push("Organization list not received from server");
        this.loading = false;
      } else {
        this.orgList = json.list;
        if (this.levelList) {
          this.loading = false;
        }
      }
    },

    async add() {
      this.validate();
      await this.validateLocales();
      if (this.errors.length) {
        return;
      }
      const xhrArgs = {
        url: this.getAddUserUrl(),
        postData: this.newUser,
        handleAs: "json",
        load: this.loadHandler,
        error: (err) => this.errors.push(err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    validate() {
      this.errors = [];
      if (!this.newUser.name) {
        this.errors.push("Name required.");
      }
      if (!this.newUser.email) {
        this.errors.push("E-mail required.");
      } else if (!this.validateEmail(this.newUser.email)) {
        this.errors.push("Valid e-mail required.");
      }
      if (!this.newUser.org) {
        this.errors.push("Organization required.");
      }
      if (!this.newUser.level) {
        this.errors.push("Level required.");
      }
    },

    /**
     * Let the browser validate the e-mail address
     *
     * @return true if the given e-mail address is valid
     *
     * Note: the Survey Tool back end may have different criteria.
     * For example (as of 2021-03), the back end requires a period, while
     * the browser may not. Also, the back end (Java) may normalize the e-mail
     * by Java trim() and toLowerCase().
     */
    validateEmail(emailAddress) {
      const el = document.createElement("input");
      el.type = "email";
      el.value = emailAddress;
      return el.checkValidity();
    },

    loadHandler(json) {
      if (json.err) {
        this.errors.push(
          "Error from the server: " + this.translateErr(json.err)
        );
      } else if (!json.userId) {
        this.errors.push("The server did not return a user id.");
      } else {
        const n = Math.floor(Number(json.userId));
        if (
          String(n) !== String(json.userId) ||
          n <= 0 ||
          !Number.isInteger(n)
        ) {
          this.errors.push("The server returned an invalid id: " + json.userId);
        } else {
          this.addedNewUser = true;
          this.userId = Number(json.userId);
          if (json.email) {
            this.newUser.email = json.email; // normalized, e.g., to lower case by server
          }
        }
      }
    },

    translateErr(err) {
      const map = {
        BAD_NAME: "Missing or invalid name",
        BAD_EMAIL: "Missing or invalid e-mail",
        BAD_ORG: "Missing or invalid organization",
        BAD_LEVEL: "Missing, invalid, or forbidden user level",
        DUP_EMAIL: "A user with that e-mail already exists",
        UNKNOWN: "An unspecified error occurred",
      };
      if (!map[err]) {
        return err;
      }
      return map[err] + " [" + err + "]";
    },

    setAllLocales() {
      this.newUser.locales = "*";
      return false;
    },

    manageThisUser() {
      cldrAccount.zoomUser(this.newUser.email);
    },

    getAddUserUrl() {
      const p = new URLSearchParams();
      p.append("s", cldrStatus.getSessionId());
      return cldrAjax.makeApiUrl("adduser", p);
    },

    getLevelsUrl() {
      const p = new URLSearchParams();
      p.append("s", cldrStatus.getSessionId());
      return cldrAjax.makeApiUrl("userlevels", p);
    },
  },
};
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
