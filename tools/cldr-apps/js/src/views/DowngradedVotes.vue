<template>
  <!-- spinner shows if there's a delay -->
  <a-spin tip="Loading" v-if="!downgradeData && !pleaseLogIn" :delay="500" />
  <p v-if="pleaseLogIn">{{ text("downgradedPleaseLogIn") }}</p>
  <template v-if="downgradeData && !pleaseLogIn">
    <section v-if="downgradeData.cats?.length">
      <p>{{ text("downgradedPathsExist") }}</p>
      <table>
        <th>{{ text("downgradedHeaderVoteType") }}</th>
        <th>{{ text("downgradedHeaderImported") }}</th>
        <th>{{ text("downgradedHeaderCount") }}</th>
        <tr v-for="cat of downgradeData.cats" :key="cat">
          <td>{{ cat.voteType }}</td>
          <td>{{ cat.isImported }}</td>
          <td>{{ cat.count }}</td>
        </tr>
        <tr>
          <td>{{ text("downgradedTotal") }}</td>
          <td></td>
          <td>{{ voteCountTotal }}</td>
        </tr>
        <tr>
          <td>{{ text("downgradedTotalImported") }}</td>
          <td></td>
          <td>{{ voteCountImported }}</td>
        </tr>
      </table>
      <p class="note">
        {{ text("downgradedNote") }}
      </p>
    </section>
    <section class="nothingToShow" v-if="voteCountImported === 0">
      {{ text("downgradedCurrentlyNone") }}
    </section>
    <section v-if="canDelete">
      <a-button @click="deleteAllImported" class="deleteButton">
        {{ text("downgradedDeleteAll") }}
      </a-button>
    </section>
  </template>
</template>

<script>
import * as cldrDowngradedVotes from "../esm/cldrDowngradedVotes.mjs";
import * as cldrNotify from "../esm/cldrNotify.mjs";
import * as cldrText from "../esm/cldrText.mjs";

export default {
  data() {
    return {
      canDelete: false,
      downgradeData: null,
      pleaseLogIn: false,
      voteCountImported: 0,
      voteCountTotal: 0,
      texts: {},
    };
  },

  created() {
    cldrDowngradedVotes.refresh(this.setData);
  },

  methods: {
    setData(data) {
      if (data == null) {
        this.pleaseLogIn = true;
      } else {
        this.downgradeData = data;
        this.countVotes();
        this.canDelete = this.voteCountImported > 0;
      }
    },

    countVotes() {
      let imported = 0;
      let total = 0;
      for (let cat of this.downgradeData.cats) {
        total += cat.count;
        if (cat.isImported) {
          imported += cat.count;
        }
      }
      this.voteCountImported = imported;
      this.voteCountTotal = total;
    },

    deleteAllImported() {
      cldrDowngradedVotes.deleteAllImported();
    },

    text(key) {
      return this.texts[key] || (this.texts[key] = cldrText.get(key));
    },
  },
};
</script>

<style scoped>
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

.deleteButton {
  margin: 1em;
  background-color: yellow; /* danger! */
}

.note {
  font-style: italic;
}

table {
  margin: 1em;
  padding: 1ex;
}

th {
  background-color: lightgray;
}

td,
th {
  border: 1px solid black;
  padding: 1ex;
}

td {
  text-align: right;
}
</style>
