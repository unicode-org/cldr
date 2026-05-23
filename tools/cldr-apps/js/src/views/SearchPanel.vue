<template>
  <a-drawer
    placement="right"
    @after-open-change="afterOpenChange"
    v-model:open="searchShown"
    title="Search"
  >
    <!--
    class="custom-class"
    root-class-name="root-class-name"
    :root-style="{ color: 'blue' }"
    style="color: red"

  -->
    <a
      target="CLDR_ST_DOCS"
      href="https://cldr.unicode.org/translation#searching-in-the-survey-tool"
      title="Help"
      >Help</a
    >
    | <a @click="hide" title="Close">Close</a>

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
              <li class="itemlink">
                <a :href="item.link">{{ item.title }}</a>
              </li>
              <li
                class="otherlink"
                v-if="item.link !== item.llink && item.locale !== 'root'"
              >
                <a :href="item.llink">(See in {{ item.localeName }})</a>
              </li>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </a-drawer>
</template>

<script lang="js">
import { ref } from "vue";
import { getTheLocaleMap } from "../esm/cldrLoad.mjs";
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

const searchShown = ref(false);

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
                // link to current locale
                link: `#/${getCurrentLocale() || locale}//${xpstrid}`,
                // requested locale
                locale,
                // name of requested locale
                localeName: getTheLocaleMap()?.getLocaleName(locale) || locale,
                // link in the specified locale
                llink: `#/${locale || getCurrentLocale() || "en"}//${xpstrid}`,
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
            searchShown,
        }
    },
    watch: {
        // feed searchText into the searchClient
        searchText: () => searchClient.update(searchText.value),
    },
    methods: {
        // TODO: needs to be an event from the parent?
        searchStop: () => searchClient.stop(),
        open: () => searchShown.value = true,
        afterOpenChange: () => console.log('open'),
    },
}
</script>

<style scoped>
li.itemlink,
li.otherlink {
  list-style: none;
}

.itemlink a,
.otherlink a {
  text-decoration: underline;
}
</style>
