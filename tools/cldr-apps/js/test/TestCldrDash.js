import * as cldrDash from "../src/esm/cldrDash.js";

export const TestCldrDash = "ok";

const assert = chai.assert;

describe("cldrDash.setData", function () {
  let json = null;

  it("should have test data", function () {
    /*
     * dashJson is defined in dash_data.js
     */
    json = dashJson;
    assert(json, "Test data is defined");
  });

  it("should not crash", function () {
    cldrDash.setData(json);
  });
});

describe("cldrDash.updatePath", function () {
  let json0 = null,
    json1 = null,
    json2 = null;

  it("should have test data", function () {
    /*
     * dashUpdateJson1 and dashUpdateJson2 are defined in dash_data.js
     */
    json0 = dashJson;
    json1 = dashUpdateJson1;
    json2 = dashUpdateJson2;
    assert(json0 && json1 && json2, "Test data is defined");
  });

  it("should add entries", function () {
    // json0 has 1 entry for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    // json1 has 3 entries for 710b6e70773e5764
    // the resulting data should have 3 entries for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    const data = cldrDash.updatePath(json0, json1);
    assert.strictEqual(countEntriesForPath(data, "710b6e70773e5764"), 3);
    assert.strictEqual(countEntriesForPath(data, "64a8a83fbacdf836"), 1);
  });

  it("should remove entries", function () {
    // json0 has 1 entry for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    // json2 has 0 entries for 710b6e70773e5764
    // the resulting data should have 0 entries for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    const data = cldrDash.updatePath(json0, json2);
    assert.strictEqual(countEntriesForPath(data, "710b6e70773e5764"), 0);
    assert.strictEqual(countEntriesForPath(data, "64a8a83fbacdf836"), 1);
  });
});

function countEntriesForPath(data, xpstrid) {
  let count = 0;
  for (let catData of data.notifications) {
    for (let group of catData.groups) {
      for (let entry of group.entries) {
        if (entry.xpstrid === xpstrid) {
          ++count;
        }
      }
    }
  }
  return count;
}
