"use strict";

/**
 * cldrGear: encapsulate functions for Survey Tool "Gear" menu
 * This is the non-dojo version. For dojo, see CldrDojoLoad.js
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrGear = (function () {
  function getItems() {
    const aboutMenu = {
      title: "About",
      special: "about",
      level: 2, // TODO: no indent if !surveyUser; refactor to obviate "level"; make valid html
    };
    const surveyUser = cldrStatus.getSurveyUser();
    if (!surveyUser) {
      return [aboutMenu]; // TODO: enable more menu items when not logged in, e.g., browse
    }
    const sessionId = cldrStatus.getSessionId();
    const surveyUserPerms = cldrStatus.getPermissions();
    // TODO: eliminate surveyUserURL, make these all "specials" like #about -- no url or jsp here
    const surveyUserURL = {
      myAccountSetting: "survey?do=listu",
      disableMyAccount: "lock.jsp",
      xmlUpload: "upload.jsp?a=/cldr-apps/survey&s=" + sessionId,
      manageUser: "survey?do=list",
      flag: "tc-flagged.jsp?s=" + sessionId,
      browse: "browse.jsp",
    };
    return [
      {
        title: "Admin Panel",
        special: "admin",
        display: surveyUser && surveyUser.userlevelName === "ADMIN",
      },
      {
        divider: true,
        display: surveyUser && surveyUser.userlevelName === "ADMIN",
      },

      {
        title: "My Account",
      }, // My Account section

      {
        title: "Settings",
        level: 2,
        url: surveyUserURL.myAccountSetting,
        display: surveyUser && true,
      },
      {
        title: "Lock (Disable) My Account",
        level: 2,
        url: surveyUserURL.disableMyAccount,
        display: surveyUser && true,
      },

      {
        divider: true,
      },
      {
        title: "My Votes",
      }, // My Votes section

      /*
       * This indirectly references "special_oldvotes" in cldrText.js
       */
      {
        special: "oldvotes",
        level: 2,
        display: surveyUserPerms && surveyUserPerms.userCanImportOldVotes,
      },
      {
        special: "recent_activity",
        level: 2,
      },
      {
        title: "Upload XML",
        level: 2,
        url: surveyUserURL.xmlUpload,
      },

      {
        divider: true,
      },
      {
        title: "My Organization(" + cldrStatus.getOrganizationName() + ")",
      }, // My Organization section

      {
        special: "vsummary" /* Cf. special_vsummary */,
        level: 2,
        display: surveyUserPerms && surveyUserPerms.userCanUseVettingSummary,
      },
      {
        title: "List " + cldrStatus.getOrganizationName() + " Users",
        level: 2,
        url: surveyUserURL.manageUser,
        display:
          surveyUserPerms &&
          (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter),
      },
      {
        special: "forum_participation" /* Cf. special_forum_participation */,
        level: 2,
        display: surveyUserPerms && surveyUserPerms.userCanMonitorForum,
      },
      {
        special:
          "vetting_participation" /* Cf. special_vetting_participation */,
        level: 2,
        display:
          surveyUserPerms &&
          (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter),
      },
      {
        title: "LOCKED: Note: your account is currently locked.",
        level: 2,
        display: surveyUserPerms && surveyUserPerms.userIsLocked,
        bold: true,
      },

      {
        divider: true,
      },
      {
        title: "Forum",
      }, // Forum section

      {
        special: "flagged",
        level: 2,
        hasFlag: true,
      },
      {
        special: "mail",
        level: 2,
        display: cldrStatus.getIsUnofficial(),
      },
      {
        special: "bulk_close_posts" /* Cf. special_bulk_close_posts */,
        level: 2,
        display: surveyUser && surveyUser.userlevelName === "ADMIN",
      },

      {
        divider: true,
      },
      {
        title: "Informational",
      }, // Informational section

      {
        special: "statistics",
        level: 2,
      },

      aboutMenu,

      {
        title: "Lookup a code or xpath",
        level: 2,
        url: surveyUserURL.browse,
      },
      {
        special: "error_subtypes",
        level: 2,
        display: surveyUserPerms && surveyUserPerms.userIsTC,
      },
    ];
  }

  let alreadySet = false;

  function set(gearMenuItems) {
    if (!alreadySet) {
      alreadySet = true;
      const parMenu = document.getElementById("manage-list");
      for (let k = 0; k < gearMenuItems.length; k++) {
        const item = gearMenuItems[k];
        (function (item) {
          if (item.display != false) {
            var subLi = document.createElement("li");
            if (item.special) {
              // special items so look up in cldrText.js
              item.title = cldrText.get("special_" + item.special);
              item.url = "#" + item.special;
              item.blank = false;
            }
            if (item.url) {
              let subA = document.createElement("a");

              if (item.hasFlag) {
                addFlagIcon(subA);
              }
              subA.appendChild(document.createTextNode(item.title + " "));
              subA.href = item.url;

              if (item.blank != false) {
                subA.target = "_blank";
                subA.appendChild(
                  cldrDom.createChunk(
                    "",
                    "span",
                    "glyphicon glyphicon-share manage-list-icon"
                  )
                );
              }

              if (item.level) {
                // append it to appropriate levels
                const level = item.level;
                for (let i = 0; i < level - 1; i++) {
                  /*
                   * Indent by creating lists within lists, each list containing only one item.
                   * TODO: indent by a better method. Note that for valid html, ul should contain li;
                   * ul directly containing element other than li is generally invalid.
                   */
                  const ul = document.createElement("ul");
                  const li = document.createElement("li");
                  ul.setAttribute("style", "list-style-type:none");
                  ul.appendChild(li);
                  li.appendChild(subA);
                  subA = ul;
                }
              }
              subLi.appendChild(subA);
            }
            if (!item.url && !item.divider) {
              // if it is pure text/html & not a divider
              if (!item.level) {
                subLi.appendChild(document.createTextNode(item.title + " "));
              } else {
                let subA = null;
                if (item.bold) {
                  subA = document.createElement("b");
                } else if (item.italic) {
                  subA = document.createElement("i");
                } else {
                  subA = document.createElement("span");
                }
                subA.appendChild(document.createTextNode(item.title + " "));

                const level = item.level;
                for (let i = 0; i < level - 1; i++) {
                  const ul = document.createElement("ul");
                  const li = document.createElement("li");
                  ul.setAttribute("style", "list-style-type:none");
                  ul.appendChild(li);
                  li.appendChild(subA);
                  subA = ul;
                }
                subLi.appendChild(subA);
              }
              if (item.divider) {
                subLi.className = "nav-divider";
              }
              parMenu.appendChild(subLi);
            }
            if (item.divider) {
              subLi.className = "nav-divider";
            }
            parMenu.appendChild(subLi);
          }
        })(item);
      }
    }
  }

  function addFlagIcon(el) {
    // forum may need images attached to it
    const img = document.createElement("img");
    img.setAttribute("src", "flag.png");
    img.setAttribute("alt", "flag");
    img.setAttribute("title", "flag.png");
    img.setAttribute("border", 0);
    el.appendChild(img);
  }

  /*
   * Make only these functions accessible from other files:
   */
  return {
    getItems,
    set,

    /*
     * The following are meant to be accessible for unit testing only:
     */
    // test: {
    //   f: f,
    // },
  };
})();
