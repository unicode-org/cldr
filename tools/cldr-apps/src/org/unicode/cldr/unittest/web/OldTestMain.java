/*
 ******************************************************************************
 * Copyright (C) 2004-2011, International Business Machines Corporation and   *
 * others. All Rights Reserved.
 ********************
 * Old SurveyTool.main() test
 **/

package org.unicode.cldr.unittest.web;

public class OldTestMain {

    // public static void main(String args[]) {
    // System.out.println("Starting some test of SurveyTool locally....");
    // try{
    // SurveyMain.cldrHome=SurveyMain.getHome()+"/cldr";
    // SurveyMain.vap="testingvap";
    // SurveyMain sm=new SurveyMain();
    // System.out.println("sm created.");
    // sm.doStartup();
    // System.out.println("sm started.");
    // sm.doStartupDB();
    // System.out.println("DB started.");
    // if(SurveyMain.isBusted != null) {
    // System.err.println("Survey Tool is down: " + SurveyMain.isBusted);
    // return;
    // }
    //
    // System.err.println("--- Starting processing of requests ---");
    // System.err.println("Mem: "+SurveyMain.freeMem());
    // CookieSession cs = new CookieSession(true, "0.0.0.0");
    // for ( String arg : args ) {
    // com.ibm.icu.dev.test.util.ElapsedTimer reqTimer = new
    // com.ibm.icu.dev.test.util.ElapsedTimer();
    // System.err.println("***********\n* "+arg);
    // if(arg.equals("-wait")) {
    // try {
    // System.err.println("*** WAITING ***");
    // System.in.read();
    // } catch(Throwable t) {}
    // continue;
    // } else if(arg.equals("-makeall")) {
    // WebContext xctx = new
    // URLWebContext("http://127.0.0.1:8080/cldr-apps/survey" + "?");
    // xctx.sm = sm;
    // xctx.session=cs;
    // for(int jj=0;jj<5;jj++) {
    // for(CLDRLocale locale : SurveyMain.getLocales()) {
    // com.ibm.icu.dev.test.util.ElapsedTimer qt = new
    // com.ibm.icu.dev.test.util.ElapsedTimer(locale.getBaseName()+"#"+Integer.toString(jj));
    // xctx.setLocale(locale);
    // DataSection.make(xctx, locale, SurveyMain.GREGO_XPATH, false);
    // System.err.println("Made: " + qt.toString() + " -- " +
    // SurveyMain.freeMem());
    // }
    // }
    // continue;
    // } else if(arg.equals("-makelots")) {
    // WebContext xctx = new
    // URLWebContext("http://127.0.0.1:8080/cldr-apps/survey" + "?");
    // xctx.sm = sm;
    // xctx.session=cs;
    // CLDRLocale locale = CLDRLocale.getInstance("az_Arab");
    // xctx.setLocale(locale);
    // for(int jj=0;jj<50000;jj++) {
    // // for(CLDRLocale locale : sm.getLocales()) {
    // com.ibm.icu.dev.test.util.ElapsedTimer qt = new
    // com.ibm.icu.dev.test.util.ElapsedTimer(locale.getBaseName()+"#"+Integer.toString(jj));
    // xctx.setLocale(locale);
    // DataSection ds = DataSection.make(xctx, locale, SurveyMain.GREGO_XPATH,
    // false);
    // DataSection.DisplaySet set =
    // ds.createDisplaySet(SortMode.getInstance(SurveyMain.PREF_SORTMODE_CODE_CALENDAR),
    // null);
    // System.err.println("Made: " + qt.toString() + " -- " +
    // SurveyMain.freeMem());
    // // }
    // }
    // continue;
    // } else if(arg.equals("-displots")) {
    // WebContext xctx = new
    // URLWebContext("http://127.0.0.1:8080/cldr-apps/survey" + "?");
    // xctx.sm = sm;
    // xctx.session=cs;
    // CLDRLocale locale = CLDRLocale.getInstance("az_Arab");
    // xctx.setLocale(locale);
    // DataSection ds = DataSection.make(xctx, locale, SurveyMain.GREGO_XPATH,
    // false);
    // long startTime = System.currentTimeMillis();
    // com.ibm.icu.dev.test.util.ElapsedTimer qt = new
    // com.ibm.icu.dev.test.util.ElapsedTimer(locale.getBaseName());
    // for(int jj=0;jj<10000;jj++) {
    // DataSection.DisplaySet set =
    // ds.createDisplaySet(SortMode.getInstance(SurveyMain.PREF_SORTMODE_CODE_CALENDAR),
    // null);
    // if((jj%1000)==1) {
    // long nowTime = System.currentTimeMillis();
    // long et = nowTime-startTime;
    // double dps= (((double)jj)/((double)et))*1000.0;
    // System.err.println("Made: " + qt.toString() + " -- " +
    // SurveyMain.freeMem() + " - " + set.rows.length + " - #"+jj +
    // ":  "+dps+"/sec");
    // }
    // }
    // continue;
    // } else if (arg.equals("-regextst")) {
    // long startTime = System.currentTimeMillis();
    // for(int jj=0;jj<5000000;jj++) {
    // SurveyMain.GREGO_XPATH.matches("calendar-.*\\|pattern\\|date-.*");
    // if((jj%1000000)==1) {
    // long nowTime = System.currentTimeMillis();
    // long et = nowTime-startTime;
    // double dps= (((double)jj)/((double)et))*1000.0;
    // System.err.println("ONE: - - #"+jj + ":  "+dps+"/sec");
    // }
    // }
    // startTime = System.currentTimeMillis();
    // Pattern pat = Pattern.compile("calendar-.*\\|pattern\\|date-.*");
    // for(int jj=0;jj<5000000;jj++) {
    // pat.matcher(SurveyMain.GREGO_XPATH).matches();
    // if((jj%1000000)==1) {
    // long nowTime = System.currentTimeMillis();
    // long et = nowTime-startTime;
    // double dps= (((double)jj)/((double)et))*1000.0;
    // System.err.println("TWO: - - #"+jj + ":  "+dps+"/sec");
    // }
    // }
    // continue;
    // }
    // System.err.println("Mem: "+SurveyMain.freeMem());
    // WebContext xctx = new
    // URLWebContext("http://127.0.0.1:8080/cldr-apps/survey" + arg);
    // xctx.sm = sm;
    // xctx.session=cs;
    //
    // xctx.reqTimer = reqTimer;
    //
    // if(xctx.field("dump").equals(SurveyMain.vap)) {
    // sm.doAdminPanel(xctx);
    // } else if(xctx.field("sql").equals(SurveyMain.vap)) {
    // sm.doSql(xctx);
    // } else {
    // sm.doSession(xctx); // Session-based Survey main
    // }
    // //xctx.close();
    // System.err.println("\n\n"+reqTimer+" for " + arg);
    // }
    // System.err.println("--- Ending processing of requests ---");
    //
    // /*
    // String ourXpath = "//ldml/numbers";
    //
    // System.out.println("xpath xpt.getByXpath("+ourXpath+") = " +
    // sm.xpt.getByXpath(ourXpath));
    // */
    // /*
    //
    // if(arg.length>0) {
    // WebContext xctx = new WebContext(false);
    // xctx.sm = sm;
    // xctx.session=new CookieSession(true);
    // for(int i=0;i<arg.length;i++) {
    // System.out.println("loading stuff for " + arg[i]);
    // xctx.setLocale(new ULocale(arg[i]));
    //
    // WebContext ctx = xctx;
    // System.out.println("  - loading CLDRFile and stuff");
    // UserLocaleStuff uf = sm.getUserFile(...
    // CLDRFile cf = sm.getUserFile(ctx,
    // (ctx.session.user==null)?null:ctx.session.user, ctx.getLocale());
    // if(cf == null) {
    // throw new InternalError("CLDRFile is null!");
    // }
    // CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(USER_FILE +
    // CLDRDBSRC); // TODO: remove. debuggin'
    // if(ourSrc == null) {
    // throw new InternalError("oursrc is null! - " + (USER_FILE + CLDRDBSRC) +
    // " @ " + ctx.getLocale() );
    // }
    // CheckCLDR checkCldr = (CheckCLDR)ctx.getByLocale(USER_FILE +
    // CHECKCLDR+":"+ctx.defaultPtype());
    // if (checkCldr == null) {
    // List checkCldrResult = new ArrayList();
    // System.err.println("Initting tests . . .");
    // long t0 = System.currentTimeMillis();
    // */
    // // checkCldr = CheckCLDR.getCheckAll(/* "(?!.*Collision.*).*" */ ".*");
    // /*
    // checkCldr.setDisplayInformation(sm.getBaselineFile());
    // if(cf==null) {
    // throw new InternalError("cf was null.");
    // }
    // checkCldr.setCldrFileToCheck(cf, ctx.getOptionsMap(basicOptionsMap()),
    // checkCldrResult);
    // System.err.println("fileToCheck set . . . on "+ checkCldr.toString());
    // ctx.putByLocale(USER_FILE + CHECKCLDR+":"+ctx.defaultPtype(), checkCldr);
    // {
    // // sanity check: can we get it back out
    // CheckCLDR subCheckCldr = (CheckCLDR)ctx.getByLocale(SurveyMain.USER_FILE
    // + SurveyMain.CHECKCLDR+":"+ctx.defaultPtype());
    // if(subCheckCldr == null) {
    // throw new InternalError("subCheckCldr == null");
    // }
    // }
    // if(!checkCldrResult.isEmpty()) {
    // ctx.putByLocale(USER_FILE + CHECKCLDR_RES+":"+ctx.defaultPtype(),
    // checkCldrResult); // don't bother if empty . . .
    // }
    // long t2 = System.currentTimeMillis();
    // System.err.println("Time to init tests " + arg[i]+": " + (t2-t0));
    // }
    // System.err.println("getPod:");
    // xctx.getPod("//ldml/numbers");
    // */
    //
    // /*
    // System.out.println("loading dbsource for " + arg[i]);
    // CLDRDBSource dbSource = CLDRDBSource.createInstance(sm.fileBase, sm.xpt,
    // new ULocale(arg[i]),
    // sm.getDBConnection(), null);
    // System.out.println("dbSource created for " + arg[i]);
    // CLDRFile my = new CLDRFile(dbSource,false);
    // System.out.println("file created ");
    // CheckCLDR check = CheckCLDR.getCheckAll("(?!.*Collision.*).*");
    // System.out.println("check created");
    // List result = new ArrayList();
    // Map options = null;
    // check.setCldrFileToCheck(my, options, result);
    // System.out.println("file set .. done with " + arg[i]);
    // */
    // /*
    // }
    // } else {
    // System.out.println("No locales listed");
    // }
    // */
    //
    // System.out.println("done...");
    // sm.doShutdownDB();
    // System.out.println("DB shutdown.");
    // } catch(Throwable t) {
    // System.out.println("Something bad happened.");
    // System.out.println(t.toString());
    // t.printStackTrace();
    // }
    // }

}
