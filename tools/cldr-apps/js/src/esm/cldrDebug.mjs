// this object is defined in SurveyTool.includeJavaScript(),
// and it's exactly the value of the CLDR_FE_DEBUG property.
// It could be JSON, but doesn't have to be, it can be a simple object.
// Example in bootstrap.properties:
//   CLDR_FE_DEBUG={someProperty:true, anArray: [4,5,6]}
// Calling site:
//   if(cldrDebug.get('someProperty')) { … }
//   const {someProperty} = cldrDebug.all(); // destructure

// Our main object
// we get an empty object if none was specified.
const feDebug = cldrFeDebug || {};

/**
 * Return the entire config object.
 *
 * Usage:
 *   `const {someProperty} = cldrDebug.all();`
 * @returns {Object}
 */
function all() {
  return feDebug;
}

/**
 *
 * @param {string} k property name
 * @returns {boolean | Object | string | undefined}
 */
function get(k) {
  return all()[k];
}

export { get, all };
