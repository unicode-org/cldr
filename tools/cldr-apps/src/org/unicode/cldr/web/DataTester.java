package org.unicode.cldr.web;

public class DataTester /* extends Registerable */ {
    //
    // //////
    //
    // /**
    // *
    // */
    // private final Vetting vetting;
    // CLDRFile file;
    // CheckCLDR check;
    // List overallResults = new ArrayList();
    // List individualResults = new ArrayList();
    // Map options = this.vetting.sm.basicOptionsMap();
    //
    // void reset() {
    // XMLSource dbSource = this.vetting.sm.makeDBSource( locale);
    // reset(dbSource);
    // }
    //
    // void reset(XMLSource dbSource) {
    // file =
    // this.vetting.sm.makeCLDRFile(dbSource).setSupplementalDirectory(this.vetting.sm.supplementalDataDir);
    // overallResults.clear();
    // check = this.vetting.sm.createCheckWithoutCollisions();
    // check.setCldrFileToCheck(file, options, overallResults);
    // setValid();
    // register();
    // }
    //
    // DataTester(Vetting vetting, CLDRLocale locale)
    // {
    // super(this.vetting.sm.lcr, locale);
    // this.vetting = vetting;
    //
    // options.put("CheckCoverage.requiredLevel","minimal");
    // options.put("CoverageLevel.localeType","");
    //
    // reset();
    // }
    // DataTester(Vetting vetting, CLDRLocale locale, XMLSource src)
    // {
    // super(this.vetting.sm.lcr, locale);
    // this.vetting = vetting;
    //
    // options.put("CheckCoverage.requiredLevel","minimal");
    // options.put("CoverageLevel.localeType","");
    //
    // reset(src);
    // }
    // //String f2 = sm.xpt.getById(85048);
    // boolean test(String xpath, String fxpath, String value) {
    // individualResults.clear();
    // check.check(xpath, fxpath, value, options, individualResults); // they
    // get the full course
    // if(!individualResults.isEmpty()) {
    // for(Object o : individualResults) {
    // CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus)o;
    // if(status.getType().equals(status.errorType)) {
    // //if(locale.equals("fr")) {
    // // if(/*sm.isUnofficial &&*/ xpath.indexOf("ii")!=-1) {
    // // if(f2.equals(xpath)) {
    // // System.err.println("ER: "+xpath + " // " + fxpath + " // " + value +
    // " - " + status.toString());
    // // }
    // return true;
    // }
    // }
    // }
    // return false;
    // }
    //
}