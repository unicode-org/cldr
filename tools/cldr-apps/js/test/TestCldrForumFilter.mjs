import * as cldrForum from "../src/esm/cldrForum.mjs";
import * as cldrForumFilter from "../src/esm/cldrForumFilter.mjs";

import * as chai from "chai";

export const TestCldrForumFilter = "ok";

const assert = chai.assert;

describe("cldrForumFilter.createMenu", function () {
  it("should not return null", function () {
    cldrForumFilter.setUserId(1);
    const actualOutput = cldrForumFilter.createMenu(null);
    assert(actualOutput != null, "not null");
  });
});

describe("cldrForumFilter.getFilteredThreadIds", function () {
  it("should return one thread for two related posts", function () {
    const posts = [
      { id: 1, parent: -1, root: -1, poster: 100, locale: "am" },
      { id: 2, parent: 1, root: 1, poster: 200, locale: "am" },
    ];
    const threadHash = cldrForum.getThreadHash(posts);
    const actualOutput = cldrForumFilter.getFilteredThreadIds(
      threadHash,
      false
    );
    const expectedOutput = ["1"];
    assert.deepEqual(actualOutput, expectedOutput);
  });
  it("should return one thread for three related posts", function () {
    const posts = [
      { id: 1, parent: -1, root: -1, poster: 100, locale: "zh" },
      { id: 2, parent: 1, root: 1, poster: 200, locale: "zh" },
      { id: 3, parent: 2, root: 1, poster: 300, locale: "zh" },
    ];
    const threadHash = cldrForum.getThreadHash(posts);
    const actualOutput = cldrForumFilter.getFilteredThreadIds(
      threadHash,
      false
    );
    const expectedOutput = ["1"];
    assert.deepEqual(actualOutput, expectedOutput);
  });
  it("should return two threads for two unrelated posts", function () {
    const posts = [
      { id: 1, parent: -1, root: -1, poster: 100, locale: "pt_PT" },
      { id: 2, parent: -1, root: -1, poster: 200, locale: "pt_PT" },
    ];
    const threadHash = cldrForum.getThreadHash(posts);
    const actualOutput = cldrForumFilter.getFilteredThreadIds(
      threadHash,
      false
    );
    const expectedOutput = ["1", "2"];
    assert.deepEqual(actualOutput, expectedOutput);
  });
});
