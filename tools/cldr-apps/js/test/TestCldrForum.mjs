import * as cldrTest from "./TestCldrTest.mjs";

import * as cldrForum from "../src/esm/cldrForum.mjs";
import * as cldrStatus from "../src/esm/cldrStatus.mjs";

import * as chai from "chai";

export const TestCldrForum = "ok";

const assert = chai.assert;

cldrStatus.setSessionId(1); // avoid "Error: sessionId falsy in getLoadForumUrl"

describe("cldrForum.parseContent", function () {
  let json = null;

  it("should get json", function () {
    /*
     * forumJson has been defined in forum_fr.js
     */
    json = forumJson;
    assert(json != null);
  });

  it("should return correct result for this json", function () {
    this.timeout(5000);

    const posts = json.ret;
    assert(posts != null, "posts is not null");

    cldrForum.setDisplayUtc(true); // so test can pass regardless of time zone

    const content = cldrForum.parseContent(posts, "info");
    assert(content != null, "content is not null");

    assert.equal(content.childElementCount, 1542, "content has 1542 children");

    assert(content.firstChild != null, "first child is not null");
    assert.equal(content.firstChild.id, "fthr_96854");
    const s1 =
      "Requesting “₾”Google#1158 (google) Vetter[v42] 2022-07-04 19:1 UTCOpenRequest(redacted 🆗)Google#2303 (google) TC[v42] 2022-07-15 04:36 UTCComment(redacted 🆗)";
    assert.equal(
      cldrTest.normalizeWhitespace(content.firstChild.textContent),
      cldrTest.normalizeWhitespace(s1)
    );

    const firstSibling = content.firstChild.nextSibling;
    assert(firstSibling != null, "first sibling is not null");
    assert.equal(firstSibling.id, "fthr_98899");
    const s2 =
      "Requesting “Micronésie”Facebook#2542 (meta) Vetter[v42] 2022-07-06 10:40 UTCOpenRequest(redacted 🆗)Guest#566 (unaffiliated) Guest[v42] 2022-07-06 12:1 UTCAgree(redacted 🆗)Google#2482 (google) Vetter[v42] 2022-07-11 08:40 UTCComment(redacted 🆗)Guest#566 (unaffiliated) Guest[v42] 2022-07-11 11:28 UTCComment(redacted 🆗)Google#2482 (google) Vetter[v42] 2022-07-11 16:10 UTCComment(redacted 🆗)";
    assert.equal(
      cldrTest.normalizeWhitespace(firstSibling.textContent),
      cldrTest.normalizeWhitespace(s2)
    );

    const secondSibling = firstSibling.nextSibling;
    assert(secondSibling != null, "second sibling is not null");
    assert.equal(secondSibling.id, "fthr_100716");
    const s3 =
      "Requesting “pt typog.”Google#2482 (google) Vetter[v42] 2022-07-11 09:1 UTCOpenRequest(redacted 🆗)Apple#2291 (apple) Vetter[v42] 2022-07-11 10:29 UTCAgree(redacted 🆗)Apple#2291 (apple) Vetter[v42] 2022-07-11 10:30 UTCComment(redacted 🆗)Guest#566 (unaffiliated) Guest[v42] 2022-07-11 11:55 UTCComment(redacted 🆗)";
    assert.equal(
      cldrTest.normalizeWhitespace(secondSibling.textContent),
      cldrTest.normalizeWhitespace(s3)
    );

    const thirdSibling = secondSibling.nextSibling;
    assert(thirdSibling != null, "third sibling is not null");
    assert.equal(thirdSibling.id, "fthr_100751");
    const s4 =
      "Requesting “El Salvador”Google#2482 (google) Vetter[v42] 2022-07-11 09:16 UTCOpenRequest(redacted 🆗)Guest#566 (unaffiliated) Guest[v42] 2022-07-11 11:53 UTCComment(redacted 🆗)";
    assert.equal(
      cldrTest.normalizeWhitespace(thirdSibling.textContent),
      cldrTest.normalizeWhitespace(s4)
    );

    assert(content.lastChild != null, "last child is not null");
    assert.equal(content.lastChild.id, "fthr_363");
    const sLast =
      "Microsoft#1530 (microsoft) Vetter[v28] 2015-05-19 15:58 UTCClosedDiscuss(redacted 🆗)";
    assert.equal(
      cldrTest.normalizeWhitespace(content.lastChild.textContent),
      cldrTest.normalizeWhitespace(sLast)
    );
  });
});

/*
 * Note: this test is currently disabled since cldrForum.getForumSummaryHtml depends
 * on a response from SurveyAjax. We should revise it to get a mock response for testing.
 */
/*
describe("cldrForum.getForumSummaryHtml", function () {
  const html = cldrForum.getForumSummaryHtml("aa", 1, true);

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
      "does contain angle brackets"
    );
  });
  */
