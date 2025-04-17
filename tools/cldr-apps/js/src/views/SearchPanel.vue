<template>
  <div>
    <a-input-search
      v-model:value="searchText"
      placeholder="Search…"
      v-model:loading="searchLoading"
    />
    <a-list
      v-if="searchResults"
      :pagination="pagination"
      item-layout="vertical"
      :data-source="searchResults"
    >
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta :description="item.xpath">
            <template #title>
              <a :href="item.link">{{ item.title }}</a>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script lang="js">
import { ref } from "vue";
import { SearchClient } from "../esm/cldrSearch.mjs";
import { getCurrentLocale } from "../esm/cldrStatus.mjs";

import { notification } from "ant-design-vue";

/**
 * The text being searched
 */
const searchText = ref("");
/**
 * True if we are loading
 */
const searchLoading = ref(false);
/**
 * Results!
 */
const searchResults = ref(null);

const pagination = ref(false);
/**
 * client for searching
 */
let searchClient = null;

export default {
    setup() {
        searchClient = new SearchClient({
            onLoading: (v) => searchLoading.value = v,
            onResults: (v) => {
              if (!v) {
                searchResults.value = null;
                return;
              }
              searchResults.value = v.map(({
                context, locale, xpstrid, xpath,
              }) => ({
                title: context,
                link: `#/${getCurrentLocale() || locale}//${xpstrid}`,
                xpath,
              }));
              if (searchResults.value.length > 5) {
                pagination.value = {
                  pageSize: 5,
                  onChange: pageNo => {},
                };
              } else {
                pagination.value = false;
              }
            },
          onError: (e) => {
            searchLoading.value = false;
            notification.error({
              message: 'Search error',
              description: `${e}`,
              placement: "topLeft",
            });
          },
        });
        return {
            searchText,
            searchLoading,
            searchResults,
            pagination,
        }
    },
    watch: {
        // feed searchText into the searchClient
        searchText: () => searchClient.update(searchText.value),
    },
    methods: {
        // TODO: needs to be an event from the parent?
        searchStop: () => searchClient.stop(),
    },
}
</script>

<style></style>
