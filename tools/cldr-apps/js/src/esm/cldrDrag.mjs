/*
 * cldrDrag: enable dragging border to resize components for Survey Tool
 */

const EDGE_PIXELS = 4;

/**
 * Enable dragging on the border between two elements to resize them
 *
 * @param {Element} elA the first (left or top) element
 * @param {Element} elB the second (right or bottom) element
 * @param {boolean} upDown true to drag up/down; false to drag left/right
 */
function enable(elA, elB, upDown) {
  let xy = 0;

  elB.addEventListener("mousemove", function (e) {
    const r = upDown ? "row-resize" : "col-resize";
    elB.style.cursor = inBorder(e, upDown) ? r : "default";
  });
  elB.addEventListener("mousedown", function (e) {
    if (inBorder(e, upDown)) {
      xy = getEventXY(e, upDown);
      document.addEventListener("mousemove", resize, false);
    }
  });
  document.addEventListener("mouseup", function () {
    document.removeEventListener("mousemove", resize, false);
  });

  function resize(e) {
    xy = doResize(e, elA, elB, upDown, xy);
  }
}

function doResize(e, elA, elB, upDown, xy) {
  const eXY = getEventXY(e, upDown);
  const delta = eXY - xy;
  if (delta) {
    const a = getHeightOrWidth(elA, upDown) + delta;
    const b = getHeightOrWidth(elB, upDown) - delta;
    const aPercent = makePercent(a, b);
    const bPercent = 100 - aPercent;
    setHeightOrWidth(elA, aPercent + "%", upDown);
    setHeightOrWidth(elB, bPercent + "%", upDown);
  }
  e.stopPropagation();
  e.preventDefault();
  return eXY;
}

function inBorder(e, upDown) {
  return (upDown ? e.offsetY : e.offsetX) < EDGE_PIXELS;
}

function getEventXY(e, upDown) {
  return upDown ? e.y : e.x;
}

function getHeightOrWidth(el, upDown) {
  const s = getComputedStyle(el);
  return parseFloat(upDown ? s.height : s.width);
}

function setHeightOrWidth(el, heightOrWidth, upDown) {
  if (upDown) {
    el.style.height = heightOrWidth;
  } else {
    el.style.width = heightOrWidth;
  }
}

/**
 * Calculate 100 * a / (a + b), and confine the result to between 10 and 90
 *
 * @param {number} a
 * @param {number} b
 * @return a number between 10 and 90
 */
function makePercent(a, b) {
  const sum = a + b;
  if (sum <= 0) {
    return 50;
  }
  const percent = (100 * a) / sum;
  if (percent <= 10) {
    return 10;
  } else if (percent >= 90) {
    return 90;
  } else {
    return percent;
  }
}

export { enable };
