<template>
  <ul>
    <template v-if="loggedIn">
      <li v-if="isAdmin"><a href="#admin///">Admin Panel</a></li>
      <!-- My Account only has border-top (section-header) if Admin Panel is shown -->
      <li v-if="isAdmin" class="section-header">My Account</li>
      <li v-else>My Account</li>
      <li>
        <ul>
          <li><a href="#account///">Account Settings</a></li>
        </ul>
      </li>
      <li v-if="!isAdmin && !accountLocked">
        <ul>
          <li>
            <a href="#lock_account///">Lock (Disable) My Account </a>
          </li>
        </ul>
      </li>
      <li class="section-header">My Votes</li>
      <li v-if="canImportOldVotes">
        <ul>
          <li><a href="#oldvotes///">Import Old Votes </a></li>
        </ul>
      </li>
      <li>
        <ul>
          <li><a v-bind:href="recentActivityUrl">Recent Activity</a></li>
        </ul>
      </li>
      <li>
        <ul>
          <li>
            <a v-bind:href="uploadXmlUrl" target="_blank">Upload XML </a>
          </li>
        </ul>
      </li>
      <li class="section-header">My Organization ({{ org }})</li>
      <li v-if="canUseVettingSummary">
        <ul>
          <li><a href="#vsummary///">Priority Items Summary</a></li>
        </ul>
      </li>
      <li v-if="canListUsers">
        <ul>
          <li>
            <a href="#list_users///">List {{ org }} Users</a>
          </li>
        </ul>
      </li>
      <li v-if="canMonitorForum">
        <ul>
          <li><a href="#forum_participation///">Forum Participation</a></li>
        </ul>
      </li>
      <li v-if="canMonitorVetting">
        <ul>
          <li><a href="#vetting_participation///">Vetting Participation</a></li>
        </ul>
      </li>
      <li v-if="accountLocked" class="emphatic">
        LOCKED: Note: your account is currently locked
      </li>
      <li class="section-header">Forum</li>
      <li>
        <ul>
          <li><a href="#announcements///">Announcements</a></li>
          <li>
            <a href="#flagged///"
              ><img
                src="flag.png"
                alt="flag"
                title="flag.png"
                border="0"
              />Flagged Items
            </a>
          </li>
        </ul>
      </li>
      <li>
        <ul>
          <li><a href="#mail///">Notifications (SMOKETEST ONLY)</a></li>
        </ul>
      </li>
      <li v-if="isAdmin">
        <ul>
          <li><a href="#bulk_close_posts///">Bulk Close Posts</a></li>
        </ul>
      </li>
    </template>
    <li class="section-header///">Informational</li>
    <li v-if="canSeeStatistics">
      <ul>
        <li><a href="#statistics///">Statistics</a></li>
      </ul>
    </li>
    <li>
      <ul>
        <li><a href="#about///">About</a></li>
      </ul>
    </li>
    <li v-if="loggedIn">
      <ul>
        <li><a href="#lookup///">Look up a code or xpath</a></li>
      </ul>
    </li>
    <li v-if="isTC">
      <ul>
        <li><a href="#error_subtypes///">Error Subtypes</a></li>
      </ul>
    </li>
  </ul>
</template>

<script>
import * as cldrStatus from "../esm/cldrStatus.mjs";

export default {
  data() {
    return {
      accountLocked: false,
      canImportOldVotes: false,
      canListUsers: false,
      canMonitorVetting: false,
      canSeeStatistics: false,
      canUseVettingSummary: false,
      isAdmin: false,
      isTC: false,
      loggedIn: false,
      org: null,
      recentActivityUrl: null,
      uploadXmlUrl: null,
      userId: 0,
    };
  },

  created() {
    this.initializeData();
  },

  methods: {
    initializeData() {
      const perm = cldrStatus.getPermissions();
      this.accountLocked = perm && perm.userIsLocked;
      this.canImportOldVotes = perm && perm.userCanImportOldVotes;
      this.canListUsers = this.canMonitorVetting =
        perm && (perm.userIsTC || perm.userIsVetter);
      this.canMonitorForum = perm && perm.userCanMonitorForum;
      // this.canSeeStatistics will be false until there is a new implementation
      this.canUseVettingSummary = perm && perm.userCanUseVettingSummary;
      this.isAdmin = perm && perm.userIsAdmin;
      this.isTC = perm && perm.userIsTC;

      const user = cldrStatus.getSurveyUser();
      this.loggedIn = !!user;
      this.userId = user ? user.id : 0;

      this.org = cldrStatus.getOrganizationName();
      this.recentActivityUrl = this.getSpecialUrl("recent_activity");
      this.uploadXmlUrl = this.getSpecialUrl("upload");
    },

    getSpecialUrl(special) {
      // This ought to be encapsulated as part of each special, which could have its own special.getHash
      // function, falling back on (or inheriting from) default ("#" + special)
      let url = "#" + special + "///";
      if ("recent_activity" === special) {
        // cf. cldrAccount.getUserActivityLink
        url += this.userId;
      } else if ("upload" === special) {
        // not one of our "modern specials"; back end still jsp
        const p = new URLSearchParams();
        p.append("s", cldrStatus.getSessionId());
        url = "upload.jsp?a=/cldr-apps/survey&" + p.toString();
      }
      return url;
    },
  },
};
</script>

<style scoped>
a {
  /* enable clicking on space to right of text, as well as on text itself */
  display: block;
}

a:hover {
  color: black;
  background-color: #ccdfff; /* light blue */
}

ul {
  list-style: none;
  display: inline-block;
  width: 100%;
}

li {
  width: 100%;
}

.section-header {
  border-top: 1px solid gray;
}

.emphatic {
  font-weight: bold;
}
</style>
