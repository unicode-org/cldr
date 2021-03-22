<template>
  <div>
    {{ row.code }} in {{ locale }} with these {{ row.items.length }} values:

    <div v-for="item in row.items" :key="item.value">
      <cldr-value :value="item.value" :dir="row.dir" />
    </div>
  </div>
</template>

<script>
export default {
  props: ["row", "locale"],
};
</script>

<style></style>
