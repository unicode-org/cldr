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

describe("cldrAddValue.tagIndexFromTextIndex", function () {
  // Assume space is a "special" character and therefore gets a tag, while letters a-z and A-Z are not special.
  // Due to dependence of cldrEscaper on the back end for full data, make no other assumptions about what characters are special.
  const text1 = "WXYZ";
  const map1 = new Map([
    [0, 0], // W
    [1, 0], // X
    [2, 0], // Y
    [3, 0], // Z
  ]);

  const text2 = "    ";
  const map2 = new Map([
    [0, 0], // space
    [1, 1], // space
    [2, 2], // space
    [3, 3], // space
  ]);

  const text3 = "abc xyz";
  const map3 = new Map([
    [0, 0], // a
    [1, 0], // b
    [2, 0], // c
    [3, 1], // space
    [4, 2], // x
    [5, 2], // y
    [6, 2], // z
  ]);

  const text4 = " A BC DEF  GHI ";
  const map4 = new Map([
    [0, 0], // space
    [1, 1], // A
    [2, 2], // space
    [3, 3], // B
    [4, 3], // C
    [5, 4], // space
    [6, 5], // D
    [7, 5], // E
    [8, 5], // F
    [9, 6], // space
    [10, 7], // space
    [11, 8], // G
    [12, 8], // H
    [13, 8], // I
    [14, 9], // space
  ]);

  it("should map text indexes to tag indexes for map1", function () {
    for (const [key, value] of map1) {
      // console.log("test map1, expect key " + key + " maps to value " + value);
      const tagIndex = cldrAddValue.tagIndexFromTextIndex(text1, key);
      assert.equal(value, tagIndex, "Maps " + key + " to " + value);
    }
  });

  it("should map text indexes to tag indexes for map2", function () {
    for (const [key, value] of map2) {
      // console.log("test map2, expect key " + key + " maps to value " + value);
      const tagIndex = cldrAddValue.tagIndexFromTextIndex(text2, key);
      assert.equal(value, tagIndex, "Maps " + key + " to " + value);
    }
  });

  it("should map text indexes to tag indexes for map3", function () {
    for (const [key, value] of map3) {
      // console.log("test map3, expect key " + key + " maps to value " + value);
      const tagIndex = cldrAddValue.tagIndexFromTextIndex(text3, key);
      assert.equal(value, tagIndex, "Maps " + key + " to " + value);
    }
  });

  it("should map text indexes to tag indexes for map4", function () {
    for (const [key, value] of map4) {
      // console.log("test map4, expect key " + key + " maps to value " + value);
      const tagIndex = cldrAddValue.tagIndexFromTextIndex(text4, key);
      assert.equal(value, tagIndex, "Maps " + key + " to " + value);
    }
  });
});
