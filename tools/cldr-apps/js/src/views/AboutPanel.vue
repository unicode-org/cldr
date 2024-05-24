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
              <!-- Looks like a Git hash, 6…40 hex chars. -->
              <a class="aboutValue" v-bind:href="makeCommitUrl(v)">
                <i class="glyphicon glyphicon-cog" />{{ v }}
              </a>
              |
              <a class="aboutValue" v-bind:href="makeCompareReleaseUrl(v)"
                >compare to release</a
              >
              |
              <a class="aboutValue" v-bind:href="makeCompareMainUrl(v)"
                >compare to main</a
              >
            </span>
            <span v-else class="aboutValue">
              <!-- some other type -->
              {{ v }}
            </span>
          </td>
        </tr>
        <tr>
          <td class="aboutKey">Vue version</td>
          <td>
            <span class="aboutValue">{{ vueVersion }}</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { version } from "vue";
import * as cldrText from "../esm/cldrText.mjs";

export default {
  data() {
    return {
      aboutData: null,
      commitPrefix: null,
      comparePrefix: null,
      compareMain: null,
      compareRelease: null,
      vueVersion: null,
    };
  },

  created() {
    this.vueVersion = " " + version;
    fetch("api/about")
      .then((r) => r.json())
      .then(this.setData);
  },

  methods: {
    setData(data) {
      this.aboutData = data;
      const oldVersion = data.OLD_VERSION || "0";
      this.commitPrefix = cldrText.get("git_commit_url_prefix");
      this.comparePrefix = cldrText.get("git_compare_url_prefix");
      this.compareMain = cldrText.get("git_compare_url_main");
      this.compareRelease = cldrText.sub("git_compare_url_release", [
        oldVersion,
      ]);
    },

    makeCommitUrl(value) {
      return this.commitPrefix + value;
    },

    makeCompareMainUrl(value) {
      return this.comparePrefix + value + "..." + this.compareMain;
    },

    makeCompareReleaseUrl(value) {
      return this.comparePrefix + this.compareRelease + "..." + value;
    },

    valueIsHash(value, key) {
      if (key.includes("HASH") && /^[a-fA-F0-9]{6,40}$/.test(value)) {
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
