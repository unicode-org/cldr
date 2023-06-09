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
            hidden,
          } in inheritance"
          v-bind:color="colorForReason(reason)"
        >
          <template #dot v-if="!isTerminal(reason)">
            <!-- nonterminal show as a down arrow -->
            <i class="glyphicon glyphicon-circle-arrow-down" />
          </template>
          <p v-bind:title="reason">
            <b class="locale" v-if="newLocale">{{ newLocale }}</b>
            <span class="reason" v-if="showReason">{{
              getReason(reason, attribute)
            }}</span>
            &nbsp;
            <!-- show the Jump button, only if we have locale AND xpath. Hide if we're already there.  -->
            <button
              class="cldr-nav-btn"
              v-if="
                !hidden &&
                locale &&
                xpath &&
                (locale != currentLocale || xpath != currentId)
              "
              @click="go(locale, xpath)"
            >
              Jump
            </button>
          </p>
          <!-- only show on change -->
          <div v-if="newXpath" :class="xpathClass(hidden)">
            {{ xpathFull }}
          </div>
        </a-timeline-item>
      </a-timeline>
      <button
        class="cldr-nav-btn"
        title="Close the Inheritance Explainer"
        @click="closeInheritanceExplainer"
      >
        Close
      </button>
    </template>
  </a-popover>
</template>

<script>
import * as cldrInheritance from "../esm/cldrInheritance.mjs";
import * as cldrStatus from "../esm/cldrStatus.mjs";
import * as cldrText from "../esm/cldrText.mjs";
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
    const { currentLocale, currentId } = cldrStatus.refs;
    return {
      visible,
      itemLocale,
      itemXpath,
      inheritance,
      reasons,
      currentLocale,
      currentId,
    };
  },
  methods: {
    xpathClass(hidden) {
      if (hidden) {
        return "xpath xpath-hidden";
      } else {
        return "xpath";
      }
    },
    explain(locale, xpath) {
      // clear this first in case something happens
      this.visible = false;
      this.inheritance = null;

      this.itemLocale = locale;
      this.itemXpath = xpath;
      if (!locale || !xpath) {
        // Shouldn't get here unless the caller made an error.
        notification.info({
          description:
            "Please click on an item first, and then click this button.",
          message: "Inheritance Explainer",
          placement: "topLeft",
        });
        return;
      }
      this.visible = true;

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
          changedAttribute: "yellow",
          codeFallback: "red",
          constructed: "purple",
          fallback: "brown",
          inheritanceMarker: "orange",
          itemAlias: "teal",
          none: "gray",
          removedAttribute: "blue",
          value: "green",
        }[reason] || null
      );
    },
    isTerminal(reason) {
      return this.reasons[reason]?.terminal;
    },
    go(locale, xpath) {
      const href = `#/${locale}//${xpath}`;
      window.location.assign(href);
    },
    getReason(reason, attribute) {
      const r = this.reasons[reason].description || reason;
      return cldrText.subTemplate(r, { attribute });
    },
    closeInheritanceExplainer() {
      this.visible = false;
    },
  },
};
</script>

<style scoped>
.xpath {
  font-size: smaller;
}

.xpath-hidden {
  opacity: 0.4;
}

.locale {
  padding-right: 0.5em;
}

.reason {
  padding-left: 0.5em;
}
</style>
