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

  // TODO: test cldrText.sub, after implementing it without dojo/string
  // The following won't work with dojo/string since we get 'require is not defined'
  /***
  describe("cldrText.sub", function () {
    it("should substitute explainRequiredVotes correctly", function () {
      const s = cldrText.sub("explainRequiredVotes", {
        requiredVotes: 2468,
      });
      const expect = "Changes to this item require 2468 votes.";
      assert(s === expect, s + " should equal " + expect);
    });
  });
  ***/
}
