/*
 * cldrChar: encapsulate Survey Tool functions related to Unicode characters
 */

import { unicodeName } from "unicode-name";

/**
 * Get the first code point in the given string
 *
 * @param {String} s the string
 * @returns {Number} the first code point
 */
function firstCodePoint(s) {
  // To support code points greater than U+FFFF, use codePointAt, NOT charCodeAt.
  return s.codePointAt(0);
}

/**
 * Get the first character in the given string
 *
 * @param {String} s the string
 * @returns {String} the first character
 */
function firstChar(s) {
  // To support code points greater than U+FFFF, use fromCodePoint/codePointAt, NOT fromCharCode/charCodeAt.
  return String.fromCodePoint(s.codePointAt(0));
}

/**
 * Split a string into an array of characters
 *
 * @param {String} s the string
 * @returns {Array} the array
 */
function split(s) {
  // To support codepoints greater than U+FFFF, use split(/(?:)/u), not split("")
  return s.split(/(?:)/u);
}

/**
 * Get the name for the given code point
 *
 * @param {Number} codePoint a code point such as 0x0020
 * @returns {String} the name such as "SPACE"
 */
function name(codePoint) {
  return unicodeName(codePoint);
}

/**
 * Get the "U+..." string representation for the given code point
 *
 * @param {Number} codePoint a code point such as 0x0020
 * @returns {String} the standard notation such as "U+0020"
 */
function uPlus(codePoint) {
  return "U+" + codePoint.toString(16).toUpperCase().padStart(4, "0");
}

/**
 * If the given string is valid "U+..." for a character code point, return that
 * character; otherwise return the string unchanged
 *
 * @param {String} s the string, maybe like "U+662F"
 * @returns {String} the converted string like "是", or the original string
 */
function fromUPlus(s) {
  if (s?.startsWith("U+")) {
    const codePoint = parseInt(s.slice(2), 16);
    if (isAllowed(codePoint)) {
      // To support codepoints greater than U+FFFF, use fromCodePoint, not fromCharCode
      return String.fromCodePoint(codePoint);
    }
  }
  return s;
}

function isAllowed(codePoint) {
  // Allow only standard code points, excluding private use and surrogates
  return (
    codePoint > 0 &&
    codePoint < 0x10ffff &&
    (codePoint & 0xffff) < 0xfffe &&
    (codePoint < 0xd800 || codePoint > 0xdfff)
  );
}

/**
 * Is the given character (single-character string) white space?
 *
 * @param {String} c -- the single-character string
 * @returns {Boolean} true or false
 */
function isWhiteSpace(c) {
  if (typeof c !== "string" || [...c].length !== 1) {
    throw new Error("isWhiteSpace requires a single-character string");
  }
  // Reference: https://util.unicode.org/UnicodeJsps/character.jsp
  return c.match(/\p{White_Space}/u);
}

export {
  firstChar,
  firstCodePoint,
  fromUPlus,
  isWhiteSpace,
  name,
  split,
  uPlus,
};
