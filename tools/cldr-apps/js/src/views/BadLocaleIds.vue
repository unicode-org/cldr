<template>
  <!-- spinner shows if there's a delay -->
  <a-spin tip="Loading" v-if="!locData && !pleaseLogIn" :delay="500" />
  <p v-if="pleaseLogIn">{{ text("badLocalesPleaseLogIn") }}</p>
  <template v-if="locData && !pleaseLogIn">
    <section v-if="problemCount">
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
      <h3>Details</h3>
      <table>
        <th v-for="h of cldrBadLocaleIds.getHeaderRow()" :key="h">
          {{ h }}
        </th>
        <tr v-for="problem of locData.problems" :key="problem">
          <td v-for="cell of cldrBadLocaleIds.getBodyRow(problem)" :key="cell">
            {{ cell }}
          </td>
        </tr>
      </table>
      <p></p>
    </section>
    <section class="nothingToShow" v-if="!problemCount">
      {{ text("badLocalesCurrentlyNone") }}
    </section>
    <section v-if="canDelete">
      <a-button @click="saveAsSheet">Save as Spreadsheet .xlsx</a-button>
      <a-button @click="fixAll" class="deleteButton">
        {{ text("badLocalesFixAll") }}
      </a-button>
    </section>
  </template>
</template>

<script setup>
import { onMounted, ref } from "vue";
import * as cldrBadLocaleIds from "../esm/cldrBadLocaleIds.mjs";
import * as cldrText from "../esm/cldrText.mjs";

const canDelete = ref(false);
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
    canDelete.value = problemCount.value > 0;
  }
}

function fixAll() {
  cldrBadLocaleIds.fixAll();
}

function text(key) {
  return texts.value[key] || (texts.value[key] = cldrText.get(key));
}

function saveAsSheet() {
  return cldrBadLocaleIds.saveAsSheet();
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

button {
  margin: 1em;
}

.deleteButton {
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
