/*
 * cldrEvent: encapsulate handling of events like mouse clicks, etc.
 * (this is mostly derived from old redesign.js and should be divided
 * into smaller more specific modules)
 */
import * as cldrForum from "./cldrForum.js";
import * as cldrGui from "./cldrGui.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrText from "./cldrText.js";

let sentenceFilter;

let cachedJson; // use a cache because the coverage can change, so we might need to update the menu

let toToggleOverlay;

let oldTypePopup = "";

function startup() {
  // for locale search
  $("body").on("click", "#show-locked", { type: "lock" }, toggleLockedRead);
  $("body").on("click", "#show-read", { type: "read" }, toggleLockedRead);
  $("#locale-info .local-search").keyup(filterAllLocale);
  $(".pull-menu > a").click(interceptPulldownLink);

  // locale chooser intercept
  $("body").on("click", ".locName", interceptLocale);

  // handle the left sidebar
  $("#left-sidebar").hover(
    function () {
      if (
        !$("body").hasClass("disconnected") &&
        !cldrStatus.getAutoImportBusy()
      ) {
        // don't hover if another dialog is open.
        $(this).addClass("active");
        toggleOverlay();
      }
    },
    function () {
      if (
        cldrStatus.getCurrentLocale() ||
        cldrStatus.getCurrentSpecial() != "locales"
      ) {
        // don't stick the sidebar open if we're not in the locale chooser.
        $(this).removeClass("active");
        toggleOverlay();
      }
    }
  );
  $(".refresh-search").click(searchRefresh);

  // help bootstrap -> close popup when click outside
  $("body").on("click", function (e) {
    $('[data-toggle="popover"]').each(function () {
      //the 'is' for buttons that trigger popups
      //the 'has' for icons within a button that triggers a popup
      if (
        !$(this).is(e.target) &&
        $(this).has(e.target).length === 0 &&
        $(".popover").has(e.target).length === 0
      ) {
        $(this).popover("hide");
      }
    });
  });

  // example on hover
  $("body").on(
    "mouseenter",
    ".vetting-page .infos-code, .vetting-page .subSpan",
    function () {
      var example = $(this)
        .closest(".d-disp,.d-item,.d-item-err,.d-item-warn")
        .find(".d-example");
      if (example) {
        $(this)
          .popover({
            html: true,
            placement: "top",
            content: example.html(),
          })
          .popover("show");
      }
    }
  );

  $("body").on(
    "mouseleave",
    ".vetting-page .infos-code, .vetting-page .subSpan",
    function () {
      $(this).popover("hide");
    }
  );
  resizeSidebar();

  $(".tip-log").tooltip({
    placement: "bottom",
  });
  $("body").keydown(function (event) {
    /*
     * Some browsers (e.g., Firefox) treat Backspace (or Delete on macOS) as a shortcut for
     * going to the previous page in the browser's history. That's a problem when we have an
     * input window open for the user to type a new candidate item, especially if the window
     * is still visible but has lost focus. Prevent that behavior for backspace when the input
     * window has lost focus. Formerly, key codes 37 (left arrow) and 39 (right arrow) were used
     * here as shortcuts for chgPage(-1) and chgPage(1), respectively. However, that caused
     * problems similar to the problem with Backspace. Reference: https://unicode.org/cldr/trac/ticket/11218
     */
    if ($(":focus").length === 0) {
      if (event.keyCode === 8) {
        // backspace
        event.preventDefault();
      }
    }
  });
}

/**
 * Size and position the sidebar relative to the header
 */
function resizeSidebar() {
  const sidebar = $("#left-sidebar");
  const header = $("#st-header");
  if (!sidebar) {
    console.log("Missing sidebar in resizeSidebar");
    return;
  }
  if (!header) {
    console.log("Missing header in resizeSidebar");
    return;
  }
  const headerHeight = header.height();
  sidebar.css("height", $(window).height() - headerHeight);
  sidebar.css("top", headerHeight);
}

/**
 * Filter all the locales (first child, then parent so we can build the tree,
 * and let the parent displayed if a child is matched)
 *
 * @param event
 * @returns false (return value is ignored by all callers)
 *
 * This function is called from elsewhere in this file, and from CldrSurveyVettingLoader.js.
 */
function filterAllLocale(event) {
  if ($(this).hasClass("local-search")) {
    $("a.locName").removeClass("active");
    $("#locale-list,#locale-menu").removeClass("active");
  }
  sentenceFilter = $("input.local-search").val().toLowerCase();
  $(".subLocaleList .locName").each(filterLocale); // filtersublocale
  $(".topLocale .locName").each(filterLocale); // filtertolocale

  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
  return false;
}

/**
 * Filter (locked and read-only) with locale
 *
 * @param event
 */
function toggleLockedRead(event) {
  var type = event.data.type;
  if ($(this).is(":checked")) {
    if (type == "read") {
      $(".locName:not(.canmodify):not(.locked)").parent().removeClass("hide");
    } else {
      $(".locName.locked").parent().removeClass("hide");
    }
  } else {
    if (type == "read") {
      $(".locName:not(.canmodify):not(.locked):not(.shown_but_locked)")
        .parent()
        .addClass("hide");
    } else {
      $(".locName.locked:not(.shown_but_locked)").parent().addClass("hide");
    }
  }
  filterAllLocale();
}

/**
 * Hide/show the locale matching the pattern and the checkbox
 */
function filterLocale() {
  var text = $(this).text().toLowerCase();
  var parent = $(this).parent();
  if (
    text.indexOf(sentenceFilter) == 0 &&
    (checkLocaleShow($(this), sentenceFilter.length) || sentenceFilter === text)
  ) {
    parent.removeClass("hide");
    if (parent.hasClass("topLocale")) {
      parent.parent().removeClass("hide");
      parent.next().children("div").removeClass("hide");
    }
  } else {
    if (parent.hasClass("topLocale")) {
      if (parent.next().children("div").not(".hide").length == 0) {
        parent.addClass("hide");
        parent.parent().addClass("hide");
      } else {
        parent.removeClass("hide");
        parent.parent().removeClass("hide");
      }
    } else {
      parent.addClass("hide");
    }
  }
}

/**
 * Should we show this locale considering the checkbox?
 *
 * @param element
 * @param size
 * @return true or false
 */
function checkLocaleShow(element, size) {
  if (size > 0) {
    return true;
  }
  if (element.hasClass("locked") && $("#show-locked").is(":checked")) {
    return true;
  }
  if (
    (!element.hasClass("canmodify") &&
      $("#show-read").is(":checked") &&
      !element.hasClass("locked")) ||
    element.hasClass("canmodify") ||
    element.hasClass("shown_but_locked") // Always show these
  ) {
    return true;
  }
  return false;
}

/**
 * Intercept the click of the locale name
 */
function interceptLocale() {
  var name = $(this).text();
  var source = $(this).attr("title");

  $("input.local-search").val(name);
  $("a.locName").removeClass("active");
  $(this).addClass("active");
  filterAllLocale();
  $("#locale-list").addClass("active");
  $("#locale-menu").addClass("active");
}

/**
 * Sidebar constructor
 *
 * @param json
 */
function unpackMenuSideBar(json) {
  if (json.menus) {
    cachedJson = json;
  } else {
    var lName = json["_v"];
    if (!cachedJson) {
      return;
    }
    json = cachedJson;
    json.covlev_user = lName;
  }
  var menus = json.menus.sections;
  var levelName = json.covlev_user;

  if (!levelName || levelName === "default") {
    levelName = json.covlev_org;
  }
  var menuRoot = $("#locale-menu");
  var level = 0;
  var levels = json.menus.levels;
  var reports = json.reports;

  // get the level number
  $.each(levels, function (index, element) {
    if (element.name == levelName) {
      level = parseInt(element.level);
    }
  });

  if (level === 0) {
    // We couldn't find the level name. Try again as if 'auto'.
    levelName = json.covlev_org;

    //get the level number
    $.each(levels, function (index, element) {
      if (element.name == levelName) {
        level = parseInt(element.level);
      }
    });

    if (level === 0) {
      // Still couldn't.
      level = 10; // fall back to CORE.
    }
  }

  var html = "<ul>";
  if (!cldrStatus.isVisitor()) {
    var tmp = null;
    var reportHtml = "";
    $.each(reports, function (index, element) {
      if (element.url !== "dashboard") {
        reportHtml +=
          '<li class="list-unstyled review-link" data-url="' +
          element.url +
          '"><div>' +
          element.display +
          "</div></li>";
      } else {
        tmp = element;
      }
    });

    if (tmp) {
      html +=
        '<li class="list-unstyled review-link" data-url="' +
        tmp.url +
        '"><div>' +
        tmp.display +
        '<span class="pull-right glyphicon glyphicon-home" style="position:relative;top:2px;right:1px;"></span></div></li>';
    }

    html +=
      '<li class="list-unstyled open-menu"><div>Reports<span class="pull-right glyphicon glyphicon-chevron-right"></span></div>';
    html += '<ul class="second-level">';
    html += reportHtml;
    html += "</ul></li>";
  }
  html += '<li class="list-unstyled" id="forum-link"><div>';
  html += 'Forum<span class="pull-right glyphicon glyphicon-comment"></span>';
  html += '<span class="' + cldrForum.SUMMARY_CLASS + '"></span>';
  html += "</div></li>";
  html += "</ul>";

  html += "<ul>";
  $.each(menus, function (index, element) {
    var menuName = element.name;
    let childCount = 0;
    html +=
      '<li class="list-unstyled open-menu"><div>' +
      menuName +
      '<span class="pull-right glyphicon glyphicon-chevron-right"></span></div><ul class="second-level">';
    $.each(element.pages, function (index, element) {
      var pageName = element.name;
      var pageId = element.id;
      $.each(element.levs, function (index, element) {
        if (parseInt(element) <= level) {
          html +=
            '<li class="sidebar-chooser list-unstyled" id="' +
            pageId +
            '"><div>' +
            pageName +
            "</div></li>";
          childCount++;
        }
      });
    });
    if (childCount === 0) {
      html += "<i>" + cldrText.get("coverage_no_items") + "</i>";
    }
    html += "</ul></li>";
  });

  html += "</ul>";

  menuRoot.html(html);
  menuRoot.find(".second-level").hide();

  // don't slide up and down infinitely
  $(".second-level").click(function (event) {
    event.stopPropagation();
    event.preventDefault();
  });

  // slide down the menu
  $(".open-menu").click(function () {
    $("#locale-menu .second-level").slideUp();
    $(".open-menu .glyphicon")
      .removeClass("glyphicon-chevron-down")
      .addClass("glyphicon-chevron-right");

    $(this).children("ul").slideDown();
    $(this)
      .find(".glyphicon")
      .removeClass("glyphicon-chevron-right")
      .addClass("glyphicon-chevron-down");
  });

  // menu
  $(".sidebar-chooser").click(function () {
    cldrStatus.setCurrentPage($(this).attr("id"));
    cldrStatus.setCurrentSpecial("");
    cldrLoad.reloadV();
    $("#left-sidebar").removeClass("active");
    toggleOverlay();
  });

  // review link
  $(".review-link").click(function () {
    $("#left-sidebar").removeClass("active");
    toggleOverlay();
    const url = $(this).data("url");
    if (url === "dashboard") {
      cldrStatus.setCurrentSpecial("general");
      cldrGui.insertDashboard();
    } else {
      $("#OtherSection").hide(); // Don't hide the other section when showing the dashboard.
      cldrStatus.setCurrentSpecial(url);
      cldrStatus.setCurrentId("");
      cldrStatus.setCurrentPage("");
      cldrLoad.reloadV();
    }
  });

  // forum link
  $("#forum-link").click(function () {
    if (cldrForum) {
      cldrForum.reload();
    }
  });

  const curLocale = cldrStatus.getCurrentLocale();
  if (curLocale) {
    $('a[data-original-title="' + curLocale + '"]').click();
    $("#title-coverage").show();
  }

  // reopen the menu to the current page
  const curPage = cldrStatus.getCurrentPage();
  if (curPage) {
    var menu = $("#locale-menu #" + curPage);
    menu.closest(".open-menu").click();
  }

  // fill the cldrForum.SUMMARY_CLASS element created above
  cldrForum.refreshSummary();
}

/**
 * Force the left sidebar to open
 */
function forceSidebar() {
  searchRefresh();
  $("#left-sidebar").mouseenter();
}

/**
 * Refresh the search field
 */
function searchRefresh() {
  $(".local-search").val("");
  $(".local-search").keyup();
}

/**
 * Toggle the overlay of the menu
 *
 * Called from elsewhere in this file, and also by toggleFix in review.js
 */
function toggleOverlay() {
  var overlay = $("#overlay");
  var sidebar = $("#left-sidebar");
  if (!sidebar.hasClass("active")) {
    overlay.removeClass("active");
    toToggleOverlay = true;

    setTimeout(function () {
      if (toToggleOverlay) {
        overlay.css("z-index", "-10");
      }
    }, 500 /* half a second */);
  } else {
    toToggleOverlay = false;
    overlay.css("z-index", "800");
    overlay.addClass("active");
  }
}

/**
 * Hide both the overlay and left sidebar
 */
function hideOverlayAndSidebar() {
  var sidebar = $("#left-sidebar");
  sidebar.removeClass("active");
  toggleOverlay();
}

/**
 * Show the help popup in the center of the screen
 *
 * @param type - "warning" or "danger"
 * @param content
 */
function popupAlert(type, content) {
  const duration = 4000; /* four seconds */
  const alert = $("#progress").closest(".alert");
  alert
    .removeClass("alert-warning")
    .removeClass("alert-info")
    .removeClass("alert-danger")
    .removeClass("alert-success");
  alert.addClass("alert-" + type);
  $("#progress_oneword").html(content);

  if (oldTypePopup != type) {
    if (!alert.is(":visible")) {
      alert.fadeIn();
      if (duration > 0) {
        setTimeout(function () {
          alert.fadeOut();
        }, duration);
      }
    }
    oldTypePopup = type;
  }
}

/**
 * Create/update the pull-down menu popover
 *
 * @param event
 */
function interceptPulldownLink(event) {
  var menu = $(this).closest(".pull-menu");
  menu
    .popover("destroy")
    .popover({
      placement: "bottom",
      html: true,
      content: menu.children("ul").html(),
      trigger: "manual",
      delay: 1500,
      template:
        '<div class="popover" onmouseover="$(this).mouseleave(function() {$(this).fadeOut(); });">' +
        '<div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3>' +
        '<div class="popover-content"><p></p></div></div></div>',
    })
    .click(function (e) {
      e.preventDefault();
    })
    .popover("show");

  event.preventDefault();
  event.stopPropagation();
}

/**
 * Add some label with a tooltip to every icon
 *
 * Called only from review.js -- not yet used for !USE_DOJO
 */
function labelizeIcon() {
  var icons = [
    {
      selector: ".d-dr-approved",
      type: "success",
      text: "Approved",
      title: 'The "Proposed" (winning) value will be in the release.',
    },
    {
      selector: ".d-dr-contributed",
      type: "success",
      text: "Contributed",
      title:
        'The "Proposed" (winning) value will be in the release (with a slightly lower status).',
    },
    {
      selector: ".d-dr-provisional",
      type: "warning",
      text: "Provisional",
      title:
        'There is a "Proposed" (winning) value, but it doesn\'t have enough votes.',
    },
    {
      selector: ".d-dr-unconfirmed",
      type: "warning",
      text: "Unconfirmed",
      title:
        'There is a "Proposed" (winning) value, but it doesn\'t have enough votes.',
    },
    {
      selector: ".d-dr-inherited-provisional",
      type: "inherited-provisional",
      text: "Inherited and Provisional",
      title: 'The "Proposed" (winning) value is inherited and provisional.',
    },
    {
      selector: ".d-dr-inherited-unconfirmed",
      type: "inherited-unconfirmed",
      text: "Inherited and Unconfirmed",
      title: 'The "Proposed" (winning) value is inherited and unconfirmed.',
    },
    {
      selector: ".d-dr-missing",
      type: "warning",
      text: "Missing",
      title: "There is no winning value. The inherited value will be used.",
    },
    {
      selector: ".i-star",
      type: "primary",
      text: "Baseline",
      title:
        "The baseline value, which was approved in the last release, plus latest XML fixes by the Technical Committee, if any.",
    },
  ];

  $.each(icons, function (index, element) {
    $(element.selector).each(function () {
      if ($(this).next(".label").length !== 0) {
        $(this).next().remove();
      }
      $(this).after(
        '<div class="label label-' +
          element.type +
          ' label-icon">' +
          element.text +
          "</div>"
      );
      $(this).next().tooltip({ title: element.title });
    });
  });
}

/**
 * Used from within event handlers. cross platform 'stop propagation'
 *
 * @param e event
 * @returns true or false
 */
function stopPropagation(e) {
  if (!e) {
    return false;
  } else if (e.stopPropagation) {
    return e.stopPropagation();
  } else if (e.cancelBubble) {
    return e.cancelBubble();
  } else {
    // hope for the best
    return false;
  }
}

export {
  filterAllLocale,
  forceSidebar,
  hideOverlayAndSidebar,
  popupAlert,
  resizeSidebar,
  searchRefresh,
  startup,
  stopPropagation,
  unpackMenuSideBar,
};
