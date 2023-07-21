import { expect } from "chai";
import mocha from "mocha";

import * as cldrXpathUtils from "../../src/esm/cldrXpathUtils.mjs";

describe("cldrXpathUtils test", function () {
  describe("extraPathAllowsNullValue() test", function () {
    it("returns false for these paths", function () {
      for (const path of ["//ldml/identity"]) {
        expect(cldrXpathUtils.extraPathAllowsNullValue(path), path).to.be.false;
      }
    });
    it("returns true for these paths", function () {
      for (const path of [
        '//ldml/units/unitLength[@type="long"]/unit[@type="light-lumen"]/gender',
        '//ldml/dates/timeZoneNames/metazone[@type="Almaty"]/short/standard',
      ]) {
        expect(cldrXpathUtils.extraPathAllowsNullValue(path), path).to.be.true;
      }
    });
  });
});
