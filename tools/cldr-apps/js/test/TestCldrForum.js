import * as cldrTest from "./TestCldrTest.js";

import * as cldrForum from "../src/esm/cldrForum.js";
import * as cldrStatus from "../src/esm/cldrStatus.js";

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

    assert.equal(content.childElementCount, 1065, "content has 1065 children");

    assert(content.firstChild != null, "first child is not null");
    assert.equal(content.firstChild.id, "fthr_fr_CA|45347");
    const s1 =
      "n (Gaeilge) TC[v38] 2020-02-06 17:26 UTCClosedClosetest" +
      "n (Gaeilge) TC[v38] 2020-02-06 17:28 UTCClosetest reply blah!";
    assert.equal(
      cldrTest.normalizeWhitespace(content.firstChild.textContent),
      cldrTest.normalizeWhitespace(s1)
    );

    const firstSibling = content.firstChild.nextSibling;
    assert(firstSibling != null, "first sibling is not null");
    assert.equal(firstSibling.id, "fthr_fr_CA|45346");
    const s2 = "n (Gaeilge) TC[v38] 2020-02-06 17:20 UTCClosedCloseFUrthermore";
    assert.equal(
      cldrTest.normalizeWhitespace(firstSibling.textContent),
      cldrTest.normalizeWhitespace(s2)
    );

    const secondSibling = firstSibling.nextSibling;
    assert(secondSibling != null, "second sibling is not null");
    assert.equal(secondSibling.id, "fthr_fr_CA|45343");
    const s3 =
      "n (Gaeilge) TC[v38] 2020-02-06 17:17 UTCClosedClosetest" +
      "n (Gaeilge) TC[v38] 2020-02-06 17:18 UTCCloseOK, replying to test in Dashboard" +
      "n (Gaeilge) TC[v38] 2020-02-06 17:19 UTCCloseAnd replying to reply";
    assert.equal(
      cldrTest.normalizeWhitespace(secondSibling.textContent),
      cldrTest.normalizeWhitespace(s3)
    );

    const thirdSibling = secondSibling.nextSibling;
    assert(thirdSibling != null, "third sibling is not null");
    assert.equal(thirdSibling.id, "fthr_fr_CA|45341");
    const s4 =
      "n (Gaeilge) TC[v38] 2020-02-06 17:12 UTCClosedCloseAnd another post from Dashboard." +
      "n (Gaeilge) TC[v38] 2020-02-06 17:14 UTCCloseAnd another reply, this time from Dashboard Fix pop-up!!!";
    assert.equal(
      cldrTest.normalizeWhitespace(thirdSibling.textContent),
      cldrTest.normalizeWhitespace(s4)
    );

    assert(content.lastChild != null, "last child is not null");
    assert.equal(content.lastChild.id, "fthr_fr|363");
    const sLast =
      "n (Microsoft) Vetter[v28] 2015-05-19 15:58 UTCClosedClose...";
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
