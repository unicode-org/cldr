"use strict";

{
  const assert = chai.assert;

  describe("cldrText.get", function () {
    it("should map foo to foo", function () {
      const s = cldrText.get("foo");
      assert(s === "foo", s + " should equal foo");
    });

    it("should map userlevel_vetter_desc to Regular Vetter", function () {
      const s = cldrText.get("userlevel_vetter_desc");
      assert(s === "Regular Vetter", s + " should equal Regular Vetter");
    });

    it("should map inherited-unconfirmed to Inherited and Unconfirmed", function () {
      const s = cldrText.get("inherited-unconfirmed");
      assert(
        s === "Inherited and Unconfirmed",
        s + " should equal Inherited and Unconfirmed"
      );
    });

    it("should map empty string to empty string", function () {
      const s = cldrText.get("");
      assert(s === "", "[" + s + "]" + "should be empty");
    });
  });

  describe("cldrText.sub", function () {
    it("should work right for array with StatusAction_popupmsg", function () {
      const key = "StatusAction_popupmsg";
      const map = ["too goofy", "🤪"];
      const expect =
        "Sorry, your vote for '🤪' could not be submitted: too goofy";
      const result = cldrText.sub(key, map);
      assert(
        result === expect,
        "[" + result + "]" + " should equal [" + expect + "]"
      );
    });

    it("should work right for single-key object with explainRequiredVotes", function () {
      const key = "explainRequiredVotes";
      const map = {
        requiredVotes: 2468,
      };
      const expect = "Changes to this item require 2468 votes.";
      const result = cldrText.sub(key, map);
      assert(
        result === expect,
        "[" + result + "]" + " should equal [" + expect + "]"
      );
    });

    it("should work right for two-key object with override_explain_msg", function () {
      const key = "override_explain_msg";
      const map = {
        overrideVotes: 9999,
        votes: 3,
      };
      const expect =
        "You have voted for this item with 9999 votes instead of the usual 3";
      const result = cldrText.sub(key, map);
      assert(
        result === expect,
        "[" + result + "]" + " should equal [" + expect + "]"
      );
    });
  });

  describe("cldrText.subVerbose", function () {
    it("should return same as sub for any arguments", function () {
      let key = "StatusAction_popupmsg";
      let map = ["too goofy", "🤪"];
      let result = cldrText.sub(key, map);
      let resultV = cldrText.test.subVerbose(key, map);
      assert(
        result === resultV,
        "[" + result + "]" + " should equal [" + resultV + "]"
      );

      key = "explainRequiredVotes";
      map = {
        requiredVotes: 2468,
      };
      result = cldrText.sub(key, map);
      resultV = cldrText.test.subVerbose(key, map);
      assert(
        result === resultV,
        "[" + result + "]" + " should equal [" + resultV + "]"
      );

      key = "override_explain_msg";
      map = {
        overrideVotes: 9999,
        votes: 3,
      };
      result = cldrText.sub(key, map);
      resultV = cldrText.test.subVerbose(key, map);
      assert(
        result === resultV,
        "[" + result + "]" + " should equal [" + resultV + "]"
      );
    });
  });
}
