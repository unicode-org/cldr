import * as cldrTest from "./TestCldrTest.mjs";

import * as cldrAddValue from "../src/esm/cldrAddValue.mjs";

import * as chai from "chai";

export const TestCldrAddValue = "ok";

const assert = chai.assert;

describe("cldrAddValue.convertTagsToText", function () {
  const tags = ["abc", "–", "xyz"];
  const textExpected = "abc–xyz";
  const text = cldrAddValue.convertTagsToText(tags);

  it("should convert tags to text", function () {
    assert.equal(text, textExpected);
  });
});

describe("cldrAddValue.convertTextToTags", function () {
  // Here "–" is U+2013 EN DASH, which is special per cldrEscaper.
  const text = "abc–xyz";
  const tagsExpected = ["abc", "–", "xyz"];
  const tags = cldrAddValue.convertTextToTags(text);

  it("should convert text to tags", function () {
    assert.deepEqual(tags, tagsExpected, "tags are equal");
  });

  // Here ″ is U+2033 DOUBLE PRIME, for which cldrChar.shouldDisplayAsTag returns true
  // although it is not special per cldrEscaper.
  const text2 = "123″789";
  const tagsExpected2 = ["123", "″", "789"];
  const tags2 = cldrAddValue.convertTextToTags(text2);

  it("should convert more text to tags", function () {
    assert.deepEqual(tags2, tagsExpected2, "tags are equal");
  });
});
