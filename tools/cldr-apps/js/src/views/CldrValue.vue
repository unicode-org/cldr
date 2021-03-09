<template>
  <span v-bind:dir="dir" class="cldrValue">
    {{ value }}
  </span>
</template>

<script>
export default {
  props: ["value", "dir"],
};
</script>

<style scoped>
.cldrValue {
  font-family: Cambria, Cochin, Georgia, Times, "Times New Roman", serif;
}
</style>
>
