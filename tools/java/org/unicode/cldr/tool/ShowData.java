/*
 ******************************************************************************
 * Copyright (C) 2005-2010, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.VariableReplacer;
import org.unicode.cldr.util.ExtractCollationRules;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.TransliteratorUtilities;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class ShowData {
    private static final int HELP1 = 0, HELP2 = 1, SOURCEDIR = 2, DESTDIR = 3,
        MATCH = 4, GET_SCRIPTS = 5,
        LAST_DIR = 6,
        COVERAGE = 7;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR().setDefault(CLDRPaths.MAIN_DIRECTORY),
        UOption.DESTDIR().setDefault(CLDRPaths.CHART_DIRECTORY + "summary/"),
        UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("getscript", 'g', UOption.NO_ARG),
        UOption.create("last", 'l', UOption.REQUIRES_ARG).setDefault(CLDRPaths.LAST_DIRECTORY + "common/main/"),
        UOption.create("coverage", 'c', UOption.REQUIRES_ARG).setDefault(Level.MODERN.toString()),
    };

    public static String dateFooter() {
        return "<p>Generation: " + CldrUtility.isoFormatDateOnly(new java.util.Date()) + "</p>" +
            System.lineSeparator();
    }

    static RuleBasedCollator uca = (RuleBasedCollator) Collator
        .getInstance(ULocale.ROOT);

    {
        uca.setNumericCollation(true);
    }

    static PathHeader.Factory prettyPathMaker = PathHeader.getFactory(CLDRConfig.getInstance().getEnglish());

    static CLDRFile english;

    static Set<String> locales;

    static Relation<String, String> parentToChildren = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);

    static Factory cldrFactory;

    public static void main(String[] args) throws Exception {
        // String p =
        // prettyPathMaker.getPrettyPath("//ldml/characters/exemplarCharacters[@alt=\"proposed-u151-4\"]");
        // String q = prettyPathMaker.getOriginal(p);

        double deltaTime = System.currentTimeMillis();
        try {
            CLDRConfig testInfo = ToolConfig.getToolInstance();
            UOption.parseArgs(args, options);
            String sourceDir = options[SOURCEDIR].value; // Utility.COMMON_DIRECTORY
            // + "main/";
            String targetDir = options[DESTDIR].value; // Utility.GEN_DIRECTORY +
            // "main/";
            cldrFactory = Factory.make(sourceDir, ".*");
            english = cldrFactory.make("en", true);
            String lastSourceDir = options[LAST_DIR].value; // Utility.COMMON_DIRECTORY

            Level requiredCoverage = Level.fromString(options[COVERAGE].toString()); // Utility.COMMON_DIRECTORY

            if (options[GET_SCRIPTS].doesOccur) {
                getScripts();
                return;
            }

            FileCopier.ensureDirectoryExists(options[DESTDIR].value);
            FileCopier.copy(ShowData.class, "summary-index.css", options[DESTDIR].value, "index.css");
            FileCopier.copy(ShowData.class, "summary-index.html", options[DESTDIR].value, "index.html");

            ToolUtilities.registerExtraTransliterators();

            // Factory collationFactory = Factory
            // .make(sourceDir.replace("incoming/vetted/","common/") + "../collation/", ".*");
            // ExtractCollationRules collationRules = new ExtractCollationRules();

            locales = new TreeSet<String>(cldrFactory.getAvailable());
            new CldrUtility.MatcherFilter(options[MATCH].value).retainAll(locales);
            // Set paths = new TreeSet();
            Set<PathHeader> prettySet = new TreeSet<PathHeader>();

            CLDRFile.Status status = new CLDRFile.Status();
            LocaleIDParser localeIDParser = new LocaleIDParser();

            //Map nonDistinguishingAttributes = new LinkedHashMap();
            //CLDRFile parent = null;

            Map<PathHeader, Relation<String, String>> pathHeaderToValuesToLocale = new TreeMap<PathHeader, Relation<String, String>>();

            Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();

            // get all the locales in a group (with same written language)
            LanguageTagParser ltp = new LanguageTagParser();

            LikelySubtags ls = new LikelySubtags();

            for (String locale : locales) {
                if (defaultContents.contains(locale)) {
                    continue;
                }
                if (locale.startsWith("supplem") || locale.startsWith("character") || locale.equals("root")) {
                    continue;
                }
                String max = ls.maximize(locale);
                if (max == null) {
                    max = locale;
                }
                String baseLanguage = ls.minimize(ltp.set(max).getLanguageScript());
                if (baseLanguage.equals(locale)) {
                    parentToChildren.put("root", locale);
                    parentToChildren.put(locale, locale);
                } else {
                    parentToChildren.put(baseLanguage, locale);
                }
            }

            for (Entry<String, Set<String>> group : parentToChildren.keyValuesSet()) {
                String locale = group.getKey();
                Set<String> children = group.getValue();
                Map<String, CLDRFile> sublocales = new TreeMap<String, CLDRFile>();

                boolean doResolved = localeIDParser.set(locale).getRegion().length() == 0;
                String languageSubtag = localeIDParser.getLanguage();
                //boolean isLanguageLocale = locale.equals(languageSubtag);

                CLDRFile file = cldrFactory.make(locale, true);
                if (file.isNonInheriting())
                    continue; // skip supplementals

                // get all of the paths
                Set<String> allPaths = new HashSet<>();
                CollectionUtilities.addAll(file.iterator(), allPaths);

                if (!locale.equals("root")) {
                    for (String childLocale : children) {
                        CLDRFile childCldrFile = cldrFactory.make(childLocale, false);
                        if (childCldrFile != null) {
                            CollectionUtilities.addAll(childCldrFile.iterator(), allPaths);
                        }
                        sublocales.put(childLocale, childCldrFile);
                    }
                }

                boolean showEnglish = !languageSubtag.equals("en");
                CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(testInfo.getSupplementalDataInfo(), locale);

                // put into set of simpler paths
                // and filter if necessary
                int skippedCount = 0;
                int aliasedCount = 0;
                int inheritedCount = 0;
                prettySet.clear();
                for (Iterator<String> it2 = allPaths.iterator(); it2.hasNext();) {
                    String path = it2.next();
                    if (path.indexOf("/alias") >= 0) {
                        skippedCount++;
                        continue; // skip code fllback
                    }
                    if (path.indexOf("/usesMetazone") >= 0) {
                        skippedCount++;
                        continue; // skip code fllback
                    }
                    if (path.indexOf("/references") >= 0) {
                        skippedCount++;
                        continue; // skip references
                    }
                    if (path.indexOf("[@alt=\"proposed") >= 0) {
                        skippedCount++;
                        continue; // skip code fllback
                    }
                    if (path.indexOf("/identity") >= 0) {
                        skippedCount++;
                        continue; // skip code fllback
                    }
                    PathHeader prettyString = prettyPathMaker.fromPath(path);
                    if (prettyString.getSectionId() != SectionId.Special) {
                        prettySet.add(prettyString);
                    }
                }

                PrintWriter pw = FileUtilities.openUTF8Writer(targetDir, locale + ".html");

                String[] headerAndFooter = new String[2];

                getChartTemplate(
                    "Locale Data Summary for " + getLocaleNameAndCode(locale),
                    ToolConstants.CHART_DISPLAY_VERSION,
                    "<script type='text/javascript'>" + System.lineSeparator()
                        + "if (location.href.split('?')[1].split(',')[0]=='hide') {" + System.lineSeparator()
                        + "document.write('<style>');" + System.lineSeparator()
                        + "document.write('.xx {display:none}');" + System.lineSeparator()
                        + "document.write('</style>');" + System.lineSeparator() + "}" + System.lineSeparator()
                        + "</script>",
                    headerAndFooter, locale.equals("root") ? "Main Charts Index" : null, false);
                pw.println(headerAndFooter[0]);
                showLinks(pw, locale);
                showChildren(pw, locale);
                if (true) {
                    pw.println("<p><b>Aliased/Inherited: </b><a href='" + locale
                        + ".html?hide'>Hide</a> <a href='" + locale
                        + ".html'>Show </a></p>");
                }
                pw.println("<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">");

                pw.println("<tr><th>No</th>"
                    + "<th width='10%'>Section</th>"
                    + "<th width='10%'>Page</th>"
                    + "<th width='10%'>Header</th>"
                    + "<th width='10%'>Code</th>"
                    + (showEnglish ? "<th>English</th>" : "")
                    + "<th>Native</th>"
                    + "<th>Sublocales…</th>"
                    + "</tr>");

                int count = 0;
                PathHeader oldParts = null;

                Relation<String, String> childValueToLocales = Relation.of(
                    new TreeMap<String, Set<String>>(), TreeSet.class);

                for (PathHeader prettyPath : prettySet) {
                    String path = prettyPath.getOriginalPath();
                    boolean zeroOutEnglish = path.indexOf("/references") < 0;

                    String source = file.getSourceLocaleID(path, status);
                    boolean isAliased = !status.pathWhereFound.equals(path);
                    if (isAliased) {
                        aliasedCount++;
                        continue;
                    }
                    String value = file.getStringValue(path);

                    //String baileyValue = file.getBaileyValue(path, null, null);
                    boolean isInherited = !source.equals(locale); // !CldrUtility.equals(baileyValue, value);
                    if (isInherited) {
                        inheritedCount++;
                    }

                    //StringBuffer tempDraftRef = new StringBuffer();

                    String lastValue = null;
                    boolean lastEquals = false;

                    childValueToLocales.clear();

                    for (Entry<String, CLDRFile> s : sublocales.entrySet()) {
                        String newChildValue = s.getValue().getStringValue(path);
                        if (newChildValue == null || CldrUtility.equals(newChildValue, value)) {
                            continue;
                        }
                        childValueToLocales.put(newChildValue, s.getKey());
                    }

                    String englishValue = null;
                    if (zeroOutEnglish) {
                        englishValue = "";
                    }
                    if (showEnglish
                        && null != (englishValue = english.getStringValue(path))) {
                    }

                    String statusClass = isAliased ? (isInherited ? " class='ah'"
                        : " class='a'") : (isInherited ? " class='h'" : "");

//                    Level currentCoverage = coverageLevel.getLevel(path);
//                    boolean hideCoverage = false;
//                    if (requiredCoverage.compareTo(currentCoverage) < 0) {
//                        hideCoverage = true;
//                    }

                    boolean hide = isAliased || isInherited;
                    if (!hide) {
                        Relation<String, String> valuesToLocales = pathHeaderToValuesToLocale.get(prettyPath);
                        if (valuesToLocales == null) {
                            pathHeaderToValuesToLocale.put(prettyPath, valuesToLocales = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
                        }
                        valuesToLocales.put(lastValue + "→→" + value, locale);
                    }
                    pw.println(
                        (hide ? "<tr class='xx'><td" : "<tr><td")
                            + statusClass
                            + ">"
                            + CldrUtility.getDoubleLinkedText(String.valueOf(++count))
                            + addPart(oldParts == null ? null : oldParts.getSection(), prettyPath.getSection())
                            + addPart(oldParts == null ? null : oldParts.getPage(), prettyPath.getPage())
                            + addPart(oldParts == null ? null : oldParts.getHeader(), prettyPath.getHeader())
                            + addPart(oldParts == null ? null : oldParts.getCode(), prettyPath.getCode())
                            // + "</td><td>" +
                            // TransliteratorUtilities.toHTML.transliterate(lastElement)
                            + showValue(showEnglish, englishValue, value, false)
                            + "</td>");
                    boolean isExemplar = path.contains("/exemplar");
                    showValue(pw, value, null, isExemplar);
                    if (!childValueToLocales.keyValuesSet().isEmpty()) {
                        for (Entry<String, Set<String>> x : childValueToLocales.keyValuesSet()) {
                            showValue(pw, x.getKey(), x.getValue(), isExemplar);
                        }
                        pw.println("<td class='info'><a class='rightLink' target='CLDR-ST-DOCS' href='http://cldr.org/index/charts#TOC-Summary'>ⓘ</a></td>");
                    }
                    pw.println("</tr>");
                    oldParts = prettyPath;
                }
                pw.println("</table><br><table>");
                pw.println("<tr><td class='a'>Aliased items: </td><td>" + aliasedCount
                    + "</td></tr>");
                pw.println("<tr><td class='h'>Inherited items:</td><td>"
                    + inheritedCount + "</td></tr>");
                if (skippedCount != 0)
                    pw.println("<tr><td>Omitted items:</td><td>" + skippedCount
                        + "</td></tr>");
                pw.println("</table>");

                // pw.println("</body></html>");
                pw.println(headerAndFooter[1]);
                pw.close();
            }
            PrintWriter pw = FileUtilities.openUTF8Writer(targetDir, "all-changed.html");
            String[] headerAndFooter = new String[2];

            getChartTemplate(
                "Locale Data Summary for ALL-CHANGED",
                ToolConstants.CHART_DISPLAY_VERSION,
                "",
                headerAndFooter, null, false);
            pw.println(headerAndFooter[0]);
            pw.println("<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">");
            pw.println("<tr>" +
                "<th>Section</th>" +
                "<th>Page</th>" +
                "<th>Header</th>" +
                "<th>Code</th>" +
                "<th>Old</th>" +
                "<th>Changed</th>" +
                "<th>Locales</th>" +
                "</tr>");
            for (Entry<PathHeader, Relation<String, String>> entry : pathHeaderToValuesToLocale.entrySet()) {
                PathHeader ph = entry.getKey();
                Set<Entry<String, Set<String>>> keyValuesSet = entry.getValue().keyValuesSet();
                String rowspan = keyValuesSet.size() == 1 ? ">" : " rowSpan='" + keyValuesSet.size() + "'>";
                pw
                    .append("<tr><td class='g'").append(rowspan)
                    .append(ph.getSectionId().toString())
                    .append("</td><td class='g'").append(rowspan)
                    .append(ph.getPageId().toString())
                    .append("</td><td class='g'").append(rowspan)
                    .append(ph.getHeader() == null ? "<i>n/a</i>" : ph.getHeader())
                    .append("</td><td class='g'").append(rowspan)
                    .append(ph.getCode())
                    .append("</td>");
                boolean addRow = false;
                for (Entry<String, Set<String>> s : keyValuesSet) {
                    String value = s.getKey();
                    int breakPoint = value.indexOf("→→");
                    if (addRow) {
                        pw.append("<tr>");
                    }
                    pw.append("<td>")
                        .append(DataShower.getPrettyValue(value.substring(0, breakPoint)))
                        .append("</td><td class='v'>")
                        .append(DataShower.getPrettyValue(value.substring(breakPoint + 2)))
                        .append("</td><td>")
                        .append(CollectionUtilities.join(s.getValue(), ", "))
                        .append("</td></tr>")
                        .append(System.lineSeparator());
                    addRow = true;
                }
            }
            pw.println(headerAndFooter[1]);
            pw.close();
        } finally {
            deltaTime = System.currentTimeMillis() - deltaTime;
            System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
            System.out.println("Done");
        }
    }

    public static void showValue(PrintWriter pw, String value, Set<String> locales, boolean isExemplar) {
        final boolean noLocales = locales == null || locales.isEmpty();
        pw.println("<td"
            + (isExemplar ? " style='max-width:20%'" : "")
            + (noLocales ? "" : " title='" + CollectionUtilities.join(locales, ", ") + "'")
            + (value == null ? "></i>n/a</i>" : " class='v'" + DataShower.getBidiStyle(value) + ">" + DataShower.getPrettyValue(value))
            + "</td>");
    }

    private static String addPart(String oldPart, String newPart) {
        String prefix;
        if (newPart.equals(oldPart)) {
            prefix = "</td><td class='n'>";
        } else if (newPart.length() == 0) {
            prefix = "</td><td>";
        } else {
            prefix = "</td><td class='g'>";
        }
        return prefix + TransliteratorUtilities.toHTML.transform(newPart);
    }

    private static void getScripts() throws IOException {
        Set<String> locales = cldrFactory.getAvailableLanguages();
        Set<String> scripts = new TreeSet<String>();
        //XPathParts parts = new XPathParts();
        Map<String, Map<String, Set<String>>> script_name_locales = new TreeMap<String, Map<String, Set<String>>>();
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "scriptNames.txt");
        for (Iterator<String> it = locales.iterator(); it.hasNext();) {
            String locale = it.next();
            System.out.println(locale);
            CLDRFile file = cldrFactory.make(locale, false);
            if (file.isNonInheriting())
                continue;
            String localeName = file.getName(locale);
            getScripts(localeName, scripts);
            if (!scripts.contains("Latn")) {
                out
                    .println(locale + "\t" + english.getName(locale) + "\t"
                        + localeName);
            }
            for (Iterator<String> it2 = UnicodeScripts.iterator(); it2.hasNext();) {
                String script = it2.next();
                if (script.equals("Latn"))
                    continue;
                String name = file.getName(CLDRFile.SCRIPT_NAME, script);
                if (getScripts(name, scripts).contains(script)) {
                    Map<String, Set<String>> names_locales = script_name_locales.get(script);
                    if (names_locales == null)
                        script_name_locales.put(script, names_locales = new TreeMap<String, Set<String>>());
                    Set<String> localeSet = names_locales.get(name);
                    if (localeSet == null)
                        names_locales.put(name, localeSet = new TreeSet<String>());
                    localeSet.add(getLocaleNameAndCode(locale));
                }
            }
        }
        for (Iterator<String> it2 = UnicodeScripts.iterator(); it2.hasNext();) {
            String script = it2.next();
            Object names = script_name_locales.get(script);
            out.println(script + "\t("
                + english.getName(CLDRFile.SCRIPT_NAME, script) + ")\t" + names);
        }
        out.close();
    }

    static Set<String> UnicodeScripts = Collections.unmodifiableSet(new TreeSet<String>(Arrays
        .asList(new String[] { "Arab", "Armn", "Bali", "Beng", "Bopo", "Brai",
            "Bugi", "Buhd", "Cans", "Cher", "Copt", "Cprt", "Cyrl", "Deva",
            "Dsrt", "Ethi", "Geor", "Glag", "Goth", "Grek", "Gujr", "Guru",
            "Hang", "Hani", "Hano", "Hebr", "Hira", "Hrkt", "Ital", "Kana",
            "Khar", "Khmr", "Knda", "Laoo", "Latn", "Limb", "Linb", "Mlym",
            "Mong", "Mymr", "Nkoo", "Ogam", "Orya", "Osma", "Phag", "Phnx",
            "Qaai", "Runr", "Shaw", "Sinh", "Sylo", "Syrc", "Tagb", "Tale",
            "Talu", "Taml", "Telu", "Tfng", "Tglg", "Thaa", "Thai", "Tibt",
            "Ugar", "Xpeo", "Xsux", "Yiii" })));

    private static Set<String> getScripts(String exemplars, Set<String> results) {
        results.clear();
        if (exemplars == null)
            return results;
        for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet()
            .addAll(exemplars)); it.next();) {
            int cp = it.codepoint;
            int script = UScript.getScript(cp);
            results.add(UScript.getShortName(script));
        }
        return results;
    }

    private static void showCollation(Factory collationFactory, String locale,
        ExtractCollationRules collationRules) {
        CLDRFile collationFile;
        try {
            collationFile = collationFactory.make(locale, false);
        } catch (RuntimeException e) {
            return; // skip
        }
        collationRules.set(collationFile);
        for (Iterator<String> it = collationRules.iterator(); it.hasNext();) {
            String key = it.next();
            System.out.println(key + ": ");
            String rules = collationRules.getRules(key);
            System.out.println(rules);
        }
    }

    private static String showValue(boolean showEnglish, String comparisonValue,
        String mainValue, boolean lastEquals) {
        return !showEnglish ? ""
            : comparisonValue == null ? (lastEquals ? "</td><td>=" : "</td><td><i>n/a</i>")
                : comparisonValue.length() == 0 ? "</td><td>&nbsp;"
                    : comparisonValue.equals(mainValue) ? "</td><td>="
                        : "</td><td class='e'" + DataShower.getBidiStyle(comparisonValue) + ">" + DataShower.getPrettyValue(comparisonValue);
    }

    static DataShower dataShower = new DataShower();

    public static class DataShower {
        static Transliterator toLatin = Transliterator.getInstance("any-latin");

        static UnicodeSet BIDI_R = new UnicodeSet(
            "[[:Bidi_Class=R:][:Bidi_Class=AL:]]");

        static String getBidiStyle(String cellValue) {
            return BIDI_R.containsSome(cellValue) ? " style='direction:rtl'" : "";
        }

        public static String getPrettyValue(String textToInsert) {
            String outValue = TransliteratorUtilities.toHTML
                .transliterate(textToInsert);
            String transValue = textToInsert;
            //String span = "";
            try {
                transValue = toLatin.transliterate(textToInsert);
            } catch (RuntimeException e) {
            }
            if (!transValue.equals(textToInsert)) {
                // WARNING: we use toXML in attributes
                outValue = "<span title='"
                    + TransliteratorUtilities.toXML.transliterate(transValue) + "'>"
                    + outValue + "</span>";
            }
            return outValue;
        }
    }

    private static String getNda(Set<String> skipList, Map<String, String> nonDistinguishingAttributes,
        CLDRFile file, String path, String parentFullPath, StringBuffer draftRef) {
        draftRef.setLength(0);
        if (parentFullPath != null && !parentFullPath.equals(path)) {
            file.getNonDistinguishingAttributes(parentFullPath,
                nonDistinguishingAttributes, skipList);
            if (nonDistinguishingAttributes.size() != 0) {
                String parentNda = "";
                for (Iterator<String> it = nonDistinguishingAttributes.keySet().iterator(); it
                    .hasNext();) {
                    String key = it.next();
                    String value = nonDistinguishingAttributes.get(key);
                    if (key.equals("draft") && !value.equals("contributed")) {
                        if (draftRef.length() != 0)
                            draftRef.append(",");
                        draftRef.append("d");
                    } else if (key.equals("alt")) {
                        if (draftRef.length() != 0)
                            draftRef.append(",");
                        draftRef.append("a");
                    } else if (key.equals("references")) {
                        if (draftRef.length() != 0)
                            draftRef.append(",");
                        draftRef.append(nonDistinguishingAttributes.get(key));
                    } else {
                        if (parentNda.length() != 0)
                            parentNda += ", ";
                        parentNda += key + "=" + nonDistinguishingAttributes.get(key);
                    }
                }
                if (parentNda.length() != 0) {
                    parentNda = parentNda.replaceAll("[/]", "/\u200B");
                    parentNda = "[" + parentNda + "]";
                }
                return parentNda;
            }
        }
        return "";
    }

    private static void showLinks(PrintWriter pw, String locale) {
        pw.print("<p>");
        showLinks2(pw, locale);
        pw.println("</p>");
    }

    private static void showLinks2(PrintWriter pw, String locale) {
        String parent = LocaleIDParser.getParent(locale);
        if (parent != null) {
            showLinks2(pw, parent);
            pw.print(" &gt; ");
        }
        pw.println(getLinkedLocaleName(locale));
    }

    private static void showChildren(PrintWriter pw, String locale) {
        LanguageTagParser ltp = new LanguageTagParser();
        TreeMap<String, String> nameToLocale = new TreeMap<>();
        for (String sublocale : parentToChildren.get(locale)) {
            String minimized = LIKELY.minimize(sublocale);
            nameToLocale.put(getEnglishLocaleName(sublocale), minimized == null ? sublocale : minimized);
        }

        boolean isRoot = locale.equals("root");
        char lastFirstChar = 0;
        boolean first = true;
        for (Entry<String, String> nameAndCode : nameToLocale.entrySet()) {
            String name = nameAndCode.getKey();
            String localeCode = nameAndCode.getValue();
            if (localeCode.equals(locale)) {
                continue;
            }
            char firstChar = name.charAt(0);
            if (first) {
                pw.print(System.lineSeparator() + "<p style='margin-left:5em'>&gt; ");
                lastFirstChar = firstChar;
                first = false;
            } else if (firstChar != lastFirstChar) {
                pw.print("</p>" + System.lineSeparator() + "<p style='margin-left:5em'> ");
                lastFirstChar = firstChar;
            } else {
                pw.print(", ");
            }
            if (isRoot) {
                pw.print(getLinkedLocaleName(localeCode));
            } else {
                pw.print(getLocaleMinusBaseAndCode(localeCode));
            }
        }
        if (first) {
            pw.println("</p>");
        }
    }

    public static String getLinkedLocaleName(String locale) {
        return "<a href='" + locale + ".html'>" + getEnglishLocaleName(locale) + "</a> [" + locale + "]";
    }

    public static String getEnglishLocaleName(String locale) {
        return english.getName(locale, true, CLDRFile.SHORT_ALTS);
    }

    private static String getLocaleNameAndCode(String locale) {
        return getEnglishLocaleName(locale) + " [" + locale + "]";
    }

    private static String getLocaleMinusBaseAndCode(String locale) {
        String full = getEnglishLocaleName(locale);
        int leftParen = full.indexOf('(');
        if (leftParen >= 0) {
            full = full.substring(leftParen + 1, full.lastIndexOf(')'));
        }
        return full + " [" + locale + "]";
    }

    static final LikelySubtags LIKELY = new LikelySubtags();

    // public static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm",
    // ULocale.ENGLISH);

    static public void getChartTemplate(String title, String version,
        String header, String[] headerAndFooter, String indexTitle, boolean showDate) throws IOException {
        if (version == null) {
            version = ToolConstants.CHART_DISPLAY_VERSION;
        }
        VariableReplacer langTag = new VariableReplacer()
            .add("%title%", title)
            .add("%header%", header)
            .add("%index-title%", "Index")
            .add("%index%", "index.html")
            .add("%header%", header)
            .add("%version%", version)
            .add("%date%", showDate ? CldrUtility.isoFormatDateOnly(new Date()) : "");
        if (indexTitle != null) {
            langTag
                .add("%index-title%", indexTitle)
                .add("%index%", "../index.html");
        }
        // "$" //
        // + "Date" //
        // + "$") // odd style to keep CVS from substituting
        ; // isoDateFormat.format(new Date())
        BufferedReader input = ToolUtilities.getUTF8Data("chart-template.html");
        StringBuffer result = new StringBuffer();
        while (true) {
            String line = input.readLine();
            if (line == null)
                break;
            String langTagPattern = langTag.replace(line);
            if (line.indexOf("%body%") >= 0) {
                headerAndFooter[0] = result.toString();
                result.setLength(0);
                continue;
            }
            result.append(langTagPattern).append(System.lineSeparator());
        }
        headerAndFooter[1] = result.toString();
    }
}
