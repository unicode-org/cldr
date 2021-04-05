import * as cldrTest from "./TestCldrTest.js";

import * as cldrErrorSubtypes from "../src/esm/cldrErrorSubtypes.js";

export const TestCldrErrorSubtypes = "ok";

const assert = chai.assert;

describe("cldrErrorSubtypes.getHtml", function () {
  const json = {
    err: "",
    COMMENT: "#",
    BEGIN_MARKER: "-*- BEGIN CheckCLDR.Subtype Mapping -*-",
    unhandled: {
      names: [
        "abbreviatedDateFieldTooWide",
        "asciiCharactersNotInCurrencyExemplars",
        "asciiCharactersNotInMainOrAuxiliaryExemplars",
      ],
      strings: [
        "abbreviated date field too wide",
        "ascii characters not in currency exemplars",
        "ascii characters not in main or auxiliary exemplars",
      ],
    },
    urls: [
      {
        names: ["displayCollision"],
        strings: ["display collision"],
        url:
          "https://sites.google.com/site/cldr/translation/short-names-and-keywords",
        status: 200,
      },
    ],
    CLDR_SUBTYPE_URL:
      '<a href="https://sites.google.com/site/cldr/development/subtypes">https://sites.google.com/site/cldr/development/subtypes</a>',
    END_MARKER: "-*- END CheckCLDR.Subtype Mapping -*-",
  };
  // replace one "error" with "snafu" in the html to avoid spurious "error" detection in parseAsMimeType
  const html = cldrErrorSubtypes
    .getHtml(json)
    .replace("CLDR error subtypes", "CLDR snafu subtypes");

  it("should not return null or empty", function () {
    assert(html != null && html !== "", "html is neither null nor empty");
  });

  const xml = "<div>" + html + "</div>";
  const xmlStr = cldrTest.parseAsMimeType(xml, "application/xml");
  it("should return valid xml when in div element", function () {
    assert(xmlStr || false, "parses OK as xml when in div element");
  });

  const htmlStr = cldrTest.parseAsMimeType(html, "text/html");
  it("should return good html", function () {
    assert(htmlStr || false, "parses OK as html");
  });

  it("should contain angle brackets", function () {
    assert(
      htmlStr.indexOf("<") !== -1 && htmlStr.indexOf(">") !== -1,
      "does contain angle brackets"
    );
  });

  const values = [json.BEGIN_MARKER, json.unhandled.strings[2]];
  for (let i in values) {
    const v = values[i];
    it("should contain " + v, function () {
      assert(html.indexOf(v) !== -1, v + " does occur in " + html);
    });
  }
});
