/*
 * cldrDom: encapsulate DOM (Document Object Model) utilities for CLDR Survey Tool
 */
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrText from "./cldrText.mjs";

/**
 * Remove a CSS class from a node
 *
 * @param {Node} obj
 * @param {String} className
 */
function removeClass(obj, className) {
  if (!obj) {
    return obj;
  }
  if (obj.className.indexOf(className) > -1) {
    obj.className = obj.className.substring(className.length + 1);
  }
  return obj;
}

/**
 * Add a CSS class from a node
 *
 * @param {Node} obj
 * @param {String} className
 */
function addClass(obj, className) {
  if (!obj) {
    return obj;
  }
  if (obj.className.indexOf(className) == -1) {
    obj.className = className + " " + obj.className;
  }
  return obj;
}

/**
 * Remove all subnodes
 *
 * @param {Node} td
 */
function removeAllChildNodes(td) {
  if (td == null) {
    return;
  }
  while (td.firstChild) {
    td.removeChild(td.firstChild);
  }
}

/**
 * Set/remove style.display
 */
function setDisplayed(div, visible) {
  if (div === null) {
    console.log("setDisplayed: called on null");
    return;
  } else if (div.domNode) {
    setDisplayed(div.domNode, visible); // recurse, it's a dijit
  } else if (!div.style) {
    console.log(
      "setDisplayed: called on malformed node " +
        div +
        " - no style! " +
        Object.keys(div)
    );
  } else {
    if (visible) {
      div.style.display = "";
    } else {
      div.style.display = "none";
    }
  }
}

/**
 * Create a DOM object with the specified text, tag, and HTML class.
 *
 * @param {String} text textual content of the new object, or null for none
 * @param {String} tag which element type to create, or null for "span"
 * @param {String} className CSS className, or null for none.
 * @return {Object} new DOM object
 */
function createChunk(text, tag, className) {
  if (!tag) {
    tag = "span";
  }
  var chunk = document.createElement(tag);
  if (className) {
    chunk.className = className;
  }
  if (text) {
    chunk.appendChild(document.createTextNode(text));
  }
  return chunk;
}

/**
 * Create a 'link' that goes to a function. By default it's an 'a', but could be a button, etc.
 * @param strid  {String} string to be used with cldrText.get
 * @param fn {function} function, given the DOM obj as param
 * @param tag {String}  tag of new obj.  'a' by default.
 * @return {Element} newobj
 */
function createLinkToFn(strid, fn, tag) {
  if (!tag) {
    tag = "a";
  }
  var msg = cldrText.get(strid);
  var obj = document.createElement(tag);
  obj.appendChild(document.createTextNode(msg));
  if (tag == "a") {
    obj.href = "";
  }
  listenFor(obj, "click", function (e) {
    fn(obj);
    cldrEvent.stopPropagation(e);
    return false;
  });
  return obj;
}

/**
 * Update the item, if it exists
 *
 * @param id ID of DOM node, or a Node itself
 * @param txt text to replace with - should just be plaintext, but currently can be HTML
 */
function updateIf(id, txt) {
  var something;
  if (id instanceof Node) {
    something = id;
  } else {
    something = document.getElementById(id);
  }
  if (something != null) {
    something.innerHTML = txt; // TODO shold only use for plain text
  }
}

/**
 * Add an event listener function to the object.
 *
 * @param {DOM} what object to listen to (or array of them)
 * @param {String} what event, bare name such as 'click'
 * @param {Function} fn function, of the form:
 *        function(e) {
 *          doSomething();
 *          cldrEvent.stopPropagation(e);
 *          return false;
 *        }
 * @param {String} ievent IE name of an event, if not 'on'+what
 * @return {DOM} returns the object what (or the array)
 */
function listenFor(whatArray, event, fn, ievent) {
  function listenForOne(what, event, fn, ievent) {
    if (!what._stlisteners) {
      what._stlisteners = {};
    }

    if (what.addEventListener) {
      if (what._stlisteners[event]) {
        if (what.removeEventListener) {
          what.removeEventListener(event, what._stlisteners[event], false);
        } else {
          console.log("Err: no removeEventListener on " + what);
        }
      }
      what.addEventListener(event, fn, false);
    } else {
      if (!ievent) {
        ievent = "on" + event;
      }
      if (what._stlisteners[event]) {
        what.detachEvent(ievent, what._stlisteners[event]);
      }
      what.attachEvent(ievent, fn);
    }
    what._stlisteners[event] = fn;

    return what;
  }

  if (Array.isArray(whatArray)) {
    for (var k in whatArray) {
      listenForOne(whatArray[k], event, fn, ievent);
    }
    return whatArray;
  } else {
    return listenForOne(whatArray, event, fn, ievent);
  }
}

/**
 * On click, select all
 *
 * @param {Node} obj to listen to
 * @param {Node} targ to select
 */
function clickToSelect(obj, targ) {
  if (!targ) {
    targ = obj;
  }
  listenFor(obj, "click", function (e) {
    if (window.getSelection) {
      window.getSelection().selectAllChildren(targ);
    }
    cldrEvent.stopPropagation(e);
    return false;
  });
  return obj;
}

// replacement for dojo/dom-construct domConstruct.toDom
function construct(html) {
  const renderer = document.createElement("template");
  renderer.innerHTML = html;
  return renderer.content;
}

function appendIcon(toElement, className, title) {
  var e = createChunk(null, "div", className);
  e.title = title;
  toElement.appendChild(e);
  return e;
}

function parentOfType(tag, obj) {
  if (!obj) {
    return null;
  }
  if (obj.nodeName === tag) {
    return obj;
  }
  return parentOfType(tag, obj.parentElement);
}

export {
  addClass,
  appendIcon,
  clickToSelect,
  construct,
  createChunk,
  createLinkToFn,
  listenFor,
  parentOfType,
  removeAllChildNodes,
  removeClass,
  setDisplayed,
  updateIf,
};
