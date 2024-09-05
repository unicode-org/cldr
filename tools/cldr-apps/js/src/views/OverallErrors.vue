<template>
  <a-spin v-if="!loaded" />
  <div v-if="loaded && errors">
    <h2>Overall Errors</h2>
    <p>
      Check the following errors carefully. These issues often require a ticket
      to be filed with CLDR to fix.
    </p>

    <cldr-error v-for="test of errors" :key="test.subtype" :status="test" />
  </div>
</template>

<script setup>
import { ref } from "vue";
import { getLocaleErrors } from "../esm/cldrDashData.mjs";
import { getCurrentLocale } from "../esm/cldrStatus.mjs";
import * as cldrNotify from "../esm/cldrNotify.mjs";

const loaded = ref(false);
const errors = ref(null);
const locale = ref(getCurrentLocale());

async function loadData() {
  const resp = await getLocaleErrors(locale.value);
  if (!resp || !resp.body) {
    errors.value = null;
  } else {
    const { body } = resp;
    const { tests } = body;
    errors.value = tests;
  }
  loaded.value = true;
}

loadData().then(
  () => {},
  (err) => {
    console.error(err);
    cldrNotify.exception(
      err,
      "Loading overall errors for " + getCurrentLocale()
    );
  }
);
</script>
