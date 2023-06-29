<template>
  <slot />
  <span v-if="!Object.keys(votes).length">
    <i>No votes.</i>
  </span>
  <table v-else class="voteInfo_perValue table table-vote">
    <tr class="voteInfo_tr voteInfo_tr_heading">
      <td class="voteInfo_orgColumn voteInfo_td">Org.</td>
      <td class="voteInfo_valueTitle voteInfo_td">User</td>
      <td class="voteInfo_voteTitle voteInfo_voteCount voteInfo_td">
        <span class="badge">TOTALCOUNT</span>
      </td>
    </tr>
    <tr v-for="(vote, uid) in votes" :key="uid" class="voteInfo_tr voteInfo_orgHeading">
      <td class="voteInfo_orgColumn voteInfo_td">ORG</td>
      <td class="voteInfo_voterInfo voteInfo_td" style="text-align: left;"><CldrUser :uid="uid"/></td>
      <td class="voteInfo_orgsVote voteInfo_voteCount voteInfo_td">
        <span class="badge">{{ vote }}</span>
      </td>
    </tr>
  </table>
  <div>
  <p class="alert alert-warning fix-popover-help">Changes to this item require {{ votingResults.requiredVotes }} votes.</p>
  </div>
</template>

<script>
// import CldrValue from "./CldrValue.vue";
import CldrUser from "./CldrUser.vue";
export default {
  components: {
    CldrUser,
  },
  computed: {},
  props: {
    votes: {
      type: Object,
      // any additional classes
      default: null,
    },
    votingResults: {
      type: Object,
      // defaults to current locale's dir
      default: null,
    },
  },
};
</script>

<style scoped></style>
