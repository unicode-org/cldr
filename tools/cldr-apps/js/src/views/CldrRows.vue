<template>
  <div>
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
    </div>
    <table class="data table table-bordered vetting-page">
      <thead>
        <tr class="headingb active">
          <th title="Code for this item">Code</th>
          <th title="Comparison value">English</th>
          <th title="Abstain from voting on this item" class="d-no">Abstain</th>
          <th title="Approval Status" class="d-status">A</th>
          <th title="Winning value">Winning</th>
          <th title="Add another value">Add</th>
          <th title="Other non-winning items">Others</th>
        </tr>
      </thead>
      <tbody v-for="row in rowData.rows" :key="row.xpath">
        <CldrRow :row="row" :locale="locale" />
      </tbody>
    </table>
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
    let theUrl;
    if (this.page) {
      theUrl = `api/voting/${this.locale}/page/${this.page}`;
    } else if (this.xpath) {
      theUrl = `api/voting/${this.locale}/row/${this.xpath}`;
    } else {
      throw Error(`Need xpath= or page= to continue.`);
    }
    fetch(theUrl, {
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
