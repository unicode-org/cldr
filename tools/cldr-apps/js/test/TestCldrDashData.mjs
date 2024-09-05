import * as cldrDashData from "../src/esm/cldrDashData.mjs";

export const TestCldrDashData = "ok";

const assert = chai.assert;

describe("cldrDashData.setData", function () {
  let json = null;

  it("should have test data", function () {
    /*
     * dashJson is defined in dash_data.mjs
     */
    json = dashJson;
    assert(json, "Test data is defined");
  });

  it("should not crash", function () {
    cldrDashData.setData(json);
  });
});

describe("cldrDashData.updatePath", function () {
  let json0 = null,
    json1 = null,
    json2 = null;

  it("should have valid test data", function () {
    /*
     * dashUpdateJson1 and dashUpdateJson2 are defined in dash_data.mjs
     */
    json0 = dashJson;
    json1 = dashUpdateJson1;
    json2 = dashUpdateJson2;
    assert(json0 && json1 && json2, "Test data is defined");
    assert(json1.xpstrid && json2.xpstrid, "Update data must include xpstrid");
  });

  it("should add entries", function () {
    // json0 has 1 entry for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    // json1 has 3 entries for 710b6e70773e5764

    // the resulting data should have 1 (combined) entry for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    // DashData should never have more than one entry per path
    const data0 = cldrDashData.setData(json0);
    const data1 = cldrDashData.updatePath(data0, json1);
    assert.strictEqual(countEntriesForPath(data1, "710b6e70773e5764"), 1);
    assert.strictEqual(countEntriesForPath(data1, "64a8a83fbacdf836"), 1);
    // the entry for 710b6e70773e5764 should have 3 notifications
    assert.strictEqual(
      countNotificationsForEntry(data1, "710b6e70773e5764"),
      3
    );
    // the entry for 64a8a83fbacdf836 should have 1 notification
    assert.strictEqual(
      countNotificationsForEntry(data1, "64a8a83fbacdf836"),
      1
    );
  });

  it("should remove entries", function () {
    // json0 has 1 entry for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    // json2 has 0 entries for 710b6e70773e5764
    // the resulting data should have 0 entries for 710b6e70773e5764 and 1 entry for 64a8a83fbacdf836
    const data0 = cldrDashData.setData(json0);
    const data2 = cldrDashData.updatePath(data0, json2);
    assert.strictEqual(countEntriesForPath(data2, "710b6e70773e5764"), 0);
    assert.strictEqual(countEntriesForPath(data2, "64a8a83fbacdf836"), 1);
    // the entry for 64a8a83fbacdf836 should have 1 notification
    assert.strictEqual(
      countNotificationsForEntry(data2, "64a8a83fbacdf836"),
      1
    );
  });
});

/**
 * Count the number of entries for the given path in the given DashData; should be 0 or 1
 *
 * @param {Object} data the object of class DashData
 * @param {String} xpstrid the xpath hex string id
 * @returns the number of entries found in the data for this xpath
 */
function countEntriesForPath(data, xpstrid) {
  let count = 0;
  for (let entry of data.entries) {
    if (entry.xpstrid === xpstrid) {
      ++count;
    }
  }
  return count;
}

/**
 * Count the number of notifications for the given path in the given DashData
 *
 * @param {Object} data the object of class DashData
 * @param {String} xpstrid the xpath hex string id
 * @returns the number of notification categories found in the DashEntry for this xpath
 */
function countNotificationsForEntry(data, xpstrid) {
  const entry = data.pathIndex[xpstrid];
  return entry.cats.size;
}
