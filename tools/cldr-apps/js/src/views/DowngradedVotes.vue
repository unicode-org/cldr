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

<script setup>
import { onMounted, ref } from "vue";
import * as cldrDowngradedVotes from "../esm/cldrDowngradedVotes.mjs";
import * as cldrText from "../esm/cldrText.mjs";

const canDelete = ref(false);
const downgradeData = ref(null);
const pleaseLogIn = ref(false);
const voteCountImported = ref(0);
const voteCountTotal = ref(0);
const texts = ref({});

onMounted(mounted);

function mounted() {
  cldrDowngradedVotes.refresh(setData);
}

function setData(data) {
  if (data == null) {
    pleaseLogIn.value = true;
  } else {
    downgradeData.value = data;
    countVotes();
    canDelete.value = voteCountImported.value > 0;
  }
}

function countVotes() {
  let imported = 0;
  let total = 0;
  for (let cat of downgradeData.value.cats) {
    total += cat.count;
    if (cat.isImported) {
      imported += cat.count;
    }
  }
  voteCountImported.value = imported;
  voteCountTotal.value = total;
}

function deleteAllImported() {
  cldrDowngradedVotes.deleteAllImported();
}

function text(key) {
  return texts.value[key] || (texts.value[key] = cldrText.get(key));
}
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
