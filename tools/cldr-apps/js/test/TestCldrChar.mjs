import * as cldrChar from "../src/esm/cldrChar.mjs";

import * as chai from "chai";

const assert = chai.assert;

export const TestCldrChar = "ok";

describe("cldrChar.firstChar", function () {
  const s1 = "abc";
  const c1 = cldrChar.firstChar(s1);
  const s2 = "𠀀好";
  const c2 = cldrChar.firstChar(s2);

  it("should return first character for ASCII", function () {
    assert.equal(c1, "a", "abc should start with a");
  });

  it("should return first character for U+20000", function () {
    assert.equal(c2, "𠀀", "𠀀好 should start with 𠀀");
  });
});

describe("cldrChar.isSpecial", function () {
  // Caution: cldrChar.isSpecial depends on cldrEscaper, which requires the Survey Tool back end for full coverage.
  // Without the back end, as when running unit tests for the front end, cldrEscaper has a very limited set of fallback data.
  // Here we assume that the fallback data covers ASCII space.
  const capitalAIsSpecial = cldrChar.isSpecial("A");
  const spaceIsSpecial = cldrChar.isSpecial(" ");
  // const nbspIsSpecial = cldrChar.isSpecial(String.fromCharCode(0x00A0));

  it("should return false for A (U+0041)", function () {
    assert.equal(capitalAIsSpecial, false, "A should not be special");
  });
  it("should return true for space (U+0020)", function () {
    assert.equal(spaceIsSpecial, true, "Space should be special");
  });
  // it("should return true for NBSP (U+0x00A0)", function () {
  //  assert.equal(aIsSpecial, true, "NBSP should be special");
  // });
});
