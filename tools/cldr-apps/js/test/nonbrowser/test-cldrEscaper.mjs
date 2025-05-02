import { expect } from "chai";
import mocha from "mocha";

import * as cldrEscaper from "../../src/esm/cldrEscaper.mjs";

function uplus(ch) {
  if (!ch) return ch;
  return "U+" + Number(ch.codePointAt(0)).toString(16);
}

describe("cldrEscaper test", function () {
  describe("LRM/RLM test", function () {
    for (const ch of ["\u200E", "\u200F", "\uFFF0"]) {
      it(`returns true for ${uplus(ch)}`, function () {
        expect(cldrEscaper.needsEscaping(ch)).to.be.ok;
      });
    }
    for (const ch of [undefined, false, null, " ", "X"]) {
      it(`returns false for ${uplus(ch)}`, function () {
        expect(cldrEscaper.needsEscaping(ch)).to.not.be.ok;
      });
    }
  });
  describe("Escaping Test", () => {
    it(`Should return undefined for a non-escapable str`, () => {
      expect(cldrEscaper.getEscapedHtml(`dd/MM/y`)).to.not.be.ok;
    });
    it(`Should return HTML for a non-escapable str`, () => {
      const html = cldrEscaper.getEscapedHtml(`dd‏/MM‏/y`); // U+200F / U+200F here
      expect(html).to.be.ok;
      expect(html).to.contain('class="visible-mark"');
      expect(html).to.contain("RLM");
    });
    it(`Should return hex for a unknown str`, () => {
      const html = cldrEscaper.getEscapedHtml(`\uFFF0`); // U+200F / U+200F here
      expect(html).to.be.ok;
      expect(html).to.contain('class="visible-mark"');
      expect(html).to.contain("U+FFF0");
    });
  });
});
