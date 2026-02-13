<template>
  <!-- spinner shows if there's a delay -->
  <a-spin tip="Loading" v-if="!locData && !pleaseLogIn" :delay="500" />
  <p v-if="pleaseLogIn">{{ text("badLocalesPleaseLogIn") }}</p>
  <template v-if="locData && !pleaseLogIn">
    <section class="nothingToShow" v-if="!problemCount">
      {{ text("badLocalesCurrentlyNone") }}
    </section>
    <section v-else>
      <p>{{ text("badLocalesExist") }}</p>
      <h3>Summary</h3>
      <table>
        <th>Rejection</th>
        <th>Locale ID Count</th>
        <tr
          v-for="rejection of Object.keys(locData.totals).sort()"
          :key="rejection"
        >
          <td>{{ rejection }}</td>
          <td>{{ locData.totals[rejection] }}</td>
        </tr>
        <tr>
          <td>TOTAL</td>
          <td>{{ problemCount }}</td>
        </tr>
      </table>
      <h3>Details Per Locale</h3>
      <table>
        <th v-for="h of cldrBadLocaleIds.getLocaleHeaderRow()" :key="h">
          {{ h }}
        </th>
        <tr v-for="problem of locData.problems" :key="problem">
          <td
            v-for="cell of cldrBadLocaleIds.getLocaleBodyRow(problem)"
            :key="cell"
          >
            {{ cell }}
          </td>
        </tr>
      </table>
      <section>
        <a-button @click="saveLocalesAsSheet"
          >Save Locales as Spreadsheet .xlsx</a-button
        >
      </section>

      <h3>Details Per User</h3>
      <table>
        <th v-for="h of cldrBadLocaleIds.getUserHeaderRow()" :key="h">
          {{ h }}
        </th>
        <tr v-for="user of locData.users" :key="user">
          <td v-for="cell of cldrBadLocaleIds.getUserBodyRow(user)" :key="cell">
            {{ cell }}
          </td>
        </tr>
      </table>
      <section>
        <a-button @click="saveUsersAsSheet"
          >Save Users as Spreadsheet .xlsx</a-button
        >
      </section>
    </section>
    <section class="importantNotice" v-if="locData.leaderlessOrgNames?.length">
      <span class="cautionIcon">⚠️</span>
      {{ text("badLocalesLeaderlessOrgs") }}
      <ul>
        <li v-for="org of locData.leaderlessOrgNames" :key="org">{{ org }}</li>
      </ul>
    </section>
    <section v-if="canFix">
      <a-button @click="fixAll" class="fixButton">
        {{ text("badLocalesFixAll") }}
      </a-button>
    </section>
  </template>
</template>

<script setup>
import { onMounted, ref } from "vue";
import * as cldrBadLocaleIds from "../esm/cldrBadLocaleIds.mjs";
import * as cldrText from "../esm/cldrText.mjs";

const canFix = ref(false);
const locData = ref(null);
const pleaseLogIn = ref(false);
const problemCount = ref(0);
const texts = ref({});

onMounted(mounted);

function mounted() {
  cldrBadLocaleIds.refresh(setData);
}

function setData(data) {
  if (data == null) {
    pleaseLogIn.value = true;
  } else {
    locData.value = data;
    problemCount.value = data.problems.length;
    canFix.value = problemCount.value > 0;
  }
}

function fixAll() {
  cldrBadLocaleIds.fixAll();
}

function text(key) {
  return texts.value[key] || (texts.value[key] = cldrText.get(key));
}

function saveLocalesAsSheet() {
  return cldrBadLocaleIds.saveLocalesAsSheet();
}

function saveUsersAsSheet() {
  return cldrBadLocaleIds.saveUsersAsSheet();
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

.importantNotice {
  font-weight: bold;
  color: #3d607c;
  background-color: #f5f5f5;
  border: 1px solid #e3e3e3;
  border-radius: 3px;
  padding: 1em;
  margin: 1em;
}

.cautionIcon {
  font-size: 32px;
}

button {
  margin: 1em;
}

.fixButton {
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
