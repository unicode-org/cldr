<template>
  <tr class="vother">
    <td class="d-code codecell">
      {{ row.code }}
    </td>
    <td class="d-disp comparisoncell">
      {{ row.displayName }}
    </td>
    <td class="d-no nocell">
      <input type="radio" />
    </td>
    <td class="d-dr statuscell">
      {{ row.resolver.winningStatus }}
    </td>
    <td title="$flyoverproposed" class="d-win proposedcell">
      <div v-for="item in row.items" :key="item.value">
        <cldr-value v-if="item.value == row.resolver.winningValue">{{
          item.value
        }}</cldr-value>
      </div>
    </td>
    <td title="$flyoveradd" class="d-win addcell">
      <button>+</button>
    </td>
    <td title="$flyoverother" class="d-win othercell">
      <div v-for="item in row.items" :key="item.value">
        <cldr-value
          v-if="item.value != row.resolver.winningValue"
          :dir="row.dir"
          >{{ item.value }}</cldr-value
        >
      </div>
    </td>
  </tr>
</template>

<script>
export default {
  props: ["row", "locale"],
};
</script>

<style></style>
