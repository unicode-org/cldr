<template>
  <div>
    <p v-if="locale">
      Locale: {{ locale }}
      <span v-if="rowData"> ({{ rowData.localeDisplayName }}) </span>
    </p>
    <p v-if="xpath">XPath: {{ xpath }}</p>
    <p v-if="page">Page: {{ page }}</p>
    <a-spin v-if="!rowData" delay="1000" tip="Loading Locale Data" />
    <span v-if="rowData.isReadOnly"
      >(Note: you may not make changes to this locale)</span
    >

    <ul>
      <li v-for="row in rowData.rows" :key="row.xpath">
        <CldrRow :row="row" :locale="locale" />
      </li>
    </ul>
  </div>
</template>

<script>
import CldrRow from "./CldrRow.vue";

export default {
  components: {
    CldrRow,
  },
  props: ["locale", "xpath", "page"],
  data: function () {
    return {
      rowData: {},
    };
  },
  created: function () {
    fetch(`api/voting/${this.locale}/row/${this.xpath}`, {
      headers: {
        "X-SurveyTool-Session": this.$cldrOpts.sessionId,
      },
    })
      .then((r) => r.json())
      .then((data) => (this.rowData = data));
  },
};
</script>

<style></style>
