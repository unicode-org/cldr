<template>
  <div>
    <p>This is only a test. But by the way, hello {{ $specialPage }}</p>

    <!-- <CldrRows locale="br" xpath="132345490064d839"> </CldrRows> -->
    <CldrRows locale="br" page="Languages_K_N"> </CldrRows>
    <hr />

    <!-- <CldrRows locale="sr_Latn" xpath="132345490064d839">

  </CldrRows> -->
  </div>
</template>

<script>
import CldrRows from "./CldrRows.vue";

export default {
  components: {
    CldrRows,
  },
};
</script>

<style></style>
