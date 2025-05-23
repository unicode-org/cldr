import * as cldrTable from "../src/esm/cldrTable.mjs";

import * as chai from "chai";

export const TestCldrTable = "ok";

const assert = chai.assert;

describe("cldrTable.makeHeaderId", function () {
  const id = cldrTable.makeHeaderId("25?!_Abc,,,");

  it("should replace adjacent punct with single underscore", function () {
    assert(id.includes("_") && !id.includes("__"));
  });

  it("should not remove digits or letters", function () {
    assert(id.includes("25") && id.includes("Abc"));
  });

  it("should match isHeaderId", function () {
    assert(cldrTable.isHeaderId(id));
  });
});
