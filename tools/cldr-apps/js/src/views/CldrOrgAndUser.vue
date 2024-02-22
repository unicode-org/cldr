<template>
  <td class="voteInfo_orgColumn voteInfo_td">{{ org }}</td>
  <td class="voteInfo_voterInfo voteInfo_td" style="text-align: left">
    <CldrUser
      :noHover="true"
      :plain="true"
      ref="user"
      @org="updateOrg"
      :uid="uid"
    />
  </td>
</template>

<script>
import CldrUser from "./CldrUser.vue";
import { ref } from "vue";
const user = ref(null);
export default {
  setup() {
    return {
      user,
    };
  },
  data() {
    return {
      org: null,
    };
  },
  components: {
    CldrUser,
  },
  computed: {
    computedOrg() {
      this.org || "-";
    },
  },
  props: {
    uid: {
      type: String,
      default: null,
    },
  },
  methods: {
    updateOrg() {
      // propagate org
      this.org = this.user.org;
    },
  },
};
</script>

<style scoped></style>
