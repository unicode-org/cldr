import * as cldrTest from "./TestCldrTest.js";

import * as cldrForumParticipation from "../src/esm/cldrForumParticipation.js";

export const TestCldrForumParticipation = "ok";

const assert = chai.assert;

describe("cldrForumParticipation.makeHtmlFromJson", function () {
  /*
   * forumParticipationJson has been defined in forum_participation_json.js
   */
  const json = forumParticipationJson;

  it("should get json", function () {
    assert(json != null);
  });

  const html = cldrForumParticipation.makeHtmlFromJson(json);

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
