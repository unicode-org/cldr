<template>
  <div>
    <a-steps :current="current">
      <a-step title="Choose Target user" @click="goBack()" />
      <!-- 0  - done in userlist-->
      <a-step title="Choose Source user" @click="cancel()" />
      <!-- 1 -->
      <a-step
        title="Choose Source locale"
        @click="
          if (transferFromId && transferToId) {
            fromLocale = null;
            toLocale = null;
            current = 2;
            started = null;
          }
        "
      />
      <!-- 2 -->
      <a-step
        title="Choose Target locale"
        @click="
          if (transferFromId && transferToId && fromLocale) {
            toLocale = null;
            current = 3;
            started = null;
          }
        "
      />
      <!-- 3 -->
      <a-step title="Copy Votes" />
      <!-- 4 -->
    </a-steps>
    <h2 v-if="transferToId && transferToInfo && !transferFromId">
      Copying votes to {{ transferToInfo.name }} {{ transferToInfo.email }}
    </h2>
    <a-spin v-if="!transferToInfo" :delay="500"
      ><i>Loading user #{{ transferToId }}</i></a-spin
    >
    <a-spin v-if="!chooseUserList" :delay="500"
      ><i>Loading user list</i></a-spin
    >
    <div v-if="!transferFromId">
      <h2>Choose the Copy Source (the user to copy votes <i>from</i>):</h2>
      <ul>
        <li v-for="user in chooseUserList" v-bind:key="user.id">
          <button @click="transferFrom(user.id)">
            <i class="glyphicon glyphicon-user" />
            {{ user.name }} ({{ user.email }})
          </button>
        </li>
      </ul>
    </div>
    <div v-if="transferFromId && transferToId">
      <h3>
        Source User:
        <i class="glyphicon glyphicon-user" />
        {{ fullUserList[transferFromId]?.name }}
        {{ fullUserList[transferFromId]?.email }} #{{ transferFromId }}
      </h3>
      <button v-if="!started" v-on:click="cancel()">
        Choose a different source user
      </button>
      <div v-if="!fromLocale && votesCache[transferFromId]">
        <h1>Choose a source locale:</h1>
        <div>
          <button
            v-for="loc in votesCache[transferFromId].locales"
            v-bind:key="loc.locale"
            @click="
              fromLocale = toLocale = loc.locale;
              current = 3;
            "
          >
            Choose <code>{{ loc.locale }}</code> {{ loc.localeDisplayName }}
          </button>
        </div>
      </div>
      <h2 v-if="fromLocale && votesCache[transferFromId]">
        Source Locale: {{ fromLocale }}
      </h2>
      <div v-if="votesCache[transferFromId]">
        <ul v-if="!votesCache[transferFromId].loading">
          <li
            v-for="k in Object.keys(votesCache[transferFromId].releaseData)"
            v-bind:key="transferFromId + ':' + k"
          >
            {{ k }}:
            <ul>
              <li
                v-for="loc in votesCache[transferFromId].releaseData[k]"
                v-bind:key="loc.locale"
              >
                <h4 v-if="!fromLocale || fromLocale == loc.locale">
                  <code>{{ loc.locale }}</code> — {{ loc.localeDisplayName }}:
                  {{ loc.count }} vote(s)
                </h4>
              </li>
            </ul>
          </li>
        </ul>
        <a-spin
          v-if="votesCache[transferFromId]?.loading"
          :delay="500"
        ></a-spin>
      </div>
      <h3>
        Target User:
        <i class="glyphicon glyphicon-user" />
        {{ transferToInfo?.name }} {{ transferToInfo?.email }} #{{
          transferToId
        }}
      </h3>
      <hr />

      <h1 v-if="!started && toLocale">
        Confirm the source and target information
      </h1>

      <a-alert
        v-if="toLocale && fromLocale != toLocale"
        type="warning"
        message="Source and Target locales differ"
        show-icon
      />

      <a-input
        v-if="fromLocale"
        v-model:value="toLocale"
        placeholder="Enter a target locale"
        style="width: 20em"
      >
        <template #addonBefore> Target Locale: </template>
      </a-input>
      <p />
      <b>
        <button
          v-on:click="transfer"
          v-if="
            fromLocale &&
            toLocale &&
            !started &&
            votesCache[transferFromId] &&
            !votesCache[transferFromId].loading
          "
        >
          Copy to “{{ toLocale }}”
        </button>
      </b>

      <p />

      <a-alert
        v-if="started"
        type="info"
        message="Copy Status"
        v-model:description="started"
        show-icon
        @click="
          started = null;
          current = 3;
        "
      />
    </div>
    <hr />
  </div>
</template>

<script>
import { notification } from "ant-design-vue";
import { ref, reactive } from "vue";
import * as cldrLoad from "../esm/cldrLoad.js";
import * as setUtils from "../esm/setUtils.mjs";

function errBox(message) {
  console.error("TransferVotes.vue: " + message);
  notification.error({
    description: "Problem with Copy Votes",
    message,
    placement: "topLeft",
    duration: 0,
  });
}

export default {
  setup() {
    const qparams = new URLSearchParams(document.location.search);
    const transferToId = qparams.get("transferTo");
    const transferFromId = qparams.get("transferFrom") || 0; // for URL based updating
    let current = 1;
    return {
      current: ref(current),
      transferToId: ref(transferToId),
      chooseUserList: reactive({}),
      fullUserList: reactive({}),
      votesCache: reactive({}),
      transferToInfo: ref(null),
      transferFromId: ref(transferFromId),
      started: ref(null),
      fromLocale: ref(null),
      toLocale: ref(null),
    };
  },
  created() {
    this.fetchData();
  },
  methods: {
    cancel() {
      this.started = null;
      this.transferFromId = 0;
      this.current = 1;
      this.fromLocale = null;
      this.toLocale = null;
    },
    goBack() {
      // go back to list users page
      this.current = 0;
      window.location.replace("v#list_users");
    },
    fetchFromVotes(id) {
      const locmap = cldrLoad.getTheLocaleMap();
      // Don't warn if the id was already different from the user choice
      const quiet = id != this.transferFromId;
      if (!this.votesCache[id]) {
        this.votesCache[id] = { loading: true };
        const sessionId = this.$cldrOpts.cldrStatus.getSessionId();
        fetch(`SurveyAjax?what=user_oldvotes&s=${sessionId}&old_user_id=${id}`)
          .then((r) => r.json())
          .then((o) => {
            return o?.user_oldvotes?.data;
          })
          .then((raw) => {
            const releaseData = raw || { no_votes: "[]" }; // handle case where there is no data
            const allLocales = new Set();
            for (const release of Object.keys(releaseData)) {
              releaseData[release] = JSON.parse(releaseData[release]).map(
                ([count, locale, localeDisplayName]) => ({
                  count,
                  locale,
                  localeDisplayName,
                })
              );
            }
            for (const release of Object.keys(releaseData)) {
              for (const { locale } of releaseData[release]) {
                allLocales.add(locale);
              }
            }
            const coll = new Intl.Collator([]);
            const locales = setUtils
              .asList(allLocales)
              .map((locale) => ({
                locale,
                localeDisplayName: locmap.getLocaleName(locale),
              }))
              .sort((a, b) =>
                coll.compare(a.localeDisplayName, b.localeDisplayName)
              );
            this.votesCache[id] = { releaseData, locales };
            console.log(
              `loaded vote data for #${id}: ${Object.keys(
                this.votesCache[id]
              ).join(",")}`
            );
            if (id != this.transferFromId && !quiet) {
              // don't print this on a simple mouseover
              console.warn(
                `Note: loaded from data for #${id} but from user is #${this.transferFromId}`
              );
            }
          })
          .catch((e) => {
            console.error(e);
            errBox(`Loading #${id} votes: ${e.message}`);
          });
      }
    },
    transferFrom(id) {
      this.current = 2;
      this.transferFromId = id;
      // Load if not loaded
      this.fetchFromVotes(id);
    },
    transfer() {
      const locmap = cldrLoad.getTheLocaleMap();
      if (!locmap.getLocaleInfo(this.fromLocale)) {
        errBox(`Invalid Source locale ${this.fromLocale}`);
        return;
      }
      if (!locmap.getLocaleInfo(this.toLocale)) {
        errBox(`Invalid Target locale ${this.toLocale}`);
        return;
      }
      this.started = `Begin copy from ${this.fromLocale} to ${this.toLocale}`;
      const sessionId = this.$cldrOpts.cldrStatus.getSessionId();
      fetch(
        `SurveyAjax?what=user_xferoldvotes&s=${sessionId}&from_user_id=${this.transferFromId}&to_user_id=${this.transferToId}&from_locale=${this.fromLocale}&to_locale=${this.toLocale}`
      )
        .then((r) => r.json())
        .then((d) => {
          const xferStatus = d?.user_xferoldvotes;
          if (!xferStatus) {
            this.started = `Done, result= ${JSON.stringify(d)}`;
          } else {
            const {
              result_count,
              from_user_id,
              from_locale,
              to_user_id,
              to_locale,
              tables,
            } = xferStatus;
            this.started =
              `Done, copied ${result_count} vote(s) from` +
              ` ${from_user_id}/${from_locale} to ` +
              `${to_user_id}/${to_locale} (${
                locmap.getLocaleInfo(to_locale)?.name
              }) in table(s) ` +
              `${tables}`;
          }
          this.current = 4;
        })
        .catch((e) => {
          errBox(e);
          this.status = e.toString();
        });
    },
    getUser(id) {
      return this.fullUserList[id] || {};
    },
    fetchData() {
      if (this.transferFromId) {
        // ID set from URL. fetch its data.
        this.transferFrom(this.transferFromId);
      }
      const sessionId = this.$cldrOpts.cldrStatus.getSessionId();
      fetch(`SurveyAjax?what=user_list&s=${sessionId}`)
        .then((r) => r.json())
        .then(({ shownUsers }) => shownUsers)
        // skip our own user
        .then((users) => {
          for (const user of users) {
            const { id } = user;
            this.fullUserList[id] = user;
          }
          const coll = new Intl.Collator([], { sensitivity: "base" });
          this.chooseUserList = users
            .filter(({ id }) => id != this.transferToId)
            .sort(({ name: namea }, { name: nameb }) =>
              coll.compare(namea, nameb)
            );
          this.transferToInfo = this.fullUserList[this.transferToId];
          if (!this.transferToInfo) {
            errBox(
              `Error: Can’t find user #${this.transferToId} to copy votes to. Please click Back and try again.`
            );
          }
        })
        .catch((err) => errBox(err.toString()));
    },
  },
};
</script>

<style scoped></style>
>
