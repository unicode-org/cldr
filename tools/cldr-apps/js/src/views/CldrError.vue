<template>
  <div style="background: #ffffff; padding: 30px">
    <a-card hoverable :title="subtypeString">
      <template #extra
        ><a-tag :title="status.subtype" :color="status.type.toLowerCase()">{{
          status.type
        }}</a-tag></template
      >
      <p>
        {{ status.message }}
      </p>
      <template v-if="status.subtypeUrl" #actions
        ><a class="warningReference" :href="status.subtypeUrl"
          >How to Fix…</a
        ></template
      >
    </a-card>
  </div>
</template>

<script>
// import * as cldrLoad from "../esm/cldrLoad.mjs";
// import * as cldrStatus from "../esm/cldrStatus.mjs";
export default {
  computed: {
    myClass() {
      return `cldrError tr_${this.status.type} `;
    },
    subtypeString() {
      return this.status.subtype
        .split(/(?=[A-Z])/)
        .map((s) => s)
        .join(" ");
    },
  },
  props: {
    status: {
      type: Object,
      // any additional classes
      default: "",
    },
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
