import * as cldrLoad from "../src/esm/cldrLoad.mjs";
import * as cldrLocales from "../src/esm/cldrLocales.mjs";

import * as chai from "chai";

export const TestCldrLocales = "ok";

const assert = chai.assert;

describe("cldrLocales.isValid", function () {
  const lm = {
    locmap: {
      locales: {
        // The locale ID "aa" should map to an object but for simple test it maps to "foo"
        aa: "foo",
      },
    },
  };
  cldrLoad.setTheLocaleMap(lm);
  it("should reject null", function () {
    assert(cldrLocales.isValid(null) === false, "null is invalid");
  });
  it("should reject empty", function () {
    assert(cldrLocales.isValid("") === false, "empty string is invalid");
  });
  it("should reject number", function () {
    assert(cldrLocales.isValid(123) === false, "123 is invalid");
  });
  it("should reject bogus_locale", function () {
    assert(
      cldrLocales.isValid("bogus_locale") === false,
      "bogus_locale is invalid"
    );
  });
  it("should accept aa", function () {
    assert(cldrLocales.isValid("aa") === true, "aa is valid");
  });
});
