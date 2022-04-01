<template>
  <div id="home">
    <p class="hang">
      For more information about the Survey Tool, see
      <a href="http://www.unicode.org/cldr">unicode.org/cldr</a>.
    </p>
    <h1>About this SurveyTool Installation</h1>
    <!-- spinner only shows if there's a delay -->
    <a-spin tip="Loading" v-if="!aboutData" :delay="500" />
    <table v-if="aboutData" class="aboutBox">
      <thead>
        <th>Property</th>
        <th>Value</th>
      </thead>
      <tbody>
        <tr v-for="(v, k) in aboutData" :key="k">
          <td class="aboutKey">
            {{ k }}
          </td>
          <td>
            <span v-if="/^(http|https):.*/.test(v)">
              <!-- Looks like a URL -->
              <a class="aboutValue" v-bind:href="v">{{ v }}</a>
            </span>
            <span v-else-if="valueIsHash(v, k)">
              <!-- Looks like a Git hash, 6…40 hex chars.
                   We need to check that CLDR_COMMIT_BASE is set, because that’s the
                   base URL for the linkification -->
              <a
                class="aboutValue"
                v-bind:href="aboutData.CLDR_COMMIT_BASE + v"
              >
                <i class="glyphicon glyphicon-cog" />{{ v }}
              </a>
            </span>
            <span v-else class="aboutValue">
              <!-- some other type -->
              {{ v }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  data() {
    return {
      aboutData: null,
    };
  },

  created() {
    fetch("api/about")
      .then((r) => r.json())
      .then((data) => (this.aboutData = data));
  },

  methods: {
    valueIsHash(value, key) {
      if (
        this.aboutData.CLDR_COMMIT_BASE &&
        key.includes("HASH") &&
        /^[a-fA-F0-9]{6,40}$/.test(value)
      ) {
        return true;
      }
      return false;
    },
  },
};
</script>

<style scoped>
.aboutBox th,
.aboutBox td {
  padding: 0.5em;
}

.aboutBox thead {
  border-bottom: 2px solid black;
}

.aboutBox tr:nth-child(even) {
  background-color: #fff;
}

.aboutBox tr:nth-child(odd) {
  background-color: #ddd;
}

.aboutBox .aboutKey {
  font-weight: bold;
}

.aboutBox .aboutValue {
  font-family: monospace;
  white-space: pre-wrap;
}
</style>
