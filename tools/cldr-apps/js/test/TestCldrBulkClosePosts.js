import * as cldrTest from "./TestCldrTest.js";

import * as cldrBulkClosePosts from "../src/esm/cldrBulkClosePosts.js";

export const TestCldrBulkClosePosts = "ok";

const assert = chai.assert;

describe("cldrBulkClosePosts.makeHtmlFromJson", function () {
  /*
   * bulkClosePostsJson has been defined in bulk_close_posts_json.js
   */
  const json = bulkClosePostsJson;

  it("should get json", function () {
    assert(json != null);
  });

  const html = cldrBulkClosePosts.makeHtmlFromJson(json);

  it("should not return null or empty", function () {
    assert(html != null && html !== "", "html is neither null nor empty");
  });

  const xmlStr = cldrTest.parseAsMimeType(html, "application/xml");
  it("should return valid xml", function () {
    assert(xmlStr || false, "parses OK as xml");
  });

  const htmlStr = cldrTest.parseAsMimeType(html, "text/html");
  it("should return good html", function () {
    assert(htmlStr || false, "parses OK as html");
  });

  it("should contain angle brackets", function () {
    assert(
      htmlStr.indexOf("<") !== -1 && htmlStr.indexOf(">") !== -1,
      "does contain angle brackets: " + htmlStr
    );
  });
});
