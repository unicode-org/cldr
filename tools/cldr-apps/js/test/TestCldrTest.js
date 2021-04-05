/*
 * cldrTest: utilities for cldr-apps js-unittest
 */

/**
 * Remove any leading or trailing whitespace, and replace any sequence of whitespace with a single space
 */
function normalizeWhitespace(s) {
  return s.replace(/^(\s*)|(\s*)$/g, "").replace(/\s+/g, " ");
}

/**
 * Parse the given string as the given mime type
 *
 * @param mimeType such as 'application/xml' or 'text/html'
 * @return the output string, or null for failure
 */
function parseAsMimeType(inputString, mimeType) {
  const doc = new DOMParser().parseFromString(inputString, mimeType);
  if (!doc) {
    console.log("no doc for " + mimeType + ", " + inputString);
    return null;
  }
  const outputString = new XMLSerializer().serializeToString(doc);
  if (!outputString) {
    console.log("no output string for " + mimeType + ", " + inputString);
    return null;
  }
  if (outputString.indexOf("error") !== -1) {
    console.log(
      "parser error for " + mimeType + ", " + inputString + ", " + outputString
    );
    return null;
  }
  return outputString;
}

export { normalizeWhitespace, parseAsMimeType };
