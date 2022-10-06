<template>
  <div>
    <button @click="doSpreadsheet">CLICK THE BIG BUTTON</button>
    <button @click="downloadUserActivity">CLICK THE OTHER BUTTON</button>
    <button @click="downloadUserActivity2">CLICK THE OTHER OTHER BUTTON</button>

    <hr />
    <a href="#account">Go back to the Account Page</a>
  </div>
</template>

<script>
import * as cldrUserListExport from "../esm/cldrUserListExport";
import * as cldrStatus from "../esm/cldrStatus";

export default {
  methods: {
    doSpreadsheet() {
      cldrUserListExport.downloadUserSpreadsheet();
    },
    downloadUserActivity() {
      cldrUserListExport.downloadUserActivity(1289, cldrStatus.getSessionId());
    },
    downloadUserActivity2() {
      cldrUserListExport.downloadUserActivity(1815, cldrStatus.getSessionId());
    },
  },
};
</script>

<style></style>
