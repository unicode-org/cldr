<template>
  <a-popover
    v-model:visible="searchShown"
    placement="leftBottom"
    trigger="click"
  >
    <template #content>
      <cldr-searchpanel ref="searchPanel" />
      <a @click="hide" title="Close">Close</a>
    </template>
    <a-button type="default" title="Search"
      ><span class="glyphicon glyphicon-search tip-log"
    /></a-button>
  </a-popover>
</template>

<script lang="js">
import SearchPanel from "./SearchPanel.vue";
import { ref } from "vue";

export default {
    setup() {
        const searchShown = ref(false);
        const hide = () => searchShown.value = false;
        return {
            searchShown,
            hide,
        };
    },

    components: {
        "cldr-searchpanel": SearchPanel,
    },

    methods: {
        stop: function() {
            // TODO: not working.
            // this.$refs.searchPanel.stop(); // tell panel to stop if popover closed
        }
    },

    watch: {
        searchShown: function() {
            if (!this.searchShown.value) {
                this.stop();
            }
        }
    },
};
</script>

<style scoped></style>
