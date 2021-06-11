<template>
  <div id="home">
    <!-- if $specialPage == retry then we have reached this page 'directly' -->
    <h1 v-if="$specialPage != 'retry'" class="hang">
      Waiting for the SurveyTool to start up…
    </h1>
    <h1>
      <a-spin size="large" :delay="1000" />
    </h1>
    <ol>
      <li v-if="!statusData">Checking…</li>
      <!-- this shows while we are waiting for status data -->
      <li v-if="fetchErr" class="st-sad">
        I’m having some trouble contacting the SurveyTool.
      </li>
      <li v-if="statusData">Checking on SurveyTool status…</li>
      <li v-if="!statusData.triedToStartUp">SurveyTool hasn’t started yet.</li>
      <li v-if="!statusData.triedToStartUp && attemptedLoadErr">
        Still trying to get the SurveyTool to start.
      </li>
      <li
        v-if="
          !statusData.triedToStartUp && attemptedLoadCount && !attemptedLoadErr
        "
      >
        Trying to start the SurveyTool…
      </li>
      <li
        v-if="
          statusData.triedToStartUp &&
          !(statusData.status && statusData.status.isSetup)
        "
      >
        The SurveyTool is trying to start.
      </li>
      <li
        v-if="
          statusData.triedToStartUp &&
          statusData.status &&
          statusData.status.isSetup
        "
      >
        The SurveyTool started up.
      </li>
      <li
        v-if="statusData.status && statusData.status.isSetup"
        class="st-happy"
      >
        Redirecting you to the SurveyTool!
      </li>
      <li v-if="statusData.status && statusData.status.isBusted" class="st-sad">
        The SurveyTool is not running, sorry. Here is the reason:
        <p class="st-reason">
          {{ statusData.status.isBusted }}
        </p>
      </li>
    </ol>

    <!-- <button v-on:click="fetchStatus()">Retry</button> -->
  </div>
</template>

<script>
import { run } from "../esm/cldrGui.js";
import { notification } from "ant-design-vue";

export default {
  data: function () {
    return {
      statusData: {},
      attemptedLoadCount: 0,
      attemptedLoadErr: null,
      fetchCount: 0,
      fetchErr: null,
    };
  },
  created: function () {
    this.fetchStatus();
  },
  methods: {
    fetchStatus: function () {
      const SECONDS_IN_MS = 1000;
      // timeouts
      const RETRY_ON_FETCH_ERR = 15 * SECONDS_IN_MS; // When we couldn't talk to server
      const NORMAL_RETRY = 5 * SECONDS_IN_MS; // "Normal" retry: starting or about to start
      const BUSTED_RETRY = 60 * SECONDS_IN_MS; // ST is down
      fetch("SurveyAjax?what=status")
        .then(
          (r) => r.json(),
          (err) => {
            this.fetchErr = err;
            this.statusData = {};
            return {}; // no data, will fall through
          }
        )
        .then((data) => {
          if (!data) {
            window.setTimeout(this.fetchStatus.bind(this), RETRY_ON_FETCH_ERR);
            return;
          }
          this.fetchErr = null;
          this.fetchCount++;
          this.statusData = data;
          if (!data.triedToStartUp) {
            // attempt to fetch /survey which will cause ST to startup
            console.log("Attempting fetch of /cldr-apps/survey");
            fetch("survey").then(
              () => this.attemptedLoadCount++,
              (err) => this.attemptedLoadErr
            );
          }
          if (data.status && data.status.isSetup) {
            if (this.$specialPage == "retry") {
              // immediately head back to the main page.
              window.location.replace("v#");
              setTimeout(
                () =>
                  notification.success({
                    message: "Reconnected",
                    description: "You have been reconnected to the SurveyTool.",
                  }),
                4000
              );
              run().catch((e) => {
                // We can get here if run() was not able to boot the page
                // That's OK, it may have been a waiting page. Reload should
                // clear it up.
                console.log(
                  `run() threw an error, so we will do a page reload. Err was: ${e}`
                );
                window.location.reload();
              });
            } else {
              window.setTimeout(() => window.location.reload(), NORMAL_RETRY);
            }
          } else if (data.status && data.status.isBusted) {
            window.setTimeout(this.fetchStatus.bind(this), BUSTED_RETRY); // try in a minute
          } else {
            window.setTimeout(this.fetchStatus.bind(this), NORMAL_RETRY);
          }
        });
    },
  },
};
</script>

<style scoped>
#home {
  /*
 * for bootstrap
 */
  padding-top: 60px;
  padding-left: 1em;
  padding-right: 1em;
  padding-bottom: 1em;
}

.st-happy {
  font-weight: bold;
  color: limegreen;
}

.st-sad {
  font-style: italic;
  border: 1px dashed red;
  color: darkred;
}

.st-reason {
  background-color: lightyellow;
  border: 1px solid darkred;
  padding: 2em;
  white-space: pre-wrap;
  font-size: larger;
  margin: 1em;
}
</style>
