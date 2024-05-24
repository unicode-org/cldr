<template>
  <div style="background: #ffffff; padding: 30px">
    <a-card hoverable :title="subtypeString">
      <!-- TODO: Error details are commented out temporarily, see discussion at CLDR-17664 -->
      <template #extra
        ><a-tag
          :title="status.subtype"
          :color="'error' || status.type.toLowerCase()"
          >{{ "error" || status.type }}</a-tag
        ></template
      >
      <!-- <p>
        {{ status.message }}
      </p> -->
      <p>Information on how to fix this is forthcoming.</p>
      <!--
      <template v-if="status.subtypeUrl" #actions>
          <a class="warningReference" :href="status.subtypeUrl">How to Fix…</a>
      </template>
        -->
    </a-card>
  </div>
</template>

<script>
export default {
  computed: {
    myClass() {
      return `cldrError tr_${this.status.type} `;
    },
    subtypeString() {
      return this.status.subtype.split(/(?=[A-Z])/).join(" ");
    },
  },
  props: {
    /**
     * 'status' is a VoteAPI.CheckStatusSummary with these properties:
     *
     *  public String message;
     *  public Type type;
     *  public Subtype subtype;
     *  public String subtypeUrl;
     *  public Phase phase;
     *  public String cause;
     *  public boolean entireLocale;
     *
     * */
    status: Object,
  },
};
</script>

<style scoped>
.subtype {
  font-size: larger;
  font-weight: bold;
  color: black;
}

.cldrError {
  border: 1px solid gray;
  padding: 1em;
  margin-bottom: 2em;
  margin-right: 1em;
}
</style>
