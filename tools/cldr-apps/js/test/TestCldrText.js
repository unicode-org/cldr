import * as cldrText from "../src/esm/cldrText.js";

export const TestCldrText = "ok";

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

describe("subVerbose", function () {
  it("should return same as sub for any arguments", function () {
    let key = "StatusAction_popupmsg";
    let map = ["too goofy", "🤪"];
    let result = cldrText.sub(key, map);
    let resultV = subVerbose(key, map);
    assert(
      result === resultV,
      "[" + result + "]" + " should equal [" + resultV + "]"
    );

    key = "explainRequiredVotes";
    map = {
      requiredVotes: 2468,
    };
    result = cldrText.sub(key, map);
    resultV = subVerbose(key, map);
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
    resultV = subVerbose(key, map);
    assert(
      result === resultV,
      "[" + result + "]" + " should equal [" + resultV + "]"
    );
  });
});

const CLDR_TEXT_DEBUG = false;

/**
 * Same as sub(), but more verbose, with subroutines, for unit testing and debugging
 */
function subVerbose(k, map) {
  const template = cldrText.get(k);
  if (!template) {
    console.log("subVerbose: missing template for k = " + k);
    return "";
  }
  if (map instanceof Array) {
    return fillInBlanksWithArray(template, map);
  } else if (map instanceof Object) {
    return fillInBlanksWithObject(template, map);
  } else {
    return "";
  }
}

/**
 * Get the string that results from filling in blanks in the given template
 *
 * @param template a string like "Sorry, your vote for '${1}' could not be submitted: ${0}"
 * @param map an array like ["too goofy", "🤪"]
 * @return a string like "Sorry, your vote for '🤪' could not be submitted: too goofy"
 */
function fillInBlanksWithArray(template, map) {
  const result = template.replace(/\${(\d)}/g, function (blank, index) {
    const replacement = map[index];
    if (CLDR_TEXT_DEBUG) {
      // index = 1; blank = ${1}; replacement = 🤪
      console.log(
        "Array: index= " +
          index +
          "; blank= " +
          blank +
          "; replacement = " +
          replacement
      );
    }
    return replacement ? replacement : "";
  });
  return result;
}

/**
 * Get the string that results from filling in blanks in the given template
 *
 * @param template a string like "Changes to this item require ${requiredVotes} votes."
 * @param map an object like {requiredVotes: 2468}
 * @return a string like "Changes to this item require 2468 votes."
 *
 * Note: placeholders of the form "${a.b}" are NOT supported (unlike old stui.js using dojo/string)
 */
function fillInBlanksWithObject(template, map) {
  const result = template.replace(/\${([^}]+)}/g, function (blank, key) {
    const replacement = map[key];
    if (CLDR_TEXT_DEBUG) {
      // key = requiredVotes; blank = ${requiredVotes}; replacement = 2468
      console.log(
        "Object: key = " +
          key +
          "; blank = " +
          blank +
          "; replacement = " +
          replacement
      );
    }
    return replacement ? replacement : "";
  });
  return result;
}
