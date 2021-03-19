<template>
  <table>
    <tr>
      <th><label for="newUser.name">Name:</label></th>
      <td>
        <input size="40" id="new_name" name="new_name" v-model="newUser.name" />
      </td>
    </tr>
    <tr>
      <th><label for="newUser.email">E-mail:</label></th>
      <td>
        <input
          size="40"
          id="new_email"
          name="new_email"
          v-model="newUser.email"
        />
      </td>
    </tr>
    <tr>
      <th><label for="newUser.org">Organization:</label></th>
      <td v-if="myorg">
        <input id="new_org" disabled="disabled" v-model="newUser.org" />
      </td>
      <td v-else>
        <select>
          <option value="" selected="selected">Choose...</option>
          <!-- for(String o : UserRegistry.getOrgList()) -->
        </select>
      </td>
    </tr>
    <tr>
      <th><label for="newUser.level">Userlevel:</label></th>
      <td>
        <select id="new_level" name="new_level" v-model="newUser.level">
          <!-- for (int lev : UserRegistry.ALL_LEVELS) -->
          <option value="lev">...</option>
        </select>
      </td>
    </tr>
    <tr>
      <th><label for="newUser.locales">Languages responsible:</label></th>
      <td>
        <input id="new_locales" name="new_locales" v-model="newUser.locales" />
        <button v-on:click="setAllLocales()">All Locales</button><br />
        (Space separated. Examples: "en de de_CH fr zh_Hant". Use the All
        Locales button to grant access to all locales. )
      </td>
    </tr>
    <tr class="submit">
      <td colspan="2"><button v-on:click="add()">Add</button></td>
    </tr>
  </table>
</template>

<script>
import * as cldrAjax from "../../../src/main/webapp/js/esm/cldrAjax.js";
import * as cldrStatus from "../../../src/main/webapp/js/esm/cldrStatus.js";

export default {
  data() {
    return {
      newUser: {
        name: null,
        email: null,
        org: null,
        level: null,
        locales: null,
      },
      err: null,
      myorg: null,
    };
  },

  created() {
    this.newUser.org = this.myorg = cldrStatus.getOrganizationName();
  },

  methods: {
    add(event) {
      const xhrArgs = {
        url: "api/adduser",
        postData: JSON.stringify(this.newUser),
        handleAs: "json",
        load: this.loadHandler,
        error: (err) => (this.err = "❌  " + err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    loadHandler(json) {
      if (json.err) {
        this.err = "Error: " + json.err;
      } else {
        this.newUser = json.newUser;
        this.err = null;
      }
    },

    setAllLocales() {
      this.newUser.locales = "*";
      return false;
    },
  },
};
</script>
