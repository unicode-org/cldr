import * as cldrProgress from "../src/esm/cldrProgress.mjs";

import * as chai from "chai";

const assert = chai.assert;

export const TestCldrProgress = "ok";

describe("cldrProgress.friendlyPercent", function () {
  it("should return a number", function () {
    const fp = cldrProgress.friendlyPercent(0, 0);
    assert(
      typeof fp === "number",
      "typeof fp should be number, got " + typeof fp
    );
  });
  it("should return 0 for -1 / 1000", function () {
    assert.equal(0, cldrProgress.friendlyPercent(-1, 1000));
  });
  it("should return 0 for 0 / 1000", function () {
    assert.equal(0, cldrProgress.friendlyPercent(0, 1000));
  });
  it("should return 1 for 1 / 1000", function () {
    assert.equal(1, cldrProgress.friendlyPercent(1, 1000));
  });
  it("should return 50 for 500 / 1000", function () {
    assert.equal(50, cldrProgress.friendlyPercent(500, 1000));
  });
  it("should return 99 for 999 / 1000", function () {
    assert.equal(99, cldrProgress.friendlyPercent(999, 1000));
  });
  it("should return 100 for 0 / 0", function () {
    assert.equal(100, cldrProgress.friendlyPercent(0, 0));
  });
  it("should return 100 for 1 / 0", function () {
    assert.equal(100, cldrProgress.friendlyPercent(1, 0));
  });
  it("should return 100 for 1000 / 1000", function () {
    assert.equal(100, cldrProgress.friendlyPercent(1000, 1000));
  });
  it("should return 100 for 1001 / 1000", function () {
    assert.equal(100, cldrProgress.friendlyPercent(1001, 1000));
  });
  it("should return 100 for 1000000 / 1000", function () {
    assert.equal(100, cldrProgress.friendlyPercent(1000000, 1000));
  });
});
