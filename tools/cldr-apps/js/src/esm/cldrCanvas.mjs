/*
 * cldrCanvas: encapsulate Survey Tool functions related to canvas, for
 * graphical display, such as a line joining a candidate item in the Info Panel
 * to the corresponding item in the Winning or Others column
 */

import * as cldrGui from "./cldrGui.mjs";

const DEFAULT_CANVAS_ID = "cldrCanvas";

function clear() {
  const ctx = getContext();
  if (ctx) {
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  }
}

function connectElements(elTo, elFrom) {
  const ctx = getContext();
  if (ctx) {
    ctx.beginPath();
    ctx.lineWidth = "2";
    ctx.strokeStyle = "rgba(0,0,0,0.5)";
    const canvasRect = ctx.canvas.getBoundingClientRect();
    const fromRect = elFrom.getBoundingClientRect();
    const toRect = elTo.getBoundingClientRect();
    const xa = (fromRect.left + fromRect.right) / 2 - canvasRect.left;
    const ya = (fromRect.top + fromRect.bottom) / 2 - canvasRect.top;
    const xz = (toRect.left + toRect.right) / 2 - canvasRect.left;
    const yz = (toRect.top + toRect.bottom) / 2 - canvasRect.top;
    ctx.moveTo(xa, ya);
    ctx.lineTo(xz, yz);
    ctx.stroke();
  }
}

function getContext() {
  const canvas = getCanvas();
  return canvas?.getContext ? canvas.getContext("2d") : null;
}

function getCanvas() {
  const parent = document.getElementById(cldrGui.MAIN_ID);
  if (!parent) {
    return null;
  }
  const parentRect = parent.getBoundingClientRect();
  let canvas = document.getElementById(DEFAULT_CANVAS_ID);
  if (!canvas) {
    canvas = parent.appendChild(document.createElement("canvas"));
    if (canvas) {
      canvas.id = DEFAULT_CANVAS_ID;
      canvas.width = parentRect.width;
      canvas.height = parentRect.height;
      canvas.style = "position:absolute; pointer-events: none; z-index: 1000";
    }
  } else if (
    canvas.width != parentRect.width ||
    canvas.height != parentRect.height
  ) {
    // Resize if needed to match parent
    canvas.width = parentRect.width;
    canvas.height = parentRect.height;
  }
  return canvas;
}

export { clear, connectElements };
