import { expect } from "chai";
import mocha from "mocha";

import { invertMap } from "../../src/esm/setUtils.mjs";

describe("setUtils test", function () {
  describe("invertMap", function () {
    it("Should be able to invert an object", () => {
      const input = {
        MODERN: ["m3", "m1", "m2", "m0"],
        CORE: ["c0"],
      };
      const expected = {
        m0: "MODERN",
        m1: "MODERN",
        m2: "MODERN",
        m3: "MODERN",
        c0: "CORE",
      };
      const actual = invertMap(input);
      expect(actual).to.deep.equal(expected);
    });
  });
});
