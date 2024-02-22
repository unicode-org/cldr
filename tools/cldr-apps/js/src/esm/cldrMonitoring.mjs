import { datadogRum } from "@datadog/browser-rum";
import { datadogLogs } from "@datadog/browser-logs";

/** called by index.js */
function init() {
  if (window.dataDogClientToken) {
    datadogLogs.init({
      clientToken: window.dataDogClientToken,
      site: "us5.datadoghq.com",
      forwardErrorsToLogs: true,
      sessionSampleRate: 100,
    });

    datadogRum.init({
      applicationId: window.dataDogAppId,
      clientToken: window.dataDogClientToken,
      site: "us5.datadoghq.com",
      service: "surveytool",
      env: window.dataDogEnv,
      version: "r" + window.dataDogSha,
      sessionSampleRate: 100,
      sessionReplaySampleRate: 20,
      trackUserInteractions: true,
      trackResources: true,
      trackLongTasks: true,
      defaultPrivacyLevel: "allow",
      allowedTracingUrls: [
        {
          match: /https:\/\/.*\.unicode\.org/,
          propagatorTypes: ["tracecontext"],
        },
      ],
    });
  }
}

export { init };
