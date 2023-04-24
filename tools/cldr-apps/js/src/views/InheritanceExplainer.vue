<template>
  <a-popover
    v-model:visible="visible"
    placement="bottomLeft"
    title="Inheritance"
    trigger="click"
  >
    <template #content>
      <a-spin size="large" :delay="500" v-if="visible && !inheritance" />
      <a-timeline id="inheritanceTimeline" v-if="visible && inheritance">
        <a-timeline-item
          v-for="{
            attribute,
            locale,
            newLocale,
            newXpath,
            reason,
            showReason,
            xpath,
            xpathFull,
          } in inheritance"
          v-bind:color="colorForReason(reason)"
        >
          <p v-bind:title="reason">
            <b class="locale" v-if="newLocale">{{ newLocale }}</b>
            <span class="reason" v-if="showReason">{{
              getReason(reason, attribute)
            }}</span>
            <!-- show the Go button, only if we have locale AND xpath. Show always, so we can navigate   -->
            <a-button
              size="small"
              shape="round"
              v-if="locale && xpath"
              @click="go(locale, xpath)"
              >Jump</a-button
            >
          </p>
          <!-- only show on change -->
          <!-- <b v-if="locale" v-bind:title="Locale ID">{{ locale }}</b> -->
          <p v-if="newXpath" class="xpath">
            {{ xpathFull }}
          </p>
        </a-timeline-item>
      </a-timeline>
    </template>
  </a-popover>
</template>

<script>
import * as cldrInheritance from "../esm/cldrInheritance.mjs";
import * as cldrText from "../esm/cldrText.js";
import { ref } from "vue";
import { notification } from "ant-design-vue";
export default {
  // props: ["visible", "locale", "xpath"],
  setup() {
    let visible = ref(false);
    let itemLocale = ref("");
    let itemXpath = ref("");
    let inheritance = ref(null);
    let reasons = ref(null);
    return {
      visible,
      itemLocale,
      itemXpath,
      inheritance,
      reasons,
    };
  },
  methods: {
    explain(locale, xpath) {
      // clear this first in case something happens
      this.visible = false;
      this.inheritance = null;

      this.itemLocale = locale;
      this.itemXpath = xpath;
      if (locale && xpath) {
        this.visible = true;
      }

      const reasons = cldrInheritance.getInheritanceReasonStrings();
      const explain = cldrInheritance.explainInheritance(locale, xpath);

      Promise.all([reasons, explain]).then(
        async () => {
          this.reasons = await reasons;
          this.inheritance = await explain;
          this.visible = true;
        },
        (err) => {
          console.error(err);
          notification.error({
            description: err.message,
            message: "Error with Inheritance",
            placement: "topLeft",
          });
          this.visible = false;
        }
      );
    },
    colorForReason(reason) {
      return (
        {
          none: "gray",
          value: "green",
          itemAlias: "teal",
          codeFallback: "red",
          constructed: "purple",
          removedAttribute: "blue",
          changedAttribute: "yellow",
          inheritanceMarker: "orange",
        }[reason] || null
      );
    },
    go(locale, xpath) {
      const href = `#/${locale}//${xpath}`;
      window.location.assign(href);
    },
    getReason(reason, attribute) {
      const r = this.reasons[reason] || reason;
      return cldrText.subTemplate(r, { attribute });
    },
  },
};
</script>

<style scoped>
/* .inheritanceTimeline {
  overflow-y: scroll;
} */

.locale {
  padding-right: 0.5em;
}

.reason {
  padding-left: 0.5em;
}
</style>
