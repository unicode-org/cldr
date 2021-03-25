/*
 * cldrMail: encapsulate functions for email for Survey Tool
 */
import * as cldrDom from "./cldrDom.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

// called as special.load
function load() {
  const url =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=mail&s=" +
    cldrStatus.getSessionId() +
    "&fetchAll=true&" +
    cldrSurvey.cacheKill();

  const message = "(loading mail " + cldrStatus.getCurrentLocale() + ")";
  cldrLoad.myLoad(url, message, loadMail);
}

function loadMail(json) {
  // TODO: shorten this function, avoid long deeply nested inner functions
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  if (!cldrLoad.verifyJson(json, "mail")) {
    return;
  }
  if (json.dataLoadTime) {
    cldrDom.updateIf("dynload", json.dataLoadTime);
  }
  const theDiv = cldrLoad.flipToEmptyOther();
  cldrDom.removeAllChildNodes(theDiv);

  const listDiv = cldrDom.createChunk("", "div", "mailListChunk");
  const contentDiv = cldrDom.createChunk("", "div", "mailContentChunk");

  theDiv.appendChild(listDiv);
  theDiv.appendChild(contentDiv);

  cldrDom.setDisplayed(contentDiv, false);
  const header = json.mail.header;
  const data = json.mail.data;

  if (data.length == 0) {
    listDiv.appendChild(
      cldrDom.createChunk(cldrText.get("mail_noMail"), "p", "helpContent")
    );
  } else {
    for (let ii in data) {
      const row = data[ii];
      const li = cldrDom.createChunk(
        row[header.QUEUE_DATE] + ": " + row[header.SUBJECT],
        "li",
        "mailRow"
      );
      if (row[header.READ_DATE]) {
        cldrDom.addClass(li, "readMail");
      }
      if (header.USER !== undefined) {
        li.appendChild(
          document.createTextNode("(to " + row[header.USER] + ")")
        );
      }
      if (row[header.SENT_DATE] !== false) {
        li.appendChild(cldrDom.createChunk("(sent)", "span", "winner"));
      } else if (row[header.TRY_COUNT] >= 3) {
        li.appendChild(
          cldrDom.createChunk(
            "(try#" + row[header.TRY_COUNT] + ")",
            "span",
            "loser"
          )
        );
      } else if (row[header.TRY_COUNT] > 0) {
        li.appendChild(
          cldrDom.createChunk(
            "(try#" + row[header.TRY_COUNT] + ")",
            "span",
            "warning"
          )
        );
      }
      listDiv.appendChild(li);

      li.onclick = (function (li, row, header) {
        return function () {
          if (!row[header.READ_DATE]) {
            cldrLoad.myLoad(
              cldrStatus.getContextPath() +
                "/SurveyAjax?what=mail&s=" +
                cldrStatus.getSessionId() +
                "&markRead=" +
                row[header.ID] +
                "&" +
                cldrSurvey.cacheKill(),
              "Marking mail read",
              function (json) {
                if (!cldrLoad.verifyJson(json, "mail")) {
                  return;
                } else {
                  cldrDom.addClass(li, "readMail"); // mark as read when server answers
                  row[header.READ_DATE] = true; // close enough
                }
              }
            );
          }
          cldrDom.setDisplayed(contentDiv, false);

          cldrDom.removeAllChildNodes(contentDiv);

          contentDiv.appendChild(
            cldrDom.createChunk(
              "Date: " + row[header.QUEUE_DATE],
              "h2",
              "mailHeader"
            )
          );
          contentDiv.appendChild(
            cldrDom.createChunk(
              "Subject: " + row[header.SUBJECT],
              "h2",
              "mailHeader"
            )
          );
          contentDiv.appendChild(
            cldrDom.createChunk(
              "Message-ID: " + row[header.ID],
              "h2",
              "mailHeader"
            )
          );
          if (header.USER !== undefined) {
            contentDiv.appendChild(
              cldrDom.createChunk("To: " + row[header.USER], "h2", "mailHeader")
            );
          }
          contentDiv.appendChild(
            cldrDom.createChunk(row[header.TEXT], "p", "mailContent")
          );

          cldrDom.setDisplayed(contentDiv, true);
        };
      })(li, row, header);
    }
  }
}

export { load };
