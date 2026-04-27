import { expect } from "chai";
import mocha from "mocha";

import * as cldrEscaper from "../../src/esm/cldrEscaper.mjs";

function uplus(ch) {
  if (!ch) return ch;
  return "U+" + Number(ch.codePointAt(0)).toString(16);
}

describe("cldrEscaper test", function () {
  describe("LRM/RLM test", function () {
    for (const ch of [
      "\u200E",
      "\u200F",
      "\u200E",
      "\uFFF0",
      "\u200E\u200F",
      "\uFFF0\u200E",
      "e\u200E",
    ]) {
      it(`returns true for ${uplus(ch)}…`, function () {
        expect(cldrEscaper.needsEscaping(ch)).to.be.ok;
      });
    }
    for (const ch of [undefined, false, null, " ", "X"]) {
      it(`returns false for ${uplus(ch)}`, function () {
        expect(cldrEscaper.needsEscaping(ch)).to.not.be.ok;
      });
    }
  });
  describe("getMapByName Test", () => {
    const map = cldrEscaper.getMapByName();
    it(`Should exist`, () => {
      expect(map).to.be.ok;
    });
    it(`Should map RLM to 200f`, () => {
      const rlm = map["RLM"]; // "\u200f"
      expect(rlm).to.be.ok;
      expect(rlm.char).to.equal("\u200f");
    });
  });
});
