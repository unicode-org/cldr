// Set operations

/**
 * @param a {Set}
 * @param b {Set}
 * @returns a ∖ b
 */
function minus(a, b) {
  const x = new Set();
  for (const i of a.values()) {
    if (!b.has(i)) {
      x.add(i);
    }
  }
  return x;
}

/**
 * @param a {Set}
 * @param b {Set}
 * @returns a ∪ b
 */
function union(a, b) {
  const x = new Set();
  for (const i of a.values()) {
    x.add(i);
  }
  for (const i of b.values()) {
    x.add(i);
  }
  return x;
}

/**
 * Convert set to array.
 * asList(new Set(1,2,3)) === [1,2,3]
 * @param a {Set}
 * @returns array of a’s values.
 */
function asList(a) {
  const l = [];
  for (const i of a.values()) {
    l.push(i);
  }
  return l;
}

export { minus, union, asList };
