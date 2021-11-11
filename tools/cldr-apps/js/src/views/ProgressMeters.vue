<template>
  <section id="ProgressMeters" v-if="!hide">
    <a-progress
      :percent="sectionBar.percent"
      :title="sectionBar.title"
      strokeColor="blue"
      type="circle"
      :width="32"
    />
    &nbsp;
    <a-progress
      :percent="voterBar.percent"
      :title="voterBar.title"
      strokeColor="orange"
      type="circle"
      :width="32"
    />
    &nbsp;
    <a-progress
      :percent="localeBar.percent"
      :title="localeBar.title"
      strokeColor="green"
      type="circle"
      :width="32"
    />
  </section>
</template>

<script>
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrStatus from "../esm/cldrStatus.js";

export default {
  props: [],
  data() {
    return {
      hide: true,
      locale: null,
      localeName: null,
      level: null,
      sectionBar: {
        description: "Your voting in this section",
        votes: -1,
        total: -1,
        percent: -1,
        title: "",
      },
      voterBar: {
        description: "Your voting in this locale",
        votes: -1,
        total: -1,
        percent: -1,
        title: "",
      },
      localeBar: {
        description: "Completion for all vetters in this locale",
        votes: -1,
        total: -1,
        percent: -1,
        title: "",
      },
    };
  },

  methods: {
    setHidden(hidden) {
      this.hide = hidden;
    },

    setLevel(level) {
      this.level = level;
    },

    setLocale(locale) {
      this.locale = locale;
      this.localeName = cldrLoad.getLocaleName(locale);
      console.log(
        "VotingCompletion setting temporary placeholder values 33/100 for locale completion"
      );
      this.updateLocaleVotesAndTotal(33, 100); // TODO: fetch data for localeBar
    },

    updateSectionVotesAndTotal(votes, total) {
      this.makeBar(this.sectionBar, votes, total);
    },

    updateVoterVotesAndTotal(votes, total) {
      this.makeBar(this.voterBar, votes, total);
    },

    updateLocaleVotesAndTotal(votes, total) {
      this.makeBar(this.localeBar, votes, total);
    },

    makeBar(bar, votes, total) {
      bar.votes = votes;
      bar.total = total;
      bar.percent = this.friendlyPercent(votes, total);
      bar.title =
        `${bar.description}:` +
        "\n" +
        `${votes} / ${total} ≈ ${bar.percent}%` +
        "\n" +
        `Locale: ${this.localeName} (${this.locale})` +
        "\n" +
        `Coverage: ${this.level}`;
    },

    friendlyPercent(votes, total) {
      if (!total) {
        // The task is finished since nothing needed to be done
        // Do not divide by zero (0 / 0 = NaN%)
        return 100;
      }
      if (!votes) {
        return 0;
      }
      // Do not round 99.9 up to 100
      const floor = Math.floor((100 * votes) / total);
      if (!floor) {
        // Do not round 0.001 down to zero
        // Instead, provide indication of slight progress
        return 1;
      }
      return floor;
    },
  },

  computed: {
    console: () => console,
  },
};
</script>

<style scoped>
#ProgressMeters {
  margin-left: 1em;
  margin-right: 1em;
}
</style>
