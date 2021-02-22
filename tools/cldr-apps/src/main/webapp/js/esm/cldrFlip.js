/*
 * Section flipping class
 */
import * as cldrDom from "./cldrDom.js";
import * as cldrSurvey from "./cldrSurvey.js";

function Flipper(ids) {
  this._panes = [];
  this._killfn = [];
  this._map = {};
  for (var k in ids) {
    var id = ids[k];
    var node = document.getElementById(id);
    // TODO if node==null throw
    if (this._panes.length > 0) {
      if (typeof cldrSurvey !== "undefined") {
        cldrDom.setDisplayed(node, false); // hide it
      }
    } else {
      this._visible = id;
    }
    this._panes.push(node);
    this._map[id] = node;
  }
}

/**
 * @param {String} id
 * @param {Node} node - if non null - replace new page with this
 */
Flipper.prototype.flipTo = function (id, node) {
  if (!this._map[id]) {
    return; // TODO throw
  }
  if (this._visible == id && node === null) {
    return; // noop - unless adding s'thing
  }
  if (typeof cldrSurvey !== "undefined") {
    cldrDom.setDisplayed(this._map[this._visible], false);
  }
  for (var k in this._killfn) {
    this._killfn[k]();
  }
  this._killfn = []; // pop?
  if (node !== null && node !== undefined) {
    cldrDom.removeAllChildNodes(this._map[id]);
    if (node.nodeType > 0) {
      this._map[id].appendChild(node);
    } else {
      for (var kk in node) {
        // it's an array, add all
        this._map[id].appendChild(node[kk]);
      }
    }
  }
  if (typeof cldrSurvey !== "undefined") {
    cldrDom.setDisplayed(this._map[id], true);
  }
  this._visible = id;
  return this._map[id];
};

/**
 * @param id page id or null for current
 * @returns
 */
Flipper.prototype.get = function (id) {
  if (id) {
    return this._map[id];
  } else {
    return this._map[this._visible];
  }
};

/**
 * @param id
 */
Flipper.prototype.flipToEmpty = function (id) {
  return this.flipTo(id, []);
};

/**
 * killFn is called on next flip
 *
 * @param killFn the function to call. No params.
 */
Flipper.prototype.addKillFn = function (killFn) {
  this._killfn.push(killFn);
};

/**
 * showfn is called, result is added to the div.  killfn is called when page is flipped.
 *
 * @param showFn
 * @param killFn
 */
Flipper.prototype.addUntilFlipped = function addUntilFlipped(showFn, killFn) {
  this.get().appendChild(showFn());
  this.addKillFn(killFn);
};

export { Flipper };
