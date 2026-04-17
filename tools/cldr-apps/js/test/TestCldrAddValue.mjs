import * as cldrTest from "./TestCldrTest.mjs";

import * as cldrAddValue from "../src/esm/cldrAddValue.mjs";

import * as chai from "chai";

export const TestCldrAddValue = "ok";

const assert = chai.assert;

describe("cldrAddValue.convertTagsToText", function () {
  const tags = ["abc", " ", "xyz"];
  const textExpected = "abc xyz";
  const text = cldrAddValue.convertTagsToText(tags);

  it("should convert tags to text", function () {
    assert.equal(text, textExpected);
  });
});

describe("cldrAddValue.convertTextToTags", function () {
  const text = "abc xyz";
  const tagsExpected = ["abc", " ", "xyz"];
  const tags = cldrAddValue.convertTextToTags(text);

  it("should convert text to tags", function () {
    assert.deepEqual(tags, tagsExpected, "tags are equal");
  });
});

