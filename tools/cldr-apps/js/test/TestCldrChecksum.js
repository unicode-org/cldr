import * as cldrTable from "../src/esm/cldrTable.js";

export const TestCldrChecksum = "ok";

const assert = chai.assert;

/**
 * Without checksum, updating DOM for about 100 rows can lead to console warning like
 * "[Violation] 'load' handler took 546ms". The checksum enables skipping DOM update
 * for rows whose checksum hasn't changed, which should improve performance if
 * calculating checksums for 1000 rows takes only a fraction of a second.
 */
const ITERATIONS = 1000;
const MAX_MILLISECS = 100; // 100 ms = one tenth of a second

/**
 * Based on JSON.stringify(theRow)) for actual data
 */
const sLong =
  '{"xpstrid":"525a75da362e7e3f",' +
  '"inheritedLocale":"de",' +
  '"code":"Chinese ► zh_Hant",' +
  '"hasVoted":false,' +
  '"voteResolver":{"value_vote":["Chinesisch (traditionell)",' +
  "16]," +
  '"raw":"{bailey: “Chinesisch (Traditionell)” trunk: {Chinesisch (traditionell),' +
  " approved}," +
  " {orgToVotes: apple={Chinesisch (traditionell)=4}," +
  " meta={Chinesisch (traditionell)=4}," +
  " google={Chinesisch (traditionell)=4}," +
  " microsoft={Chinesisch (traditionell)=4}," +
  " totals: {Chinesisch (traditionell)=16}," +
  " conflicted: []}," +
  " sameVotes: []," +
  " O: Chinesisch (traditionell)," +
  " N: null," +
  " totals: {Chinesisch (traditionell)=16}," +
  " winning: {Chinesisch (traditionell)," +
  ' approved}}",' +
  '"orgs":{"apple":{"votes":{"Chinesisch (traditionell)":4},' +
  '"orgVote":"Chinesisch (traditionell)",' +
  '"status":"ok"},' +
  '"meta":{"votes":{"Chinesisch (traditionell)":4},' +
  '"orgVote":"Chinesisch (traditionell)",' +
  '"status":"ok"},' +
  '"google":{"votes":{"Chinesisch (traditionell)":4},' +
  '"orgVote":"Chinesisch (traditionell)",' +
  '"status":"ok"},' +
  '"microsoft":{"votes":{"Chinesisch (traditionell)":4},' +
  '"orgVote":"Chinesisch (traditionell)",' +
  '"status":"ok"}},' +
  '"valueIsLocked":false,' +
  '"nameTime":{"De":1591784803000,' +
  '"Anon":1591134568000,' +
  '"Anon Anon":1594076788000,' +
  '"German-Germany Volker":1591173307000},' +
  '"requiredVotes":8},' +
  '"winningVhash":"Q2hpbmVzaXNjaCAodHJhZGl0aW9uZWxsKQ",' +
  '"canFlagOnLosing":false,' +
  '"displayName":"Traditional Chinese",' +
  '"statusAction":"ALLOW",' +
  '"inheritedValue":"Chinesisch (Traditionell)",' +
  '"winningValue":"Chinesisch (traditionell)",' +
  '"extraAttributes":{},' +
  '"coverageValue":40,' +
  '"xpathId":581,' +
  '"xpath":"//ldml/localeDisplayNames/languages/language[@type=\\"zh_Hant\\"]",' +
  '"rowFlagged":false,' +
  '"confirmStatus":"approved",' +
  '"displayExample":"<div class=\'cldr_example\'>Traditional Chinese",' +
  '"voteVhash":"",' +
  '"items":{"4oaR4oaR4oaR":{"tests":[],' +
  '"rawValue":"↑↑↑",' +
  '"valueHash":"4oaR4oaR4oaR",' +
  '"pClass":"fallback",' +
  '"isBaselineValue":false,' +
  '"value":"↑↑↑",' +
  '"example":"<div class=\'cldr_example\'>Chinesisch (Traditionell)"},' +
  '"Q2hpbmVzaXNjaCAodHJhZGl0aW9uZWxsKQ":{"tests":[],' +
  '"rawValue":"Chinesisch (traditionell)",' +
  '"valueHash":"Q2hpbmVzaXNjaCAodHJhZGl0aW9uZWxsKQ",' +
  '"pClass":"winner",' +
  '"isBaselineValue":true,' +
  '"votes":{"1442":{"org":"microsoft",' +
  '"level":"vetter",' +
  '"name":"German-Germany Anon",' +
  '"votes":4,' +
  '"email":"ms_anon (at) org.com"},' +
  '"1781":{"org":"google",' +
  '"level":"vetter",' +
  '"name":"De",' +
  '"votes":4,' +
  '"email":"002-anon (at) org.com"},' +
  '"1234":{"org":"meta",' +
  '"level":"vetter",' +
  '"name":"Anon Anon",' +
  '"votes":4,' +
  '"email":"anon (at) org.com"},' +
  '"1234":{"org":"apple",' +
  '"level":"vetter",' +
  '"name":"Anon",' +
  '"votes":4,' +
  '"email":"anon.anon (at) anon.de"}},' +
  '"value":"Chinesisch (traditionell)",' +
  '"example":"<div class=\'cldr_example\'>Chinesisch (traditionell)"}}}';

describe("cldrTable.cldrChecksum", function () {
  const s1 = "animal=猫";
  const s2 = "animal=狗";
  const c1 = cldrTable.cldrChecksum(s1);
  const c2 = cldrTable.cldrChecksum(s1); // c2 should equal c1
  const c3 = cldrTable.cldrChecksum(s2); // c3 should not equal c1
  const cLong = cldrTable.cldrChecksum(sLong);

  it("should not return null or undefined", function () {
    assert(
      cLong !== null && cLong !== undefined,
      "cLong should not be null or undefined"
    );
  });

  it("should return a number", function () {
    assert(typeof cLong === "number", "typeof cLong should be number");
  });

  it("should return same for same strings", function () {
    assert(c1 === c2, "c1 should equal c2");
  });

  it("should return different for different strings", function () {
    assert(c1 !== c3, "c1 should not equal c3");
  });

  it("should be fast enough", function () {
    const startTime = Date.now();
    for (let i = 0; i < ITERATIONS; i++) {
      const c = cldrTable.cldrChecksum(sLong);
      assert(c === cLong, "c should equal cLong");
    }
    // This occasionally failed on github, which presumably is sometimes slow
    // for reasons beyond our control. Therefore, don't assert, just send a
    // warning message to the console.
    const duration = Date.now() - startTime; // typically 15 ms
    if (duration > MAX_MILLISECS) {
      console.log(
        "WARNING: cldrChecksum duration was " +
          duration +
          " ms; " +
          ITERATIONS +
          " iterations should take less than " +
          MAX_MILLISECS +
          " ms"
      );
    }
  });
});
