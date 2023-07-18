<template>
  <div id="home">
    <!-- if $specialPage == retry then we have reached this page 'directly' -->
    <h1 v-if="$specialPage != 'retry'" class="hang">Loading the SurveyTool…</h1>
    <a-steps direction="vertical" :status="status" :current="current">
      <a-step title="Checking on things…" />
      <!-- 1 -->
      <a-step title="Trying to start the SurveyTool" />
      <!-- 2 -->
      <a-step title="The SurveyTool is starting" />
      <!-- 3 -->
      <a-step title="The SurveyTool has started!" />
      <!-- 4 -->
      <a-step title="Redirecting you to the SurveyTool…" />
      <!-- 5 -->
    </a-steps>
    <a-spin size="large" :delay="1000" />
    <ul>
      <li v-if="fetchErr" class="st-sad">
        I’m having some trouble contacting the SurveyTool.
      </li>
      <li v-if="!statusData.triedToStartUp && attemptedLoadErr">
        Still trying to get the SurveyTool to start.
      </li>
      <li v-if="statusData.status && statusData.status.isBusted" class="st-sad">
        The SurveyTool is not running, sorry. Here is the reason:
        <p class="st-reason">
          {{ statusData.status.isBusted }}
        </p>
      </li>
    </ul>
    <!-- <button v-on:click="fetchStatus()">Retry</button> -->
  </div>
</template>

<script>
import { run } from "../esm/cldrGui.mjs";
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
  computed: {
    current() {
      if (!this.statusData) {
        return 1; // checking
      } else if (!this.statusData.triedToStartUp) {
        return 2; // trying to start
      } else if (
        this.statusData.triedToStartUp &&
        !(this.statusData.status && this.statusData.status.isSetup)
      ) {
        return 3; // starting up
        //  step 4 started?
      } else if (
        this.statusData.triedToStartUp &&
        this.statusData.status &&
        this.statusData.status.isSetup
      ) {
        return 5; // redirecting
      }
    },
    status() {
      if (
        this.statusData &&
        this.statusData.status &&
        this.statusData.status.isBusted
      ) {
        return "error";
      } else if (this.attemptedLoadErr) {
        return "wait";
      } else if (this.attemptedLoadCount > 2) {
        return "wait";
      } else if (this.statusData.status && this.statusData.status.isSetup) {
        return "finish";
      } else {
        return "process";
      }
    },
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
            if (window.location.hash.startsWith(`#${this.$specialPage}`)) {
              // If the URL actually goes to this page (as in '#retry'),
              // then head back to the main page.
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
              // Otherwise, cause a reload, which should take the user back where they were.
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
  margin-top: 10em;
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
