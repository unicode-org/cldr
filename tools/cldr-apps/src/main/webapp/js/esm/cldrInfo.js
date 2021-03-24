/*
 * cldrInfo: encapsulate Survey Tool "Info Panel" (right sidebar) functions
 */
import * as cldrDeferHelp from "./cldrDeferHelp.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForum from "./cldrForum.js";
import * as cldrForumPanel from "./cldrForumPanel.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

let unShow = null;
let lastShown = null;

/**
 * Make the object "theObj" cause the info window to show when clicked.
 *
 * @param {String} str
 * @param {Node} tr the TR element that is clicked
 * @param {Node} theObj to listen to
 * @param {Function} fn the draw function
 */
function listen(str, tr, theObj, fn) {
  cldrDom.listenFor(theObj, "click", function (e) {
    show(str, tr, theObj /* hideIfLast */, fn);
    cldrEvent.stopPropagation(e);
    return false;
  });
}

function showNothing() {
  return show(null, null, null, null);
}

function showMessage(str) {
  return show(str, null, null, null);
}

function showWithRow(str, tr) {
  return show(str, tr, null, null);
}

function showRowObjFunc(tr, hideIfLast, fn) {
  return show(null, tr, hideIfLast, fn);
}

/**
 * Display the right-hand "info" panel.
 *
 * @param {String} str the string to show at the top
 * @param {Node} tr the <TR> of the row
 * @param {Object} hideIfLast
 * @param {Function} fn
 */
function show(str, tr, hideIfLast, fn) {
  if (unShow) {
    unShow();
    unShow = null;
  }

  if (tr && tr.sethash) {
    cldrLoad.updateCurrentId(tr.sethash);
  }
  setLastShown(hideIfLast);

  /*
   * This is the temporary fragment used for the
   * "info panel" contents.
   */
  var fragment = document.createDocumentFragment();

  if (tr && tr.theRow) {
    const { theRow } = tr;
    const { helpHtml, rdf } = theRow;
    if (helpHtml || rdf) {
      cldrDeferHelp.addDeferredHelpTo(fragment, helpHtml, rdf);
    }
    // extra attributes
    if (
      theRow.extraAttributes &&
      Object.keys(theRow.extraAttributes).length > 0
    ) {
      var extraHeading = cldrDom.createChunk(
        cldrText.get("extraAttribute_heading"),
        "h3",
        "extraAttribute_heading"
      );
      var extraContainer = cldrDom.createChunk("", "div", "extraAttributes");
      appendExtraAttributes(extraContainer, theRow);
      theHelp.appendChild(extraHeading);
      theHelp.appendChild(extraContainer);
    }
  }

  if (cldrStatus.isDashboard()) {
    fixPopoverVotePos();
  }

  if (str) {
    // If a simple string, clone the string
    var div2 = document.createElement("div");
    div2.innerHTML = str;
    fragment.appendChild(div2);
  }
  // If a generator fn (common case), call it.
  if (fn) {
    unShow = fn(fragment);
  }

  var theVoteinfo = null;
  if (tr && tr.voteDiv) {
    theVoteinfo = tr.voteDiv;
  }
  if (theVoteinfo) {
    fragment.appendChild(theVoteinfo.cloneNode(true));
  }
  if (tr && tr.ticketLink) {
    fragment.appendChild(tr.ticketLink.cloneNode(true));
  }

  // forum stuff
  if (tr && tr.forumDiv) {
    /*
     * The name forumDivClone is a reminder that forumDivClone !== tr.forumDiv.
     * TODO: explain the reason for using cloneNode here, rather than using
     * tr.forumDiv directly. Would it work as well to set tr.forumDiv = forumDivClone,
     * after cloning?
     */
    var forumDivClone = tr.forumDiv.cloneNode(true);
    cldrForumPanel.loadInfo(fragment, forumDivClone, tr); // give a chance to update anything else
    fragment.appendChild(forumDivClone);
  }

  if (tr && tr.theRow && tr.theRow.xpath) {
    fragment.appendChild(
      cldrDom.clickToSelect(
        cldrDom.createChunk(tr.theRow.xpath, "div", "xpath")
      )
    );
  }
  var pucontent = document.getElementById("itemInfo");
  if (!pucontent) {
    console.log("itemInfo not found in show!");
    return;
  }

  // Now, copy or append the 'fragment' to the
  // appropriate spot. This depends on how we were called.
  if (tr) {
    if (cldrStatus.isDashboard()) {
      cldrSurvey.showHelpFixPanel(fragment);
    } else {
      cldrDom.removeAllChildNodes(pucontent);
      pucontent.appendChild(fragment);
    }
  } else {
    if (!cldrStatus.isDashboard()) {
      // show, for example, dataPageInitialGuidance in Info Panel
      var clone = fragment.cloneNode(true);
      cldrDom.removeAllChildNodes(pucontent);
      pucontent.appendChild(clone);
    }
  }
  fragment = null;

  // for the voter
  $(".voteInfo_voterInfo").hover(
    function () {
      var email = $(this).data("email").replace(" (at) ", "@");
      if (email !== "") {
        $(this).html(
          '<a href="mailto:' +
            email +
            '" title="' +
            email +
            '" style="color:black"><span class="glyphicon glyphicon-envelope"></span></a>'
        );
        $(this).closest("td").css("text-align", "center");
        $(this).children("a").tooltip().tooltip("show");
      } else {
        $(this).html($(this).data("name"));
        $(this).closest("td").css("text-align", "left");
      }
    },
    function () {
      $(this).html($(this).data("name"));
      $(this).closest("td").css("text-align", "left");
    }
  );
  if (!cldrStatus.isDashboard()) {
    return pucontent;
  } else {
    return null;
  }
}

function setLastShown(obj) {
  if (lastShown && obj != lastShown) {
    cldrDom.removeClass(lastShown, "pu-select");
    const partr = parentOfType("TR", lastShown);
    if (partr) {
      cldrDom.removeClass(partr, "selectShow");
    }
  }
  if (obj) {
    cldrDom.addClass(obj, "pu-select");
    const partr = parentOfType("TR", obj);
    if (partr) {
      cldrDom.addClass(partr, "selectShow");
    }
  }
  lastShown = obj;
}

function reset() {
  lastShown = null;
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

export { listen, reset, showMessage, showNothing, showRowObjFunc, showWithRow };
