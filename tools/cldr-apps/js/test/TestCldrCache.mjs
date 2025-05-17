import * as cldrCache from "../src/esm/cldrCache.mjs";

import * as chai from "chai";

export const TestCldrCache = "ok";

const assert = chai.assert;

describe("cldrCache.LRU", function () {
  const c = new cldrCache.LRU(3);

  it("should work up to its capacity", function () {
    c.clear();
    c.set("1", "test1");
    c.set("2", "test2");
    c.set("3", "test3");
    const val1 = c.get("1");
    const val2 = c.get("2");
    const val3 = c.get("3");
    assert(
      val1 === "test1" && val2 === "test2" && val3 === "test3",
      "should get 3 items that were set but got " +
        val1 +
        " " +
        val2 +
        " " +
        val3
    );
  });

  it("should discard LRU", function () {
    c.set("4", "test4"); // 1 should be discarded
    assert(c.get("4") === "test4", "should get MRU");
    assert(
      c.get("1") === undefined,
      "should not get LRU that should have been discarded"
    );
  });

  it("should clear", function () {
    c.clear();
    assert(c.get("4") === undefined, "should not get anything after clear");
  });

  it("should revise LRU for get", function () {
    c.set("A", "testA");
    c.set("B", "testB");
    c.set("C", "testC");
    c.get("A"); // now A should be MRU and B should be LRU
    c.set("D", "testD"); // B (not A) should get discarded
    assert(c.get("B") === undefined, "should have discarded B");
    assert(c.get("A") === "testA", "should not have discarded A");
    assert(c.get("D") === "testD", "should have added D");
  });
});
