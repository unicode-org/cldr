<template>
  <slot />
  <span v-if="!Object.keys(votes).length">
    <i>No votes.</i>
  </span>
  <table v-else class="voteInfo_perValue table table-vote">
    <tr class="voteInfo_tr voteInfo_tr_heading">
      <td class="voteInfo_voteTitle voteInfo_voteCount voteInfo_td">
        <!-- <span class="badge">vote</span> -->
        Vote
      </td>
      <td class="voteInfo_orgColumn voteInfo_td">Org.</td>
      <td class="voteInfo_valueTitle voteInfo_td">User</td>
    </tr>
    <tr
      v-for="(vote, uid) in votes"
      :key="uid"
      class="voteInfo_tr voteInfo_orgHeading"
    >
      <td class="voteInfo_orgsVote voteInfo_voteCount voteInfo_td">
        <!-- <span class="badge">{{ vote }}</span> -->
        {{ vote }}
      </td>
      <CldrOrgAndUser :uid="uid" />
      <!-- two tds -->
    </tr>
  </table>
  <div>
    <p class="alert alert-warning fix-popover-help">
      Changes to this item require {{ votingResults.requiredVotes }} votes.
    </p>
  </div>
</template>

<script>
import CldrOrgAndUser from "./CldrOrgAndUser.vue";
export default {
  components: {
    CldrOrgAndUser,
  },
  computed: {},
  setup() {
    return {
      votesByValue: null,
    };
  },
  created() {
    // parallelize value-to-vote
    const vbv = [];
    const { winner } = this;
    for (let i = 0; i < this.votingResults.value_vote.length; i += 2) {
      const value = this.votingResults.value_vote[i + 0];
      const vote = this.votingResults.value_vote[i + 1];
      vbv.push({
        value,
        vote,
      });
    }

    // sort with winner first then by vote
    vbv.sort((a, b) => {
      // winner first
      if (a.value == this) {
        return -1;
      } else if (b.value == this) {
        return 1;
      } else if (a.vote > b.vote) {
        return -1;
      } else if (a.vote < b.vote) {
        return 1;
      } else {
        return 0;
      }
    });
    // now add all voters
    const allvotes = Object.entries(this.votes); // [uid, value]
    for (let i = 0; i < vbv.length; i++) {
      const { value } = vbv[i];
      vbv[i].voters = allvotes
        .filter(([uid, v]) => v === value)
        .map(([uid]) => uid);
    }
    this.votesByValue = vbv;
  },
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
    winner: {
      type: String,
      defaults: null,
    },
  },
};
</script>

<style scoped></style>
