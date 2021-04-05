import * as cldrTest from "./TestCldrTest.js";

import * as cldrAccount from "../src/esm/cldrAccount.js";

export const TestCldrAccount = "ok";

const assert = chai.assert;

const json = {
  visitors: "",
  isBusted: "0",
  what: "user_list",
  err: "",
  org: "SurveyTool",
  preset_do: "",
  SurveyOK: "1",
  preset_fromint: -1,
  userPerms: {
    canCreateUsers: true,
    levels: {
      0: {
        string: "0: (ADMIN)",
        isManagerFor: true,
        name: "admin",
        canCreateOrSetLevelTo: true,
      },
      1: {
        string: "1: (TC)",
        isManagerFor: true,
        name: "tc",
        canCreateOrSetLevelTo: true,
      },
      2: {
        string: "2: (MANAGER)",
        isManagerFor: true,
        name: "manager",
        canCreateOrSetLevelTo: true,
      },
      3: {
        string: "3: (EXPERT)",
        isManagerFor: true,
        name: "expert",
        canCreateOrSetLevelTo: true,
      },
      5: {
        string: "5: (VETTER)",
        isManagerFor: true,
        name: "vetter",
        canCreateOrSetLevelTo: true,
      },
      8: {
        string: "8: (ANONYMOUS)",
        isManagerFor: true,
        name: "anonymous",
        canCreateOrSetLevelTo: true,
      },
      999: {
        string: "999: (LOCKED)",
        isManagerFor: true,
        name: "locked",
        canCreateOrSetLevelTo: true,
      },
      10: {
        string: "10: (STREET)",
        isManagerFor: true,
        name: "street",
        canCreateOrSetLevelTo: true,
      },
    },
    canModifyUsers: true,
  },
  shownUsers: [
    {
      voteCountMenu: [4, 100],
      votecount: 100,
      org: "SurveyTool",
      lastlogin: "2021-02-04 17:12:04.0",
      userCanDeleteUser: false,
      userlevelName: "admin",
      active: "0 sec.",
      seen: "19 sec.",
      locales: "*",
      userlevel: 0,
      emailHash: "f8450a97cc7e38e6d109425c87b41634",
      name: "admin",
      havePermToChange: true,
      id: 1,
      actions: {},
      email: "admin@",
    },
  ],
  uptime: "",
  isSetup: "1",
};

describe("cldrAccount.getTable", function () {
  const html = cldrAccount.getTable(json);

  it("should not return null or empty", function () {
    assert(html != null && html !== "", "html is neither null nor empty");
  });

  const xml = "<div>" + html + "</div>";
  const xmlStr = cldrTest.parseAsMimeType(xml, "application/xml");
  it("should return valid xml when in div element", function () {
    assert(xmlStr || false, "parses OK as xml when in div element");
  });

  const htmlStr = cldrTest.parseAsMimeType(html, "text/html");
  it("should return good html", function () {
    assert(htmlStr || false, "parses OK as html");
  });

  it("should contain angle brackets", function () {
    assert(
      htmlStr.indexOf("<") !== -1 && htmlStr.indexOf(">") !== -1,
      "does contain angle brackets"
    );
  });
});

describe("cldrAccount.getHtml", function () {
  const html = cldrAccount.getHtml(json);

  it("should not return null or empty", function () {
    assert(html != null && html !== "", "html is neither null nor empty");
  });

  const xml = "<div>" + html + "</div>";
  const xmlStr = cldrTest.parseAsMimeType(xml, "application/xml");
  it("should return valid xml when in div element", function () {
    assert(xmlStr || false, "parses OK as xml when in div element");
  });

  const htmlStr = cldrTest.parseAsMimeType(html, "text/html");
  it("should return good html", function () {
    assert(htmlStr || false, "parses OK as html");
  });

  it("should contain angle brackets", function () {
    assert(
      htmlStr.indexOf("<") !== -1 && htmlStr.indexOf(">") !== -1,
      "does contain angle brackets"
    );
  });
});
