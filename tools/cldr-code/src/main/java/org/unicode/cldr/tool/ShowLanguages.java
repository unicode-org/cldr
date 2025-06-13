/*
 ******************************************************************************
 * Copyright (C) 2004-2011, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.NameType;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.CodeType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.XPathParts;

@CLDRTool(alias = "showlanguages", description = "Generate Language info charts")
public class ShowLanguages {
    private static final boolean SHOW_NATIVE = true;
    static final boolean SHOW_SKIPPED = false;
    static int skipped = 0;

    static Comparator col =
            new org.unicode.cldr.util.MultiComparator(
                    Collator.getInstance(new ULocale("en")),
                    new UTF16.StringComparator(true, false, 0));

    static StandardCodes sc = StandardCodes.make();

    static Factory cldrFactory =
            CLDRConfig.getInstance().getCldrFactory(); // .make(CLDRPaths.MAIN_DIRECTORY, ".*");
    static CLDRFile english = CLDRConfig.getInstance().getEnglish();
    static NameGetter englishNameGetter = english.nameGetter();

    public static void main(String[] args) throws IOException {
        System.out.println("Writing into " + FormattedFileWriter.CHART_TARGET_DIR);
        FileCopier.ensureDirectoryExists(FormattedFileWriter.CHART_TARGET_DIR);
        FileCopier.copy(ShowLanguages.class, "index.css", FormattedFileWriter.CHART_TARGET_DIR);
        FormattedFileWriter.copyIncludeHtmls(FormattedFileWriter.CHART_TARGET_DIR);

        StringWriter sw = printLanguageData(cldrFactory, "index.html");
        writeSupplementalIndex("index.html", sw);

        // cldrFactory = Factory.make(Utility.COMMON_DIRECTORY + "../dropbox/extra2/", ".*");
        // printLanguageData(cldrFactory, "language_info2.txt");
        System.out.println("Done - wrote into " + FormattedFileWriter.CHART_TARGET_DIR);
        if (skipped > 0) {
            System.err.println(
                    "*** WARNING ***\nTODO CLDR-1129: "
                            + skipped
                            + " skipped xpath(s) - set SHOW_SKIPPED=true to debug.");
        }
    }

    /** */
    public static FormattedFileWriter.Anchors SUPPLEMENTAL_INDEX_ANCHORS =
            new FormattedFileWriter.Anchors();

    static SupplementalDataInfo supplementalDataInfo =
            SupplementalDataInfo.getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

    private static StringWriter printLanguageData(Factory cldrFactory, String filename)
            throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        LanguageInfo linfo = new LanguageInfo(cldrFactory);
        linfo.showCoverageGoals(pw);

        new ChartDtdDelta().writeChart(SUPPLEMENTAL_INDEX_ANCHORS);
        ShowLocaleCoverage.showCoverage(SUPPLEMENTAL_INDEX_ANCHORS, null);

        new ChartDayPeriods().writeChart(SUPPLEMENTAL_INDEX_ANCHORS);
        new ChartLanguageMatching().writeChart(SUPPLEMENTAL_INDEX_ANCHORS);
        new ChartLanguageGroups().writeChart(SUPPLEMENTAL_INDEX_ANCHORS);
        new ChartSubdivisions().writeChart(SUPPLEMENTAL_INDEX_ANCHORS);
        if (ToolConstants.CHART_VERSION.compareTo("37") >= 0) {
            new ChartUnitConversions().writeChart(SUPPLEMENTAL_INDEX_ANCHORS);
            new ChartUnitPreferences().writeChart(SUPPLEMENTAL_INDEX_ANCHORS);
        }
        // since we don't want these listed on the supplemental page, use null

        new ShowPlurals().printPlurals(english, null, pw, cldrFactory);

        linfo.printLikelySubtags(pw);

        linfo.showCountryLanguageInfo(pw);

        linfo.showLanguageCountryInfo(pw);

        //      linfo.showTerritoryInfo();
        //      linfo.printCountryData(pw);

        // linfo.printDeprecatedItems(pw);

        // PrintWriter pw1 = new PrintWriter(new FormattedFileWriter(pw, "Languages and
        // Territories", null));
        // pw1.println("<tr><th>Language \u2192 Territories");
        // pw1.println("</th><th>Territory \u2192 Language");
        // pw1.println("</th><th>Territories Not Represented");
        // pw1.println("</th><th>Languages Not Represented");
        // pw1.println("</th></tr>");
        //
        // pw1.println("<tr><td>");
        // linfo.print(pw1, CLDRFile.LANGUAGE_NAME, CLDRFile.TERRITORY_NAME);
        // pw1.println("</td><td>");
        // linfo.print(pw1, CLDRFile.TERRITORY_NAME, CLDRFile.LANGUAGE_NAME);
        // pw1.println("</td><td>");
        // linfo.printMissing(pw1, CLDRFile.TERRITORY_NAME, CLDRFile.TERRITORY_NAME);
        // pw1.println("</td><td>");
        // linfo.printMissing(pw1, CLDRFile.LANGUAGE_NAME, CLDRFile.TERRITORY_NAME);
        // pw1.println("</td></tr>");
        //
        // pw1.close();

        printLanguageScript(linfo, pw);
        printScriptLanguageTerritory(linfo, pw);

        linfo.showCorrespondances();

        // linfo.showCalendarData(pw);

        linfo.showCountryInfo(pw);
        linfo.printCurrency(pw);
        linfo.printContains(pw);

        linfo.printWindows_Tzid(pw);
        linfo.printAliases(pw);

        linfo.printCharacters(pw);

        pw.close();

        return sw;
    }

    private static void writeSupplementalIndex(String filename, StringWriter sw)
            throws IOException {
        String[] replacements = {
            "%date%", CldrUtility.isoFormatDateOnly(new Date()),
            "%contents%", SUPPLEMENTAL_INDEX_ANCHORS.toString(),
            "%data%", sw.toString(),
            "%index%", "../index.html"
        };
        PrintWriter pw2 =
                org.unicode.cldr.draft.FileUtilities.openUTF8Writer(
                        FormattedFileWriter.CHART_TARGET_DIR, filename);
        FileUtilities.appendFile(ShowLanguages.class, "supplemental.html", replacements, pw2);
        pw2.close();
    }

    private static void printLanguageScript(LanguageInfo linfo, PrintWriter pw) throws IOException {
        PrintWriter pw1;
        TablePrinter tablePrinter =
                new TablePrinter()
                        .addColumn("Language", "class='source'", null, "class='source'", true)
                        .setSpanRows(true)
                        .setSortPriority(0)
                        .setBreakSpans(true)
                        .addColumn(
                                "Code",
                                "class='source'",
                                CldrUtility.getDoubleLinkMsg(),
                                "class='source'",
                                true)
                        .setSpanRows(true)
                        .addColumn(
                                "ML",
                                "class='target' title='modern language'",
                                null,
                                "class='target'",
                                true)
                        .setSpanRows(true)
                        .setSortPriority(1)
                        .addColumn(
                                "P", "class='target' title='primary'", null, "class='target'", true)
                        .setSortPriority(3)
                        .addColumn("Script", "class='target'", null, "class='target'", true)
                        .setSortPriority(3)
                        .addColumn("Code", "class='target'", null, "class='target'", true)
                        .addColumn(
                                "MS",
                                "class='target' title='modern script'",
                                null,
                                "class='target'",
                                true)
                        .setSortPriority(2);

        TablePrinter tablePrinter2 =
                new TablePrinter()
                        .addColumn("Script", "class='source'", null, "class='source'", true)
                        .setSpanRows(true)
                        .setSortPriority(0)
                        .setBreakSpans(true)
                        .addColumn(
                                "Code",
                                "class='source'",
                                CldrUtility.getDoubleLinkMsg(),
                                "class='source'",
                                true)
                        .setSpanRows(true)
                        .addColumn(
                                "MS",
                                "class='target' title='modern script'",
                                null,
                                "class='target'",
                                true)
                        .setSpanRows(true)
                        .setSortPriority(1)
                        .addColumn("Language", "class='target'", null, "class='target'", true)
                        .setSortPriority(3)
                        .addColumn("Code", "class='target'", null, "class='target'", true)
                        .addColumn(
                                "ML",
                                "class='target' title='modern language'",
                                null,
                                "class='target'",
                                true)
                        .setSortPriority(2)
                        .addColumn(
                                "P", "class='target' title='primary'", null, "class='target'", true)
                        .setSortPriority(3);

        // get the codes so we can show the remainder
        Set<String> remainingScripts =
                new TreeSet<>(getScriptsToShow()); // StandardCodes.MODERN_SCRIPTS);
        UnicodeSet temp = new UnicodeSet();
        for (String script : getScriptsToShow()) {
            temp.clear();
            try {
                temp.applyPropertyAlias("script", script);
            } catch (RuntimeException e) {
            } // fall through
            if (temp.size() == 0) {
                remainingScripts.remove(script);
                System.out.println("Removing: " + script);
            } else {
                System.out.println("Keeping: " + script);
            }
        }
        remainingScripts.remove("Brai");
        remainingScripts.remove("Hira");
        remainingScripts.remove("Qaai");
        remainingScripts.remove("Hrkt");
        remainingScripts.remove("Zzzz");
        remainingScripts.remove("Zyyy");

        Set<String> remainingLanguages = new TreeSet<>(getLanguagesToShow());
        for (String language : getLanguagesToShow()) {
            Scope s = Iso639Data.getScope(language);
            Type t = Iso639Data.getType(language);
            if (s != Scope.Individual && s != Scope.Macrolanguage || t != Type.Living) {
                remainingLanguages.remove(language);
            }
        }

        Set<String> languages = supplementalDataInfo.getBasicLanguageDataLanguages();
        for (String language : languages) {
            Set<BasicLanguageData> basicLanguageData =
                    supplementalDataInfo.getBasicLanguageData(language);
            for (BasicLanguageData basicData : basicLanguageData) {
                String secondary =
                        isOfficial(language) // basicData.getType() ==
                                // BasicLanguageData.Type.primary
                                ? "\u00A0"
                                : "N";
                for (String script : basicData.getScripts()) {
                    addLanguageScriptCells(
                            tablePrinter, tablePrinter2, language, script, secondary);
                    remainingScripts.remove(script);
                    remainingLanguages.remove(language);
                }
            }
        }
        for (String language : remainingLanguages) {
            addLanguageScriptCells(tablePrinter, tablePrinter2, language, "Zzzz", "?");
        }
        for (String script : remainingScripts) {
            addLanguageScriptCells(tablePrinter, tablePrinter2, "und", script, "?");
        }

        pw1 =
                new PrintWriter(
                        new FormattedFileWriter(
                                null, "Languages and Scripts", null, SUPPLEMENTAL_INDEX_ANCHORS));
        pw1.println(tablePrinter.toTable());
        pw1.close();

        pw1 =
                new PrintWriter(
                        new FormattedFileWriter(
                                null, "Scripts and Languages", null, SUPPLEMENTAL_INDEX_ANCHORS));
        pw1.println(tablePrinter2.toTable());
        pw1.close();
    }

    static final Map<String, OfficialStatus> languageToBestStatus = new HashMap<>();

    static {
        for (String language : supplementalDataInfo.getLanguagesForTerritoriesPopulationData()) {
            Set<String> territories =
                    supplementalDataInfo.getTerritoriesForPopulationData(language);
            if (territories == null) {
                continue;
            }
            int underbar = language.indexOf('_');
            String base = underbar < 0 ? null : language.substring(0, underbar);

            for (String territory : territories) {
                PopulationData data =
                        supplementalDataInfo.getLanguageAndTerritoryPopulationData(
                                language, territory);
                OfficialStatus status = data.getOfficialStatus();
                OfficialStatus old;
                old = languageToBestStatus.get(language);
                if (old == null || status.compareTo(old) > 0) {
                    languageToBestStatus.put(language, status);
                }
                if (base != null) {
                    old = languageToBestStatus.get(base);
                    if (old == null || status.compareTo(old) > 0) {
                        languageToBestStatus.put(base, status);
                    }
                }
            }
        }
    }

    private static boolean isOfficial(String language) {
        OfficialStatus status = languageToBestStatus.get(language);
        if (status != null && status.isMajor()) {
            return true;
        }
        int underbar = language.indexOf('_');
        if (underbar < 0) {
            return false;
        }
        return isOfficial(language.substring(0, underbar));
    }

    private static Set<String> getLanguagesToShow() {
        return getEnglishTypes("language", NameType.LANGUAGE);
    }

    private static Set<String> getEnglishTypes(String type, NameType nameType) {
        Set<String> result = new HashSet<>(sc.getSurveyToolDisplayCodes(type));
        for (Iterator<String> it = english.getAvailableIterator(nameType); it.hasNext(); ) {
            XPathParts parts = XPathParts.getFrozenInstance(it.next());
            String newType = parts.getAttributeValue(-1, "type");
            if (!result.contains(newType)) {
                result.add(newType);
            }
        }
        return result;
    }

    private static Set<String> getScriptsToShow() {
        return getEnglishTypes("script", NameType.SCRIPT);
    }

    private static void printScriptLanguageTerritory(LanguageInfo linfo, PrintWriter pw)
            throws IOException {
        PrintWriter pw1;
        TablePrinter tablePrinter2 =
                new TablePrinter()
                        .addColumn(
                                "Sample Char",
                                "class='source'",
                                null,
                                "class='source sample'",
                                true)
                        .setSpanRows(true)
                        .addColumn("Script", "class='source'", null, "class='source'", true)
                        .setSpanRows(true)
                        .setSortPriority(0)
                        .setBreakSpans(true)
                        .addColumn(
                                "Code",
                                "class='source'",
                                CldrUtility.getDoubleLinkMsg(),
                                "class='source'",
                                true)
                        .setSpanRows(true)
                        .addColumn("T", "class='target'", null, "class='target'", true)
                        .setSortPriority(1)
                        .addColumn("Language", "class='target'", null, "class='target'", true)
                        .setSortPriority(2)
                        .addColumn("Native", "class='target'", null, "class='target'", true)
                        .addColumn("Code", "class='target'", null, "class='target'", true)
                        .addColumn("T", "class='target'", null, "class='target'", true)
                        .setSortPriority(3)
                        .addColumn("Territory", "class='target'", null, "class='target'", true)
                        .setSortPriority(4)
                        .addColumn("Native", "class='target'", null, "class='target'", true)
                        .addColumn("Code", "class='target'", null, "class='target'", true);

        // get the codes so we can show the remainder
        Set<String> remainingScripts = new TreeSet<>(getScriptsToShow());
        Set<String> remainingTerritories = new TreeSet<>(sc.getGoodAvailableCodes("territory"));
        UnicodeSet temp = new UnicodeSet();
        for (String script : getScriptsToShow()) {
            temp.clear();
            try {
                temp.applyPropertyAlias("script", script);
            } catch (RuntimeException e) {
            } // fall through
            if (temp.size() == 0) {
                remainingScripts.remove(script);
                System.out.println("Removing: " + script);
            } else {
                System.out.println("Keeping: " + script);
            }
        }
        remainingScripts.remove("Brai");
        remainingScripts.remove("Hira");
        remainingScripts.remove("Qaai");
        remainingScripts.remove("Hrkt");
        remainingScripts.remove("Zzzz");
        remainingScripts.remove("Zyyy");

        Set<String> remainingLanguages = new TreeSet<>(getLanguagesToShow());
        for (String language : getLanguagesToShow()) {
            Scope s = Iso639Data.getScope(language);
            Type t = Iso639Data.getType(language);
            if (s != Scope.Individual && s != Scope.Macrolanguage || t != Type.Living) {
                remainingLanguages.remove(language);
            }
        }

        Set<String> languages = supplementalDataInfo.getBasicLanguageDataLanguages();
        for (String language : languages) {
            Set<BasicLanguageData> basicLanguageData =
                    supplementalDataInfo.getBasicLanguageData(language);
            for (BasicLanguageData basicData : basicLanguageData) {
                if (basicData.getType() != BasicLanguageData.Type.primary) {
                    continue;
                }
                Set<String> mainTerritories = getTerritories(language);
                if (mainTerritories.size() == 0) {
                    continue;
                    // mainTerritories.add("ZZ");
                }

                TreeSet<String> mainScripts = new TreeSet<>(basicData.getScripts());
                if (mainScripts.size() == 0) {
                    continue;
                }
                for (String script : mainScripts) {
                    for (String territory : mainTerritories) {
                        addLanguageScriptCells2(tablePrinter2, language, script, territory);
                        remainingTerritories.remove(territory);
                    }
                    remainingScripts.remove(script);
                }
            }
            remainingLanguages.remove(language);
        }
        // for (String language : remainingLanguages) {
        // addLanguageScriptCells2( tablePrinter2, language, "Zzzz", "ZZ");
        // }
        // for (String script : remainingScripts) {
        // addLanguageScriptCells2( tablePrinter2, "und", script, "ZZ");
        // }
        // for (String territory : remainingTerritories) {
        // addLanguageScriptCells2( tablePrinter2, "und", "Zzzz", territory);
        // }

        pw1 =
                new PrintWriter(
                        new FormattedFileWriter(
                                null,
                                "Scripts, Languages, and Territories",
                                null,
                                SUPPLEMENTAL_INDEX_ANCHORS));
        pw1.println(tablePrinter2.toTable());
        pw1.close();
    }

    private static Relation<String, String> territoryFix;

    private static Set<String> getTerritories(String language) {
        if (territoryFix == null) { // set up the data
            initTerritoryFix();
        }
        Set<String> territories = territoryFix.getAll(language);
        if (territories == null) {
            territories = new TreeSet<>();
        }
        return territories;
    }

    private static void initTerritoryFix() {
        territoryFix = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Set<String> languages = supplementalDataInfo.getLanguages();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String language2 : languages) {
            if (language2.contains("_")) {
                ltp.set(language2).getLanguage();
                addOfficialTerritory(ltp, language2, ltp.getLanguage());
            } else {
                addOfficialTerritory(ltp, language2, language2);
            }
        }
    }

    private static void addOfficialTerritory(
            LanguageTagParser ltp, String language, String baseLanguage) {
        // territoryFix.putAll(baseLanguage,
        // supplementalDataInfo.getTerritoriesForPopulationData(language));
        Set<String> territories = supplementalDataInfo.getTerritoriesForPopulationData(language);
        if (territories == null) {
            return;
        }
        for (String territory : territories) {
            PopulationData data =
                    supplementalDataInfo.getLanguageAndTerritoryPopulationData(language, territory);
            OfficialStatus status = data.getOfficialStatus();
            if (status.isMajor()) {
                territoryFix.put(baseLanguage, territory);
                System.out.println(
                        "\tAdding\t" + baseLanguage + "\t" + territory + "\t" + language);
            }
        }
    }

    private static void addLanguageScriptCells2(
            TablePrinter tablePrinter2, String language, String script, String territory) {
        CLDRFile nativeLanguage = null;
        if (SHOW_NATIVE) {
            try {
                nativeLanguage = cldrFactory.make(language + "_" + script + "_" + territory, true);
            } catch (RuntimeException e) {
                try {
                    nativeLanguage = cldrFactory.make(language + "_" + script, true);
                } catch (RuntimeException e2) {
                    try {
                        nativeLanguage = cldrFactory.make(language, true);
                    } catch (RuntimeException e3) {
                    }
                }
            }
            // check for overlap
            if (nativeLanguage != null
                    && !script.equals("Jpan")
                    && !script.equals("Hans")
                    && !script.equals("Hant")) {
                UnicodeSet scriptSet;
                try {
                    String tempScript = script.equals("Kore") ? "Hang" : script;
                    scriptSet = new UnicodeSet("[:script=" + tempScript + ":]");
                } catch (RuntimeException e) {
                    scriptSet = new UnicodeSet();
                }
                UnicodeSet exemplars = nativeLanguage.getExemplarSet("", WinningChoice.WINNING);
                if (scriptSet.containsNone(exemplars)) {
                    System.out.println(
                            "Skipping CLDR file -- exemplars differ: "
                                    + language
                                    + "\t"
                                    + nativeLanguage.getLocaleID()
                                    + "\t"
                                    + scriptSet
                                    + "\t"
                                    + exemplars);
                    nativeLanguage = null;
                }
            }
        }
        String languageName =
                englishNameGetter.getNameFromTypeEnumCode(NameType.LANGUAGE, language);
        if (languageName == null) languageName = "???";
        String isLanguageTranslated = "";
        String nativeLanguageName =
                nativeLanguage == null
                        ? null
                        : nativeLanguage
                                .nameGetter()
                                .getNameFromTypeEnumCode(NameType.LANGUAGE, language);
        if (nativeLanguageName == null || nativeLanguageName.equals(language)) {
            nativeLanguageName = "<i>n/a</i>";
            isLanguageTranslated = "n";
        }

        String scriptName = englishNameGetter.getNameFromTypeEnumCode(NameType.SCRIPT, script);
        // String nativeScriptName = nativeLanguage == null ? null :
        // nativeLanguage.getName(CLDRFile.SCRIPT_NAME,script);
        // if (nativeScriptName != null && !nativeScriptName.equals(script)) {
        // scriptName = nativeScriptName + "[" + scriptName + "]";
        // }

        String isTerritoryTranslated = "";
        String territoryName =
                englishNameGetter.getNameFromTypeEnumCode(NameType.TERRITORY, territory);
        String nativeTerritoryName =
                nativeLanguage == null
                        ? null
                        : nativeLanguage
                                .nameGetter()
                                .getNameFromTypeEnumCode(NameType.TERRITORY, territory);
        if (nativeTerritoryName == null || nativeTerritoryName.equals(territory)) {
            nativeTerritoryName = "<i>n/a</i>";
            isTerritoryTranslated = "n";
        }

        // Type t = Iso639Data.getType(language);
        // if ((s == Scope.Individual || s == Scope.Macrolanguage || s == Scope.Collection) && t ==
        // Type.Living) {
        // // ok
        // } else if (!language.equals("und")){
        // scriptModern = "N";
        // }
        // String languageModern = oldLanguage.contains(t) ? "O" : language.equals("und") ? "?" :
        // "";

        Info scriptMetatdata = ScriptMetadata.getInfo(script);
        tablePrinter2
                .addRow()
                .addCell(scriptMetatdata.sampleChar)
                .addCell(scriptName)
                .addCell(script)
                .addCell(isLanguageTranslated)
                .addCell(languageName)
                .addCell(nativeLanguageName)
                .addCell(language)
                .addCell(isTerritoryTranslated)
                .addCell(territoryName)
                .addCell(nativeTerritoryName)
                .addCell(territory)
                .finishRow();
    }

    static ImmutableMap<String, String> fixScriptGif =
            ImmutableMap.<String, String>builder()
                    .put("hangul", "hangulsyllables")
                    .put("japanese", "hiragana")
                    .put("unknown or invalid script", "unknown")
                    .put("Hant", "Hant")
                    .put("Hans", "Hans")
                    .build();

    private static String getGifName(String script) {
        String temp = fixScriptGif.get(script);
        if (temp != null) {
            return temp;
        }
        String scriptName = englishNameGetter.getNameFromTypeEnumCode(NameType.SCRIPT, script);
        scriptName = scriptName.toLowerCase(Locale.ENGLISH);
        temp = fixScriptGif.get(scriptName);
        if (temp != null) {
            return temp;
        }
        return scriptName;
    }

    private static Set<Type> oldLanguage =
            Collections.unmodifiableSet(
                    EnumSet.of(Type.Ancient, Type.Extinct, Type.Historical, Type.Constructed));

    private static void addLanguageScriptCells(
            TablePrinter tablePrinter,
            TablePrinter tablePrinter2,
            String language,
            String script,
            String secondary) {
        try {
            String languageName =
                    englishNameGetter.getNameFromTypeEnumCode(NameType.LANGUAGE, language);
            if (languageName == null) {
                languageName = "¿" + language + "?";
                System.err.println("No English Language Name for:" + language);
            }
            String scriptName = englishNameGetter.getNameFromTypeEnumCode(NameType.SCRIPT, script);
            if (scriptName == null) {
                scriptName = "¿" + script + "?";
                System.err.println("No English Language Name for:" + script);
            }
            String scriptModern =
                    StandardCodes.isScriptModern(script) ? "" : script.equals("Zzzz") ? "n/a" : "N";
            // Scope s = Iso639Data.getScope(language);
            Type t = Iso639Data.getType(language);
            // if ((s == Scope.Individual || s == Scope.Macrolanguage || s == Scope.Collection) && t
            // == Type.Living) {
            // // ok
            // } else if (!language.equals("und")){
            // scriptModern = "N";
            // }
            String languageModern =
                    oldLanguage.contains(t) ? "O" : language.equals("und") ? "?" : "";

            tablePrinter
                    .addRow()
                    .addCell(languageName)
                    .addCell(language)
                    .addCell(languageModern)
                    .addCell(secondary)
                    .addCell(scriptName)
                    .addCell(script)
                    .addCell(scriptModern)
                    .finishRow();

            tablePrinter2
                    .addRow()
                    .addCell(scriptName)
                    .addCell(script)
                    .addCell(scriptModern)
                    .addCell(languageName)
                    .addCell(language)
                    .addCell(languageModern)
                    .addCell(secondary)
                    .finishRow();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /** */
    private static Map<String, Set<String>> getInverse(
            Map<String, Set<String>> language_territories) {
        // get inverse relation
        Map<String, Set<String>> territory_languages = new TreeMap<>();
        for (Iterator<String> it = language_territories.keySet().iterator(); it.hasNext(); ) {
            String language = it.next();
            Set<String> territories = language_territories.get(language);
            for (Iterator<String> it2 = territories.iterator(); it2.hasNext(); ) {
                String territory = it2.next();
                Set<String> languages = territory_languages.get(territory);
                if (languages == null)
                    territory_languages.put(territory, languages = new TreeSet<String>(col));
                languages.add(language);
            }
        }
        return territory_languages;
    }

    static final Map<String, String> NAME_TO_REGION =
            getNameToCode(CodeType.territory, NameType.TERRITORY);
    static final Map<String, String> NAME_TO_CURRENCY =
            getNameToCode(CodeType.currency, NameType.CURRENCY);

    private static SortedMap<String, String> getNameToCode(CodeType codeType, NameType nameType) {
        SortedMap<String, String> temp = new TreeMap<String, String>(col);
        for (String territory : StandardCodes.make().getAvailableCodes(codeType)) {
            String name = englishNameGetter.getNameFromTypeEnumCode(nameType, territory);
            temp.put(name == null ? territory : name, territory);
        }
        temp = Collections.unmodifiableSortedMap(temp);
        return temp;
    }

    /**
     * @param value_delimiter TODO
     */
    private static void addTokens(
            String key, String values, String value_delimiter, Map<String, Set<String>> key_value) {
        if (values != null) {
            Set<String> s = key_value.get(key);
            if (s == null) key_value.put(key, s = new TreeSet<String>(col));
            s.addAll(Arrays.asList(values.split(value_delimiter)));
        }
    }

    static void addTokens(
            String key, String values, String value_delimiter, Multimap<String, String> key_value) {
        if (values != null) {
            key_value.putAll(key, Arrays.asList(values.split(value_delimiter)));
        }
    }

    public static void showContents(Appendable pw, String... items) {
        try {
            pw.append("</div>" + System.lineSeparator());
            pw.append("<h3>Contents</h3>" + System.lineSeparator());
            pw.append("<ol>" + System.lineSeparator());
            for (int i = 0; i < items.length; i += 2) {
                pw.append(
                        "<li><a href='#"
                                + items[i]
                                + "'>"
                                + items[i + 1]
                                + "</a></li>"
                                + System.lineSeparator());
            }
            pw.append("</ol><hr>" + System.lineSeparator());

            pw.append("<div align='center'>" + System.lineSeparator());
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
