<template>
  <h2>XPath Calculator</h2>
  <p class="helpHtml">
    <em>Instructions:</em> Enter an XPath, such as
    <kbd>//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator</kbd>
    into the "XPath string" field, or, enter a hex or decimal XPath id, such as
    <kbd>1d142c4be7841aa7</kbd> or <kbd>15305</kbd> into the corresponding
    field. Then press the <kbd>Tab</kbd> key. The other fields will be filled
    in.
  </p>
  <label for="str">XPath string: </label
  ><input id="str" v-model="str" v-on:change="lookupPath" size="160" />
  <br />
  <label for="hex">XPath hex id: </label
  ><input id="hex" v-model="hex" v-on:change="lookupPath" size="32" />
  <br />
  <label for="dec">XPath decimal id: </label
  ><input id="dec" v-model="dec" v-on:change="lookupPath" size="8" />
  <div id="xpathStatus">
    {{ xpathStatus }}
  </div>

  <h2>What Is...</h2>
  <p class="helpHtml">
    <em>Instructions:</em> Enter a code or a portion of a name in the "What is"
    field, such as <kbd>jgo</kbd> or <kbd>English</kbd>, and press the
    <kbd>Tab</kbd> key. A list of matching codes will be shown.
  </p>
  <label for="whatis">What is: </label>
  <input id="whatis" v-model="whatis" v-on:change="lookupWhatis" />
  <label for="loc">Base Locale: </label>
  <input id="loc" name="loc" v-model="baseLocale" />
  <div id="whatisAnswer">{{ whatisAnswer }}</div>
</template>

<script>
import * as cldrAjax from "../../../src/main/webapp/js/esm/cldrAjax.js";

export default {
  data() {
    return {
      baseLocale: "en_US",
      dec: "",
      hex: "",
      str: "",
      whatis: "",
      whatisAnswer: "",
      xpathStatus: "",
    };
  },

  methods: {
    /**
     * Look up the entered xpath string, hex code, or decimal code
     */
    lookupPath(event) {
      const el = event.target;
      const from = el.id; // str, hex, or dec
      const v = el.value;
      if (v.length == 0) {
        return;
      }
      this.xpathStatus = "Looking up " + from + " " + v + "...";
      const xhrArgs = {
        url: this.getLookupPathAjaxUrl(from, v),
        postData: from === "str" ? { str: v } : null,
        handleAs: "json",
        load: this.pathLoadHandler,
        error: (err) => (this.xpathStatus = "❌  " + err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    pathLoadHandler(json) {
      if (json.err) {
        // TODO: given "12345678", don't report "Status 500 Internal Server Error; URL: api/xpath/dec/12345678"!
        // Caused by: java.lang.InternalError: Exceeded max 768000 @ 12345678
        // at org.unicode.cldr.web.IntHash.get(IntHash.java:61)
        // The server should be more robust, and catch such exceptions, or not throw them in the first place
        this.xpathStatus = "❌  " + json.message;
      } else {
        this.str = json.xpath;
        this.hex = json.hexId;
        this.dec = json.decimalId;
        this.xpathStatus = "✅";
      }
    },

    /**
     * Look up the entered "What is" string
     */
    lookupWhatis() {
      if (!this.whatis) {
        this.whatisAnswer = "";
        return;
      }
      this.whatisAnswer = "Looking up " + this.whatis + "...";
      const xhrArgs = {
        url: this.getWhatisAjaxUrl(),
        // TODO: the back end should return json, not text (html)
        handleAs: "text",
        load: (answer) => (this.whatisAnswer = answer),
        error: (err) => (this.whatisAnswer = "Error: " + err),
      };
      cldrAjax.sendXhr(xhrArgs);
    },

    getLookupPathAjaxUrl(from, v) {
      if (from === "str") {
        // request will be post; v will be in body
        return "api/xpath/str";
      } else {
        // request will be get
        return "api/xpath/" + from + "/" + v;
      }
    },

    getWhatisAjaxUrl() {
      const p = new URLSearchParams();
      p.append("loc", this.baseLocale);
      p.append("q", this.whatis);
      // TODO: not jsp! Maybe api/whatis
      return "browse_results.jsp?" + p.toString();
    },
  },

  computed: {
    console: () => console,
  },
};
</script>

<style scoped>
#whatis {
  font-size: x-large;
}
.helpHtml {
  margin: 1em;
}
</style>
