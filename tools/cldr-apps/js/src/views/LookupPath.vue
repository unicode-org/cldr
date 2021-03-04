<template>
  <form action="">
    <label>Base Locale: <input name="loc" value="en-US" /></label
    ><input type="submit" value="change" />
  </form>
  <br />
  <hr />
  <table>
    <tbody>
      <tr>
        <td style="border-right: 1px solid gray">
          What is...
          <input
            id="whatis"
            v-on:change="lookup_whatis()"
            style="font-size: x-large"
          />
        </td>
        <td>
          <b>xpath calculator - </b><br />
          <label
            >XPath:<input
              id="xpath"
              v-on:change="lookup_xpath('xpath')"
              size="160" /></label
          ><br />
          <label
            >PathHeader:<input
              id="pathheader"
              size="160"
              disabled="true" /></label
          ><br />
          <label
            >XPath strid:<input
              id="strid"
              v-on:change="lookup_xpath('strid')"
              size="32"
          /></label>
          <label
            >XPathID (dec#)<input
              id="xpathid"
              v-on:change="lookup_xpath('xpathid')"
              size="8"
          /></label>
          <div id="xpath_answer" style="font-style: italic">
            enter a value and hit the tab key to begin
          </div>
        </td>
      </tr>
    </tbody>
  </table>
  <div id="whatis_answer"></div>
  <hr />
  <div class="helpHtml" style="margin: 2em">
    <h4>Instructions:</h4>
    <b>What Is...</b>: Enter a code or a portion of a name in the "What Is"
    field, such as "jgo" or "English", and press the Tab key. A list of matching
    codes will be shown.
    <p>
      <b>XPath Calculator</b>: Enter an XPath, such as
      <tt>//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator</tt>
      into the XPath field, and press the Tab key. Or, enter an XPath strid,
      such as <tt>1d142c4be7841aa7</tt> into the XPath strid field and press the
      Tab key. The other fields (if applicable) will be filled in.
    </p>
  </div>
</template>

<script>
import * as cldrAjax from "../../../src/main/webapp/js/esm/cldrAjax.js";

export default {
  methods: {
    lookup_whatis: function () {
      var v = document.getElementById("whatis").value;
      var r = document.getElementById("whatis_answer");
      if (v.length == 0) {
        r.innerHTML = "";
        return;
      }
      r.innerHTML = "<i>Looking up " + v + "...</i>";
      cldrAjax.sendXhr({
        url: "/cldr-apps/browse_results.jsp?loc=en_US&q=" + v,
        load: function (h) {
          r.innerHTML = h;
        },
        error: function (err) {
          r.innerHTML = "Error: " + err;
        },
      });
    },

    lookup_xpath: function (from) {
      var v = document.getElementById(from).value;
      if (v.length == 0) {
        return;
      }
      var r = document.getElementById("xpath_answer");
      r.innerHTML = "<i>Looking up xpath " + v + "...</i>";
      const apiMap = {
        strid: "hex",
        xpathid: "dec",
      };

      cldrAjax.sendXhr({
        url: "api/xpath/" + apiMap[from] + "/" + v,
        handleAs: "json",
        load: function (h) {
          if (h.err) {
            r.innerHTML = h.message;
          } else {
            function updateIf(id, txt) {
              var something = document.getElementById(id);
              if (!txt || txt == -1) {
                txt = "";
              }
              if (something != null) {
                something.value = txt;
              }
            }
            updateIf("xpathid", h.decimalId);
            updateIf("xpath", h.xpath);
            updateIf("strid", h.hexId);
            updateIf("pathheader", h.pathheader);
          }
        },
        error: function (err) {
          r.innerHTML = "Error: " + err;
        },
      });
    },
  },
};
</script>
