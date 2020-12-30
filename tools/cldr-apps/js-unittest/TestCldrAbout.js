"use strict";

{
  const assert = chai.assert;

  describe("cldrAbout.test.getHtml", function () {
    const json = {
      ICU_VERSION: "68.1.0.0",
    };
    const html = cldrAbout.test.getHtml(json);

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

    const label = "ICU";
    const value = json.ICU_VERSION;
    it("should contain " + label + " and " + value, function () {
      assert(html.indexOf(label) !== -1, label + " does occur in " + html);
      assert(html.indexOf(value) !== -1, value + " does occur in " + html);
    });
  });
}
