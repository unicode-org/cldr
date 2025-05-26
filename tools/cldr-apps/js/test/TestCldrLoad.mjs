import * as cldrLoad from "../src/esm/cldrLoad.mjs";

import * as chai from "chai";

export const TestCldrLoad = "ok";

const assert = chai.assert;

describe("cldrLoad.localeMapReady", function () {
  const lm = {
    locmap: {
      locales: {
        // The locale ID "aa" should map to an object but for simple test it maps to "foo"
        aa: "foo",
      },
    },
  };
  it("should be ready after setTheLocaleMap", function () {
    cldrLoad.setTheLocaleMap(lm);
    assert(cldrLoad.localeMapReady() === true, "map should be ready");
  });
});
