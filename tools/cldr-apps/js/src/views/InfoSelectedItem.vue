<template>
  <a-tooltip :title="title">
    <div class="info-selected-item" v-if="displayValue && description">
      <div>
        Value:
        <span :class="valueClass" :lang="language" :dir="direction">{{
          displayValue
        }}</span>
        <br />
        <span class="value-description">{{ description }}</span>
        <template v-if="linkUrl">
          <br />
          <a :href="linkUrl">{{ linkText }}</a>
        </template>
      </div>
      <div v-html="testHtml"></div>
      <div v-if="exampleHtml">
        Example:<br />
        <div
          v-html="exampleHtml"
          class="d-example"
          :lang="language"
          :dir="direction"
        ></div>
      </div>
    </div>
  </a-tooltip>
</template>

<script>
import * as cldrText from "../esm/cldrText.mjs";

export default {
  data() {
    return {
      displayValue: null,
      valueClass: null,
      description: null,
      language: null,
      direction: null,
      linkUrl: null,
      linkText: null,
      testHtml: null,
      exampleHtml: null,
      title: null,
    };
  },

  created() {
    this.title = cldrText.get("info_panel_selected");
  },

  methods: {
    setValueAndClass(displayValue, valueClass) {
      this.displayValue = displayValue;
      this.valueClass = valueClass;
    },

    setLanguageAndDirection(language, direction) {
      this.language = language;
      this.direction = direction;
    },

    setDescription(description) {
      this.description = description;
    },

    setLink(linkUrl, linkText) {
      this.linkUrl = linkUrl;
      this.linkText = linkText;
    },

    setTestHtml(testHtml) {
      this.testHtml = testHtml;
    },

    setExampleHtml(exampleHtml) {
      this.exampleHtml = exampleHtml;
    },
  },
};
</script>

<style scoped>
.info-selected-item {
  margin: 4px 4px 6px 2px;
  padding: 4px 4px 6px 4px;
  border: 2px solid white;
}

.info-selected-item:hover {
  border: 2px solid #bce8f1;
}

.value-description {
  margin-top: 0.25em;
  font-size: small;
}
</style>
