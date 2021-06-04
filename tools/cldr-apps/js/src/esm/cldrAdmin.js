/*
 * cldrAdmin: encapsulate the Admin Panel.
 */
import * as cldrAccount from "./cldrAccount.js";
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

let panelLast = null;
let panels = {};
let panelFirst = null;

let exceptions = [];
let exceptionNames = {};

// called as special.load
function load() {
  cldrInfo.showNothing();

  const ourDiv = document.createElement("div");
  const surveyUser = cldrStatus.getSurveyUser();
  const hasPermission = surveyUser && surveyUser.userlevelName === "ADMIN";
  if (!hasPermission) {
    ourDiv.innerHTML = cldrText.get("E_NO_PERMISSION");
  } else {
    ourDiv.innerHTML = getHtml();
  }
  // caution: ourDiv isn't added to DOM until we call flipToOtherDiv
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
  if (hasPermission) {
    loadAdminPanel();
  }
}

function getHtml() {
  let html =
    cldrStatus.logoIcon() +
    "<h2>Survey Tool Administration | " +
    window.location.hostname +
    "</h2>\n" +
    "<a href='#createAndLogin'>CreateAndLogin</a>\n" +
    " | <a href='#vetting_participation2///'>VettingParticipation2 (Locales.txt)</a>\n" +
    "<div style='float: right; font-size: x-small;'>" +
    "<span id='visitors'></span></div>\n" +
    "<hr />\n" +
    "<div class='fnotebox'>" +
    "For instructions, see <a href='http://cldr.unicode.org/index/survey-tool/admin'>Admin Docs</a>.<br />" +
    "Tabs do not (currently) auto update. Click a tab again to update.</div>\n" +
    "<div id='adminStuff'></div>\n";
  return html;
}

function loadAdminPanel() {
  const adminStuff = document.getElementById("adminStuff");
  if (!adminStuff) {
    return;
  }
  const content = document.createDocumentFragment();
  const list = document.createElement("ul");
  list.className = "adminList";
  content.appendChild(list);

  addAdminPanel("admin_users", adminUsers, list, content);
  addAdminPanel("admin_threads", adminThreads, list, content);
  addAdminPanel("admin_exceptions", adminExceptions, list, content);
  addAdminPanel("admin_settings", adminSettings, list, content);

  // last panel loaded.
  // If it's in the hashtag, use it, otherwise first.
  // TODO: this doesn't work since we no longer have "#!";
  // if needed, revise the mechanism; see panelSwitch
  // if (window.location.hash && window.location.hash.indexOf("#!") == 0) {
  //  panelSwitch(window.location.hash.substring(2));
  // }
  if (!panelLast) {
    // not able to load anything.
    panelSwitch(panelFirst.type);
  }
  adminStuff.appendChild(content);
}

function addAdminPanel(type, fn, list, content) {
  const panel = (panels[type] = {
    type: type,
    name: cldrText.get(type) || type,
    desc:
      cldrText.get(type + "_desc") ||
      "(no description - missing from cldrText)",
    fn: fn,
  });
  panel.div = document.createElement("div");
  panel.div.style.display = "none";
  panel.div.className = "adminPanel";

  const h = document.createElement("h3");
  h.className = "adminTitle";
  h.appendChild(document.createTextNode(panel.desc || type));
  panel.div.appendChild(h);

  panel.udiv = document.createElement("div");
  panel.div.appendChild(panel.udiv);

  panel.listItem = document.createElement("li");
  panel.listItem.appendChild(document.createTextNode(panel.name || type));
  panel.listItem.title = panel.desc || type;
  panel.listItem.className = "notselected";
  panel.listItem.onclick = function (e) {
    panelSwitch(panel.type);
    return false;
  };
  list.appendChild(panel.listItem);

  content.appendChild(panel.div);

  if (!panelFirst) {
    panelFirst = panel;
  }
}

function adminUsers(div) {
  const frag = document.createDocumentFragment();
  const u = document.createElement("div");
  u.appendChild(document.createTextNode("Loading..."));
  frag.appendChild(u);
  cldrDom.removeAllChildNodes(div);
  div.appendChild(frag);
  loadOrFail("do=users", function (json) {
    loadAdminUsers(json, u);
  });
}

function adminThreads(div) {
  const frag = document.createDocumentFragment();
  div.className = "adminThreads";
  const u = cldrDom.createChunk("Loading...", "div", "adminThreadList");
  const stack = cldrDom.createChunk(null, "div", "adminThreadStack");
  frag.appendChild(u);
  frag.appendChild(stack);
  const c2s = cldrDom.createChunk(
    cldrText.get("clickToSelect"),
    "button",
    "clickToSelect"
  );
  cldrDom.clickToSelect(c2s, stack);

  cldrDom.removeAllChildNodes(div);
  div.appendChild(c2s);
  div.appendChild(frag);
  loadOrFail("do=threads", function (json) {
    loadAdminThreads(json, u, stack);
  });
}

function adminSettings(div) {
  const frag = document.createDocumentFragment();
  div.className = "adminSettings";
  const u = cldrDom.createChunk("Loading...", "div", "adminSettingsList");
  frag.appendChild(u);
  loadOrFail("do=settings", function (json) {
    loadAdminSettings(json, u);
  });
  cldrDom.removeAllChildNodes(div);
  div.appendChild(frag);
}

function loadAdminUsers(json, u) {
  const frag2 = document.createDocumentFragment();

  if (!json || !json.users || Object.keys(json.users) == 0) {
    frag2.appendChild(document.createTextNode(cldrText.get("No users.")));
  } else {
    for (let sess in json.users) {
      const cs = json.users[sess];
      const user = cldrDom.createChunk(null, "div", "adminUser");
      user.appendChild(
        cldrDom.createChunk("Session: " + sess, "span", "adminUserSession")
      );
      if (cs.user) {
        user.appendChild(cldrAccount.createUser(cs.user));
      } else {
        user.appendChild(
          cldrDom.createChunk("(anonymous)", "div", "adminUserUser")
        );
      }
      /*
       * cs.lastBrowserCallMillisSinceEpoch = time elapsed in millis since server heard from client
       * cs.lastActionMillisSinceEpoch = time elapsed in millis since user did active action
       * cs.millisTillKick = how many millis before user will be kicked if inactive
       */
      user.appendChild(
        cldrDom.createChunk(
          "LastCall: " +
            cs.lastBrowserCallMillisSinceEpoch +
            ", LastAction: " +
            cs.lastActionMillisSinceEpoch +
            ", IP: " +
            cs.ip +
            ", ttk:" +
            (parseInt(cs.millisTillKick) / 1000).toFixed(1) +
            "s",
          "span",
          "adminUserInfo"
        )
      );

      const unlinkButton = cldrDom.createChunk(
        cldrText.get("admin_users_action_kick"),
        "button",
        "admin_users_action_kick"
      );
      user.appendChild(unlinkButton);
      unlinkButton.onclick = function (e) {
        unlinkButton.className = "deactivated";
        unlinkButton.onclick = null;
        loadOrFail("do=unlink&s=" + cs.id, function (json) {
          cldrDom.removeAllChildNodes(unlinkButton);
          if (json.removing == null) {
            unlinkButton.appendChild(
              document.createTextNode("Already Removed")
            );
          } else {
            unlinkButton.appendChild(document.createTextNode("Removed."));
          }
        });
        return cldrEvent.stopPropagation(e);
      };
      frag2.appendChild(user);
      frag2.appendChild(document.createElement("hr"));
    }
  }
  cldrDom.removeAllChildNodes(u);
  u.appendChild(frag2);
}

function loadAdminThreads(json, u, stack) {
  if (!json || !json.threads || Object.keys(json.threads.all) == 0) {
    cldrDom.removeAllChildNodes(u);
    u.appendChild(document.createTextNode(cldrText.get("No threads.")));
  } else {
    const frag2 = document.createDocumentFragment();
    cldrDom.removeAllChildNodes(stack);
    stack.innerHTML = cldrText.get("adminClickToViewThreads");
    let deadThreads = {};
    if (json.threads.dead) {
      const header = cldrDom.createChunk(
        cldrText.get("adminDeadThreadsHeader"),
        "div",
        "adminDeadThreadsHeader"
      );
      const deadul = cldrDom.createChunk("", "ul", "adminDeadThreads");
      for (let jj = 0; jj < json.threads.dead.length; jj++) {
        const theThread = json.threads.dead[jj];
        const deadLi = cldrDom.createChunk("#" + theThread.id, "li");
        deadThreads[theThread.id] = theThread.text;
        deadul.appendChild(deadLi);
      }
      header.appendChild(deadul);
      stack.appendChild(header);
    }
    for (let id in json.threads.all) {
      const t = json.threads.all[id];
      const thread = cldrDom.createChunk(null, "div", "adminThread");
      const tid = cldrDom.createChunk(id, "span", "adminThreadId");
      thread.appendChild(tid);
      if (deadThreads[id]) {
        tid.className = tid.className + " deadThread";
      }
      thread.appendChild(
        cldrDom.createChunk(t.name, "span", "adminThreadName")
      );
      thread.appendChild(
        cldrDom.createChunk(
          cldrText.get(t.state),
          "span",
          "adminThreadState_" + t.state
        )
      );
      thread.onclick = (function (t, id) {
        return function () {
          stack.innerHTML = "<b>" + id + ":" + t.name + "</b>\n";
          if (deadThreads[id]) {
            stack.appendChild(
              cldrDom.createChunk(deadThreads[id], "pre", "deadThreadInfo")
            );
          }
          stack.appendChild(
            cldrDom.createChunk("\n\n```\n", "pre", "textForTrac")
          );
          for (let q in t.stack) {
            stack.innerHTML = stack.innerHTML + t.stack[q] + "\n";
          }
          stack.appendChild(
            cldrDom.createChunk("```\n\n", "pre", "textForTrac")
          );
        };
      })(t, id);
      frag2.appendChild(thread);
    }
    cldrDom.removeAllChildNodes(u);
    u.appendChild(frag2);
  }
}

function adminExceptions(div) {
  const frag = document.createDocumentFragment();

  div.className = "adminThreads";
  const v = cldrDom.createChunk(null, "div", "adminExceptionList");
  v.setAttribute("id", "admin_v");
  const stack = cldrDom.createChunk(null, "div", "adminThreadStack");
  stack.setAttribute("id", "admin_stack");

  frag.appendChild(v);
  const u = cldrDom.createChunk(null, "div");
  u.setAttribute("id", "admin_u");
  v.appendChild(u);
  frag.appendChild(stack);

  const c2s = cldrDom.createChunk(
    cldrText.get("clickToSelect"),
    "button",
    "clickToSelect"
  );
  cldrDom.clickToSelect(c2s, stack);

  cldrDom.removeAllChildNodes(div);
  div.appendChild(c2s);

  exceptions = [];
  exceptionNames = {};

  div.appendChild(frag);
  const loading = cldrDom.createChunk(
    cldrText.get("loading"),
    "p",
    "adminExceptionFooter"
  );
  loading.setAttribute("id", "admin_loading");
  v.appendChild(loading);
  loadNext(null); // load the first exception
}

function loadNext(from) {
  let append = "do=exceptions";
  if (from) {
    append = append + "&before=" + from;
  }
  console.log("Loading: " + append);
  loadOrFail(append, function (json) {
    loadAdminExceptions(json, from);
  });
}

function loadAdminExceptions(json, from) {
  const u = document.getElementById("admin_u");
  const v = document.getElementById("admin_v");
  const loading = document.getElementById("admin_loading");
  const stack = document.getElementById("admin_stack");

  if (!json || !json.exceptions || !json.exceptions.entry) {
    if (!from) {
      v.appendChild(
        cldrDom.createChunk(
          cldrText.get("no_exceptions"),
          "p",
          "adminExceptionFooter"
        )
      );
    } else {
      // just the last one
      v.removeChild(loading);
      v.appendChild(
        cldrDom.createChunk(
          cldrText.get("last_exception"),
          "p",
          "adminExceptionFooter"
        )
      );
    }
  } else {
    if (json.exceptions.entry.time == from) {
      console.log("Asked for <" + from + " but got =" + from);
      v.removeChild(loading);
      return;
    }
    const frag2 = document.createDocumentFragment();
    if (!from) {
      cldrDom.removeAllChildNodes(stack);
      stack.innerHTML = cldrText.get("adminClickToViewExceptions");
    }
    // TODO: if(json.threads.dead) frag2.appendChunk(json.threads.dead.toString(),"span","adminDeadThreads");
    if (json.exceptions.entry) {
      const entry = json.exceptions.entry;
      exceptions.push(json.exceptions.entry);
      const exception = cldrDom.createChunk(null, "div", "adminException");
      if (entry.header && entry.header.length < 80) {
        exception.appendChild(
          cldrDom.createChunk(entry.header, "span", "adminExceptionHeader")
        );
      } else {
        const t = cldrDom.createChunk(
          entry.header.substring(0, 80) + "...",
          "span",
          "adminExceptionHeader"
        );
        t.title = entry.header;
        exception.appendChild(t);
      }
      exception.appendChild(
        cldrDom.createChunk(entry.DATE, "span", "adminExceptionDate")
      );
      const clicky = (function (entry) {
        return function (event) {
          var frag3 = document.createDocumentFragment();
          frag3.appendChild(
            cldrDom.createChunk(entry.header, "span", "adminExceptionHeader")
          );
          frag3.appendChild(
            cldrDom.createChunk(entry.DATE, "span", "adminExceptionDate")
          );

          if (entry.UPTIME) {
            frag3.appendChild(
              cldrDom.createChunk(entry.UPTIME, "span", "adminExceptionUptime")
            );
          }
          if (entry.CTX) {
            frag3.appendChild(
              cldrDom.createChunk(entry.CTX, "span", "adminExceptionUptime")
            );
          }
          for (let q in entry.fields) {
            const f = entry.fields[q];
            const k = Object.keys(f);
            frag3.appendChild(cldrDom.createChunk(k[0], "h4", "textForTrac"));
            frag3.appendChild(
              cldrDom.createChunk("\n```", "pre", "textForTrac")
            );
            frag3.appendChild(
              cldrDom.createChunk(f[k[0]], "pre", "adminException" + k[0])
            );
            frag3.appendChild(
              cldrDom.createChunk("```\n", "pre", "textForTrac")
            );
          }

          if (entry.LOGSITE) {
            frag3.appendChild(
              cldrDom.createChunk("LOGSITE\n", "h4", "textForTrac")
            );
            frag3.appendChild(
              cldrDom.createChunk("\n```", "pre", "textForTrac")
            );
            frag3.appendChild(
              cldrDom.createChunk(entry.LOGSITE, "pre", "adminExceptionLogsite")
            );
            frag3.appendChild(
              cldrDom.createChunk("```\n", "pre", "textForTrac")
            );
          }
          cldrDom.removeAllChildNodes(stack);
          stack.appendChild(frag3);
          cldrEvent.stopPropagation(event);
          return false;
        };
      })(entry);
      cldrDom.listenFor(exception, "click", clicky);
      const head = exceptionNames[entry.header];
      if (head) {
        if (!head.others) {
          head.others = [];
          head.count = document.createTextNode("");
          const countSpan = document.createElement("span");
          countSpan.appendChild(head.count);
          countSpan.className = "adminExceptionCount";
          cldrDom.listenFor(countSpan, "click", function (e) {
            // prepare div
            if (!head.otherdiv) {
              head.otherdiv = cldrDom.createChunk(
                null,
                "div",
                "adminExceptionOtherList"
              );
              head.otherdiv.appendChild(
                cldrDom.createChunk(cldrText.get("adminExceptionDupList"), "h4")
              );
              for (let k in head.others) {
                head.otherdiv.appendChild(head.others[k]);
              }
            }
            cldrDom.removeAllChildNodes(stack);
            stack.appendChild(head.otherdiv);
            cldrEvent.stopPropagation(e);
            return false;
          });
          head.appendChild(countSpan);
        }
        head.others.push(exception);
        head.count.nodeValue = cldrText.sub("adminExceptionDup", [
          head.others.length,
        ]);
        head.otherdiv = null; // reset
      } else {
        frag2.appendChild(exception);
        exceptionNames[entry.header] = exception;
      }
    }
    u.appendChild(frag2);

    if (json.exceptions.entry && json.exceptions.entry.time) {
      if (exceptions.length > 0 && exceptions.length % 8 == 0) {
        v.removeChild(loading);
        const more = cldrDom.createChunk(
          cldrText.get("more_exceptions"),
          "p",
          "adminExceptionMore adminExceptionFooter"
        );
        more.setAttribute("id", "admin_more");
        more.onclick = more.onmouseover = function () {
          v.removeChild(more);
          v.appendChild(loading);
          loadNext(json.exceptions.entry.time);
          return false;
        };
        v.appendChild(more);
      } else {
        setTimeout(function () {
          loadNext(json.exceptions.entry.time);
        }, 500);
      }
    }
  }
}

function loadAdminSettings(json, u) {
  if (!json || !json.settings || Object.keys(json.settings.all) == 0) {
    cldrDom.removeAllChildNodes(u);
    u.appendChild(document.createTextNode(cldrText.get("nosettings")));
  } else {
    const frag2 = document.createDocumentFragment();
    for (let id in json.settings.all) {
      const t = json.settings.all[id];
      const thread = cldrDom.createChunk(null, "div", "adminSetting");
      thread.appendChild(cldrDom.createChunk(id, "span", "adminSettingId"));
      if (id === "CLDR_HEADER") {
        // TODO: simplify -- functions are too deeply nested
        // TODO: fix the "change" feature or remove it; it was already broken before CLDR-14251
        (function (theHeader, theValue) {
          let setHeader = appendInputBox(thread, "adminSettingsChangeTemp");
          setHeader.value = theValue;
          setHeader.stChange = function (onOk, onErr) {
            loadOrFail(
              "do=settings_set&setting=" + theHeader,
              function (json) {
                if (!json || !json.settings_set || !json.settings_set.ok) {
                  onErr(cldrText.get("failed"));
                  onErr(json.settings_set.err);
                } else {
                  if (json.settings_set[theHeader]) {
                    setHeader.value = json.settings_set[theHeader];
                    if (theHeader == "CLDR_HEADER") {
                      cldrSurvey.updateSpecialHeader(setHeader.value);
                    }
                  } else {
                    setHeader.value = "";
                    if (theHeader == "CLDR_HEADER") {
                      cldrSurvey.updateSpecialHeader(null);
                    }
                  }
                  onOk(cldrText.get("changed"));
                }
              },
              setHeader.value /* postData = last arg to loadOrFail */
            );
            return false;
          };
        })(id, t); // call it

        if (id === "CLDR_HEADER") {
          cldrSurvey.updateSpecialHeader(t);
        }
      } else {
        thread.appendChild(cldrDom.createChunk(t, "span", "adminSettingValue"));
      }
      frag2.appendChild(thread);
    }
    cldrDom.removeAllChildNodes(u);
    u.appendChild(frag2);
  }
}

function loadOrFail(urlAppend, loadHandler, postData) {
  const ourUrl =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=admin_panel&" +
    urlAppend +
    "&s=" +
    cldrStatus.getSessionId() +
    cldrSurvey.cacheKill();

  const xhrArgs = {
    url: ourUrl,
    handleAs: "json",
    load: loadHandler,
    error: loadOrFailErrorHandler,
    postData: postData,
  };

  if (postData) {
    /*
     * Make a POST request
     */
    console.log("admin post: ourUrl: " + ourUrl + " data:" + postData);
    xhrArgs.headers = {
      "Content-Type": "text/plain",
    };
  } else {
    /*
     * Make a GET request
     */
    console.log("admin get: ourUrl: " + ourUrl);
  }
  cldrAjax.sendXhr(xhrArgs);
}

function loadOrFailErrorHandler(err) {
  const el = document.getElementById("adminStuff");
  el.className = "ferrbox";
  el.innerHTML =
    "Error while loading: <div style='border: 1px solid red;'>" +
    err +
    "</div>";
}

function panelSwitch(name) {
  if (panelLast) {
    panelLast.div.style.display = "none";
    panelLast.listItem.className = "notselected";
    panelLast = null;
  }
  if (name && panels[name]) {
    panelLast = panels[name];
    panelLast.listItem.className = "selected";
    panelLast.fn(panelLast.udiv);
    panelLast.div.style.display = "block";
    // TODO: implement without "#!"; see "#!" in loadAdminPanel
    //// window.location.hash = "#!" + name;
  }
}

function appendInputBox(parent, which) {
  const label = cldrDom.createChunk(cldrText.get(which), "div", which);
  const input = document.createElement("input");
  input.stChange = function (onOk, onErr) {};
  const change = cldrDom.createChunk(
    cldrText.get("appendInputBoxChange"),
    "button",
    "appendInputBoxChange"
  );
  const cancel = cldrDom.createChunk(
    cldrText.get("appendInputBoxCancel"),
    "button",
    "appendInputBoxCancel"
  );
  const notify = document.createElement("div");
  notify.className = "appendInputBoxNotify";
  input.className = "appendInputBox";
  label.appendChild(change);
  label.appendChild(cancel);
  label.appendChild(notify);
  label.appendChild(input);
  parent.appendChild(label);
  input.label = label;

  const doChange = function () {
    cldrDom.addClass(label, "d-item-selected");
    cldrDom.removeAllChildNodes(notify);
    notify.appendChild(cldrDom.createChunk(cldrText.get("loading"), "i"));
    const onOk = function (msg) {
      cldrDom.removeClass(label, "d-item-selected");
      cldrDom.removeAllChildNodes(notify);
      notify.appendChild(
        hideAfter(cldrDom.createChunk(msg, "span", "okayText"))
      );
    };
    const onErr = function (msg) {
      cldrDom.removeClass(label, "d-item-selected");
      cldrDom.removeAllChildNodes(notify);
      notify.appendChild(cldrDom.createChunk(msg, "span", "stopText"));
    };
    input.stChange(onOk, onErr);
  };

  const changeFn = function (e) {
    doChange();
    cldrEvent.stopPropagation(e);
    return false;
  };
  const cancelFn = function (e) {
    input.value = "";
    doChange();
    cldrEvent.stopPropagation(e);
    return false;
  };
  const keypressFn = function (e) {
    if (!e || !e.keyCode) {
      return true; // not getting the point here.
    } else if (e.keyCode == 13) {
      doChange();
      return false;
    } else {
      return true;
    }
  };
  cldrDom.listenFor(change, "click", changeFn);
  cldrDom.listenFor(cancel, "click", cancelFn);
  cldrDom.listenFor(input, "keypress", keypressFn);
  return input;
}

function hideAfter(whom, when) {
  if (!when) {
    when = 10000;
  }
  setTimeout(function () {
    whom.style.opacity = "0.8";
  }, when / 3);
  setTimeout(function () {
    whom.style.opacity = "0.5";
  }, when / 2);
  setTimeout(function () {
    cldrDom.setDisplayed(whom, false);
  }, when);
  return whom;
}

/*
 * Make only these functions accessible from other files:
 */
export {
  load,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getHtml,
};
