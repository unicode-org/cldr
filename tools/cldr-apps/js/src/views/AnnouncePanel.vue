<template>
  <!-- spinner shows if there's a delay -->
  <a-spin tip="Loading" v-if="!announcementData && !pleaseLogIn" :delay="500" />
  <p v-if="pleaseLogIn">To view announcements, please log in.</p>
  <template v-if="announcementData">
    <section class="fullJustified">
      <span class="summaryCounts">
        {{ unreadCount }} unread, {{ totalCount }} total
      </span>
      <span class="rightControl">
        <input type="checkbox" v-model="showUnreadOnly" /><label
          for="showUnreadOnly"
        >
          &nbsp;show unread only</label
        >
      </span>
    </section>
    <div
      class="nothingToShow"
      v-if="announcementData.announcements.length === 0"
    >
      There are no announcements yet
    </div>
    <div
      class="nothingToShow"
      v-if="
        announcementData.announcements.length > 0 &&
        unreadCount === 0 &&
        showUnreadOnly
      "
    >
      There are no unread announcements 🎉
    </div>
    <div v-if="canAnnounce" class="composeButton">
      <a-button title="Compose a new announcement" @click="startCompose">
        Compose a new announcement
      </a-button>
    </div>
    <div v-if="formIsVisible" ref="popover" class="popoverForm">
      <AnnounceForm
        :postOrCancel="finishCompose"
        :formHasAllOrgs="canChooseAllOrgs"
      />
    </div>
    <template
      v-for="(announcement, i) in announcementData.announcements"
      :key="i"
    >
      <AnnouncePost
        :announcement="announcement"
        :checkmarkChanged="checkmarkChanged"
        :showUnreadOnly="showUnreadOnly"
      />
    </template>
  </template>
</template>

<script>
import * as cldrAnnounce from "../esm/cldrAnnounce.mjs";
import AnnounceForm from "./AnnounceForm.vue";
import AnnouncePost from "./AnnouncePost.vue";
import { notification } from "ant-design-vue";

export default {
  components: {
    AnnounceForm,
    AnnouncePost,
  },

  data() {
    return {
      announcementData: null,
      canAnnounce: false,
      canChooseAllOrgs: false,
      formIsVisible: false,
      pleaseLogIn: false,
      showUnreadOnly: true,
      totalCount: 0,
      unreadCount: 0,
    };
  },

  created() {
    this.canAnnounce = cldrAnnounce.canAnnounce();
    this.canChooseAllOrgs = cldrAnnounce.canChooseAllOrgs();
    cldrAnnounce.refresh(this.setData);
  },

  methods: {
    setData(data) {
      if (data == null) {
        this.pleaseLogIn = true;
      } else {
        this.announcementData = data;
        this.updateCounts();
      }
    },

    checkmarkChanged(event, announcement) {
      cldrAnnounce.saveCheckmark(event.target.checked, announcement);
      this.updateCounts();
    },

    updateCounts() {
      this.totalCount = this.announcementData.announcements.length;
      let checkedCount = 0;
      for (let announcement of this.announcementData.announcements) {
        if (announcement.checked) {
          ++checkedCount;
        }
      }
      this.unreadCount = this.totalCount - checkedCount;
    },

    startCompose() {
      this.formIsVisible = true;
    },

    finishCompose(formState) {
      this.formIsVisible = false;
      if (formState) {
        cldrAnnounce.compose(formState, this.composeResult);
      }
    },

    composeResult(result) {
      if (result?.ok) {
        notification.success({
          placement: "topLeft",
          message: "Your announcement was posted successfully",
          duration: 3,
        });
      } else {
        const errMessage = result?.err || "unknown";
        notification.error({
          placement: "topLeft",
          message: "Your announcement was not posted: " + errMessage,
        });
      }
      cldrAnnounce.refresh(this.setData);
    },
  },
};
</script>

<style scoped>
.summaryCounts {
  display: block;
  margin-left: 0;
  margin-right: auto;
  font-size: larger;
}

.nothingToShow {
  font-weight: bold;
  font-size: 24px;
  color: #40a9ff;
  background-color: #f5f5f5;
  border: 1px solid #e3e3e3;
  border-radius: 3px;
  padding: 1em;
  margin: 1em;
}

.fullJustified {
  display: flex;
  justify-content: space-between;
  flex-direction: row;
  align-items: baseline;
}

.rightControl {
  display: flex;
  justify-content: flex-end;
  text-align: baseline;
}

.popoverForm {
  display: block;
  top: 10%;
  left: 10%;
  width: 80%;
  position: absolute;
  padding: 20px 20px;
  z-index: 1200;
  background-color: #f5f5f5;
  border: 1px solid #e3e3e3;
  border-radius: 3px;
}

.composeButton {
  padding: 1em;
}

label {
  font-weight: normal;
  margin: 0; /* override bootcamp */
}

input {
  margin: 0; /* override bootcamp */
}
</style>
