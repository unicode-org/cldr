/*
 * cldrCache: a simple least-recently-used cache for the Survey Tool front end
 *
 * Based partly on: https://stackoverflow.com/questions/996505/lru-cache-implementation-in-javascript
 * post by odinho - Velmont
 */
export class LRU {
  constructor(max = 100) {
    this._max = max;
    this._cache = new Map();
  }

  clear() {
    this._cache.clear();
  }

  get(key) {
    const val = this._cache.get(key);
    if (val) {
      // delete and set again so it's most recent
      this._cache.delete(key);
      this._cache.set(key, val);
    }
    return val;
  }

  set(key, val) {
    if (this._cache.has(key)) {
      // delete before setting again so it's most recent
      this._cache.delete(key);
    } else if (this._cache.size == this._max) {
      this._cache.delete(this._oldest());
    }
    this._cache.set(key, val);
  }

  _oldest() {
    // Iteration happens in insertion order (chronologically), so the first is the oldest
    // Reference: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map
    return this._cache.keys().next().value;
  }
}
