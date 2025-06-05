import { expect, assert } from "chai";

import * as cldrLoad from "../src/esm/cldrLoad.mjs";
import * as cldrLocaleMap from "../src/esm/cldrLocaleMap.mjs";
import * as cldrStatus from "../src/esm/cldrStatus.mjs";
import * as cldrSurvey from "../src/esm/cldrSurvey.mjs";

import * as chai from "chai";

export const TestCldrSurvey = "ok";

/** local function for resetting things before test calls */
function setCldrLoad() {
  const flm = {
    locales: {
      ar: {
        bcp47: "ar",
        parent: "root",
        sub: [
          "ar_DZ",
          "ar_BH",
          "ar_TD",
          "ar_KM",
          "ar_DJ",
          "ar_EG",
          "ar_ER",
          "ar_IQ",
          "ar_IL",
          "ar_JO",
          "ar_KW",
          "ar_LB",
          "ar_LY",
          "ar_MR",
          "ar_MA",
          "ar_OM",
          "ar_PS",
          "ar_QA",
          "ar_SA",
          "ar_SO",
          "ar_SS",
          "ar_SD",
          "ar_SY",
          "ar_TN",
          "ar_AE",
          "ar_EH",
          "ar_YE",
          "ar_001",
        ],
        dcChild: "ar_001",
        highestParent: "ar",
        name: "Arabic",
        dir: "rtl",
        type: "common",
        extended: false,
        tc: true,
      },
      bn: {
        bcp47: "bn",
        parent: "root",
        sub: ["bn_BD", "bn_IN"],
        dcChild: "bn_BD",
        highestParent: "bn",
        name: "Bangla",
        type: "common",
        dir: "ltr", // we now explicitly set this, even for ltr
        extended: false,
        tc: true,
      },
    },
  };

  cldrLoad.setTheLocaleMap(new cldrLocaleMap.LocaleMap(flm));
}

function unsetCldrLoad() {
  cldrLoad.setTheLocaleMap(new cldrLocaleMap.LocaleMap(null));
  cldrStatus.setCurrentLocale(null);
}

describe("cldrSurvey.setLang", function () {
  before(() => setCldrLoad());
  after(() => unsetCldrLoad());
  it("should be able to set a node explicitly", () => {
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n, "bn", "rtl");
    expect(n.lang).to.equal("bn");
    expect(n.dir).to.equal("rtl");
  });
  it("should be able to set another node explicitly", () => {
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n, "bn", "ltr");
    expect(n.lang).to.equal("bn");
    expect(n.dir).to.equal("ltr");
  });
  it("should be able to set a node with ar language", () => {
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n, "ar");
    expect(n.lang).to.equal("ar");
    expect(n.dir).to.equal("rtl");
  });
  it("should be able to set a node with bn language", () => {
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n, "bn");
    expect(n.lang).to.equal("bn");
    expect(n.dir).to.equal("ltr");
  });
  it("should be able to set a node with default ar language", () => {
    cldrStatus.setCurrentLocale("ar");
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n);
    expect(n.lang).to.equal("ar");
    expect(n.dir).to.equal("rtl");
  });
  it("should be able to set a node with default bn language", () => {
    cldrStatus.setCurrentLocale("bn");
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n);
    expect(n.lang).to.equal("bn");
    expect(n.dir).to.equal("ltr");
  });
  it("should be able to set a node with override ar language", () => {
    cldrStatus.setCurrentLocale("ar");
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n, undefined, "ltr");
    expect(n.lang).to.equal("ar");
    expect(n.dir).to.equal("ltr");
  });
  it("should be able to set a node with override bn language", () => {
    cldrStatus.setCurrentLocale("bn");
    const n = document.createElement("span");
    expect(n).to.be.ok;
    cldrSurvey.setLang(n, undefined, "rtl");
    expect(n.lang).to.equal("bn");
    expect(n.dir).to.equal("rtl");
  });
  it("Should fail on a bad override directionality", () => {
    cldrStatus.setCurrentLocale("bn");
    const n = document.createElement("span");
    expect(n).to.be.ok;
    assert.throws(() => cldrSurvey.setLang(n, undefined, "RIGHT_TO_LEFT"));
  });
});
