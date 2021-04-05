import * as cldrTest from "./TestCldrTest.js";

import * as cldrCreateLogin from "../src/esm/cldrCreateLogin.js";

export const TestCldrCreateLogin = "ok";

const assert = chai.assert;

describe("cldrCreateLogin.getHtml", function () {
  const html = cldrCreateLogin.getHtml();

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
});
