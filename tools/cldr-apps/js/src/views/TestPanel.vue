<template>
  <div>
    <img v-if="specialFlag" src="STLogo.png" title="Logo of the Survey Tool" />
    <p v-if="!specialFlag">
      No special flag was set. It can be enabled with the following in bootstrap.properties or equivalent:
      <pre>
        CLDR_FE_DEBUG={specialFlag:true}
      </pre>
    </p>
    <p>This is only a test! But by the way, hello {{ $specialPage }}</p>

    <CldrRows locale="br" xpstrid="132345490064d839"> </CldrRows>
    <!-- <CldrRows locale="br" page="Languages_K_N"> </CldrRows> -->
    <hr />

    <!-- <CldrRows locale="sr_Latn" xpstrid="132345490064d839">

  </CldrRows> -->
  </div>
</template>

<script>
import CldrRows from "./CldrRows.vue";
import * as cldrDebug from "../esm/cldrDebug.mjs";

export default {
  components: {
    CldrRows,
  },
  setup() {
    const { specialFlag } = cldrDebug.all();
    return {
      specialFlag: !!specialFlag,
    };
  },
};
</script>

<style></style>
