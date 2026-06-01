import * as cldrStatus from "../src/esm/cldrStatus.mjs";

import * as chai from "chai";

export const TestCldrStatus = "ok";

const assert = chai.assert;

describe("cldrStatus.getRunningStamp", function () {
  const stamp = cldrStatus.getRunningStamp();

  it("should not return null or undefined", function () {
    assert(
      stamp !== null && stamp !== undefined,
      "stamp should not be null or undefined"
    );
  });

  it("should return a number", function () {
    assert(
      typeof stamp === "number",
      "typeof stamp should be number, got " + typeof stamp
    );
  });
});

describe("cldrStatus.runningStampChanged", function () {
  const stamp = 12345;
  const changed1 = cldrStatus.runningStampChanged(stamp);
  const changed2 = cldrStatus.runningStampChanged(stamp);
  const changed3 = cldrStatus.runningStampChanged(stamp + 1);

  it("should return false the first time", function () {
    assert(changed1 === false, "changed1 should be false, got " + changed1);
  });

  it("should return false for same stamp", function () {
    assert(changed2 === false, "changed2 should be false, got " + changed2);
  });

  it("should return true for changed stamp", function () {
    assert(changed3 === true, "changed3 should be true, got " + changed3);
  });
});

describe("cldrStatus.getCurrentId and setCurrentId", function () {
  const id1 = "42abc";
  cldrStatus.setCurrentId(id1);
  const id2 = cldrStatus.getCurrentId();

  it("should get what is set", function () {
    assert(id2 === id1, "id2 = " + id2 + " should be same as id1 = " + id1);
  });

  const id3number = 1234;
  const id3string = id3number.toString();
  cldrStatus.setCurrentId(id3number);
  const id4 = cldrStatus.getCurrentId();

  it("should convert a number to a string", function () {
    assert(
      id4 === id3string,
      "id4 = " + id4 + " should be same as id3string = " + id3string
    );
  });
});
