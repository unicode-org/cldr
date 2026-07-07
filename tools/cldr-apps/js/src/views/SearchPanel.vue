<template>
  <a-drawer
    placement="right"
    @after-open-change="afterOpenChange"
    v-model:open="searchShown"
    title="Search"
    :width="800"
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
    <a-alert v-if="searchTruncated" type="warning" message="Too many results, so
    we’re only showing some of them."" show-icon />

    <a-spin v-if="searchProcessing != 0" />
    <span v-if="searchProcessing != 0">{{ searchProcessing }} items found</span>
    <a-list
      v-if="searchResults"
      :pagination="pagination"
      item-layout="vertical"
      :data-source="searchResults"
    >
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta :description="item.description">
            <!-- item.xpath-->
            <template #title>
              <div class="itemlink">
                <a :href="item.link">{{ item.title }}</a>
              </div>
              <div class="itemConfidence">
                <a-progress
                  :percent="item.confidence"
                  :steps="5"
                  size="small"
                  :showInfo="false"
                  stroke-color="#52c41a"
                />
              </div>
              <div
                class="otherlink"
                v-if="item.link !== item.llink && item.locale !== 'root'"
              >
                <a :href="item.llink">(»{{ item.localeName }})</a>
              </div>
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
import { getXpathMap } from "../esm/cldrSurvey.mjs";

import * as cldrNotify from "../esm/cldrNotify.mjs";
import { comparePathHeaders } from "../esm/cldrXpathMap.mjs";

/**
 * The text being searched
 */
const searchText = ref("");
/**
 * True if we are loading
 */
const searchLoading = ref(false);
const searchProcessing = ref(0);
const searchTruncated = ref(false);
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
          onResults({ results, isTruncated }) {
              searchTruncated.value = isTruncated;
              const v = results;
              if (!v) {
                searchResults.value = null;
                return;
              }
              if (v.length > 5) {
                pagination.value = {
                  pageSize: 5,
                  onChange: pageNo => {},
                };
              } else {
                pagination.value = false;
              }
              mapSearchResults(v)
              .then(results => searchResults.value=results)
              .catch(e => {
                cldrNotify.exception(e, `Processing Search Results`);
                searchLoading.value = false;
                searchProcessing.value = 0;
              });
            },
          onError: (e) => {
            searchLoading.value = false;
            cldrNotify.exception(e, `Search Error`);
          },
        });
        return {
            pagination,
            searchLoading,
            searchProcessing,
            searchResults,
            searchShown,
            searchText,
            searchTruncated,
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
        afterOpenChange: () => {/*console.log('open')*/},
    },
}

async function mapSearchResults(v) {
  if (!v.length) return [];
  // preload all xpath entries for sort
  const xpathMap = getXpathMap();
  searchProcessing.value = v.length;
  console.log(`Updating PH for ${v.length} items`);
  const seenXpath = new Set();
  const lookups = [];
  const newItems = [];
  for (let i=0; i<v.length; i++) {
    // skip duplicate xpaths
    if (!seenXpath.has(v[i].xpath)) {
      seenXpath.add(v[i].xpath);
      lookups.push(new Promise((resolve) =>
      {
        xpathMap.getPathHeader(v[i].xpath)
        .then(ph => {
          newItems.push({...v[i], ph});
          resolve();
        });
      }));
    }
  }
  // wait for all PH lookups
  await(Promise.all(lookups));
  // now we can sort
  newItems.sort((a, b) => {
    if (a.confidence !== b.confidence) {
      return b.confidence - a.confidence;
    }
    return comparePathHeaders(a.ph, b.ph);
  })
  console.log(`PH updated for ${newItems.length} items (minus dup)`);
  searchProcessing.value = 0;

  return newItems.map(({
    context, locale, xpstrid, xpath, ph, confidence,
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
    // ph: JSON.stringify(ph),
    description: xpathMap.formatPathHeader(ph),
    confidence,
  }));
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

.itemConfidence {
  float: right;
}
</style>
