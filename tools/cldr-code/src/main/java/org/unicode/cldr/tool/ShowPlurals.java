package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.DecimalQuantitySamples;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.unicode.cldr.test.BuildIcuCompactDecimalFormat;
import org.unicode.cldr.test.BuildIcuCompactDecimalFormat.CurrencyStyle;
import org.unicode.cldr.tool.GeneratePluralRanges.RangeSample;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.PluralSnapshot;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

public class ShowPlurals {

    private static final String NO_PLURAL_DIFFERENCES = "<i>no plural differences</i>";
    private static final String NOT_AVAILABLE =
            "<i>Not available.<br>Please <a target='_blank' href='"
                    + CLDRURLS.CLDR_NEWTICKET_URL
                    + "'>file a ticket</a> to supply.</i>";
    final SupplementalDataInfo supplementalDataInfo;

    public static void main(String[] args) {
        Factory cldrFactory =
                CLDRConfig.getInstance().getCldrFactory(); // .make(CLDRPaths.MAIN_DIRECTORY, ".*");
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            new ShowPlurals().printPlurals(english, null, pw, cldrFactory);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public ShowPlurals() {
        supplementalDataInfo = CLDRConfig.getInstance().getSupplementalDataInfo();
    }

    public ShowPlurals(SupplementalDataInfo supplementalDataInfo) {
        this.supplementalDataInfo = supplementalDataInfo;
    }

    public void printPlurals(
            CLDRFile english, String localeFilter, PrintWriter index, Factory factory)
            throws IOException {
        String section1 = "Rules";
        String section2 = "Comparison";

        final String title = "Language Plural Rules";
        final PrintWriter pw =
                new PrintWriter(
                        new FormattedFileWriter(
                                null, title, null, ShowLanguages.SUPPLEMENTAL_INDEX_ANCHORS));

        pw.append("<div style='margin-right:2em; margin-left:2em'>\n");
        ShowLanguages.showContents(pw, "rules", "Rules", "comparison", "Comparison");

        pw.append(
                "<h2>"
                        + CldrUtility.getDoubleLinkedText("rules", "1. " + section1)
                        + "</h2>"
                        + System.lineSeparator());
        pw.append("<div style='margin-right:2em; margin-left:2em'>\n");
        printPluralTable(english, localeFilter, pw, factory);
        pw.append("</div>\n");

        pw.append(
                "<h2>"
                        + CldrUtility.getDoubleLinkedText("comparison", "2. " + section2)
                        + "</h2>"
                        + System.lineSeparator());
        pw.append(
                "<p style='text-align:left'>The plural forms are abbreviated by first letter, with 'x' for 'other'. "
                        + "If values are made redundant by explicit 0 and 1, they are underlined. "
                        + "The fractional and integral results are separated for clarity.</p>"
                        + System.lineSeparator());
        pw.append("<div style='margin-right:2em; margin-left:2em'>\n");
        PluralSnapshot.writeTables(english, pw);
        pw.append("</div>\n");
        pw.append("</div>\n");
        appendBlanksForScrolling(pw);
        pw.close();
    }

    public static void appendBlanksForScrolling(final Appendable pw) {
        try {
            pw.append(Utility.repeat("<br>", 100)).append(System.lineSeparator());
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public void printPluralTable(
            CLDRFile english, String localeFilter, Appendable appendable, Factory factory)
            throws IOException {

        final TablePrinter tablePrinter =
                new TablePrinter()
                        .setTableAttributes("class='dtf-table'")
                        .addColumn("Name", "class='source'", null, "class='source'", true)
                        .setSortPriority(0)
                        .setHeaderAttributes("class='dtf-th'")
                        .setCellAttributes("class='dtf-s'")
                        .setBreakSpans(true)
                        .addColumn(
                                "Code",
                                "class='source'",
                                CldrUtility.getDoubleLinkMsg(),
                                "class='source'",
                                true)
                        .setHeaderAttributes("class='dtf-th'")
                        .setCellAttributes("class='dtf-s'")
                        .addColumn("Type", "class='source'", null, "class='source'", true)
                        .setHeaderAttributes("class='dtf-th'")
                        .setCellAttributes("class='dtf-s'")
                        .setBreakSpans(true)
                        .addColumn("Category", "class='target'", null, "class='target'", true)
                        .setHeaderAttributes("class='dtf-th'")
                        .setCellAttributes("class='dtf-s'")
                        .setSpanRows(false)
                        .addColumn("Examples", "class='target'", null, "class='target'", true)
                        .setHeaderAttributes("class='dtf-th'")
                        .setCellAttributes("class='dtf-s'")
                        .addColumn("Minimal Pairs", "class='target'", null, "class='target'", true)
                        .setHeaderAttributes("class='dtf-th'")
                        .setCellAttributes("class='dtf-s'")
                        .addColumn("Rules", "class='target'", null, "class='target' nowrap", true)
                        .setHeaderAttributes("class='dtf-th'")
                        .setCellAttributes("class='dtf-s'")
                        .setSpanRows(false);
        PluralRulesFactory prf = PluralRulesFactory.getInstance(supplementalDataInfo);
        // Map<ULocale, PluralRulesFactory.SamplePatterns> samples =
        // PluralRulesFactory.getLocaleToSamplePatterns();
        Set<String> cardinalLocales = supplementalDataInfo.getPluralLocales(PluralType.cardinal);
        Set<String> ordinalLocales = supplementalDataInfo.getPluralLocales(PluralType.ordinal);
        Set<String> all = new LinkedHashSet<>(cardinalLocales);
        all.addAll(ordinalLocales);

        LanguageTagCanonicalizer canonicalizer = new LanguageTagCanonicalizer();
        SampleMaker sampleMaker = new SampleMaker();

        for (String locale : supplementalDataInfo.getPluralLocales()) {
            if (localeFilter != null && !localeFilter.equals(locale) || locale.equals("root")) {
                continue;
            }
            final String name = english.nameGetter().getNameFromIdentifier(locale);
            String canonicalLocale = canonicalizer.transform(locale);
            if (!locale.equals(canonicalLocale)) {
                String redirect =
                        "<i>=<a href='#" + canonicalLocale + "'>" + canonicalLocale + "</a></i>";
                tablePrinter
                        .addRow()
                        .addCell(name)
                        .addCell(locale)
                        .addCell(redirect)
                        .addCell(redirect)
                        .addCell(redirect)
                        .addCell(redirect)
                        .addCell(redirect)
                        .finishRow();
                continue;
            }

            CLDRFile cldrFile2;
            try {
                cldrFile2 = factory.make(locale, true);
            } catch (Exception e1) {
                continue;
            }
            sampleMaker.setCldrFile(cldrFile2);

            for (PluralType pluralType : PluralType.values()) {
                if (pluralType == PluralType.ordinal && !ordinalLocales.contains(locale)
                        || pluralType == PluralType.cardinal && !cardinalLocales.contains(locale)) {
                    continue;
                }
                final PluralInfo plurals = supplementalDataInfo.getPlurals(pluralType, locale);
                ULocale locale2 = new ULocale(locale);
                final PluralMinimalPairs samplePatterns =
                        PluralMinimalPairs.getInstance(locale2.toString());
                //                    pluralType == PluralType.ordinal ? null
                //                    : CldrUtility.get(samples, locale2);

                String rules = plurals.getRules();
                rules +=
                        rules.length() == 0
                                ? "other:<i>everything</i>"
                                : ";other:<i>everything else</i>";
                rules = rules.replace(":", " → ").replace(";", ";<br>");
                PluralRules pluralRules = plurals.getPluralRules();
                // final Map<PluralInfo.Count, String> typeToExamples =
                // plurals.getCountToStringExamplesMap();
                // final String examples = typeToExamples.get(type).toString().replace(";",
                // ";<br>");
                Set<Count> counts = plurals.getCounts();
                for (PluralInfo.Count count : counts) {
                    String keyword = count.toString();
                    DecimalQuantitySamples exampleList =
                            pluralRules.getDecimalSamples(
                                    keyword,
                                    PluralRules.SampleType
                                            .INTEGER); // plurals.getSamples9999(count);
                    DecimalQuantitySamples exampleList2 =
                            pluralRules.getDecimalSamples(keyword, PluralRules.SampleType.DECIMAL);
                    if (exampleList == null) {
                        exampleList = exampleList2;
                        exampleList2 = null;
                    }
                    String examples = getExamples(exampleList);
                    if (exampleList2 != null) {
                        examples += "<br>" + getExamples(exampleList2);
                    }
                    String rule = pluralRules.getRules(keyword);
                    rule =
                            rule != null
                                    ? rule.replace(":", " → ")
                                            .replace(" and ", " and<br>&nbsp;&nbsp;")
                                            .replace(" or ", " or<br>")
                                    : counts.size() == 1
                                            ? "<i>everything</i>"
                                            : "<i>everything else</i>";

                    String sample = counts.size() == 1 ? NO_PLURAL_DIFFERENCES : NOT_AVAILABLE;
                    if (samplePatterns != null) {
                        String samplePattern =
                                samplePatterns.get(
                                        pluralType.standardType,
                                        Count.valueOf(
                                                keyword)); // CldrUtility.get(samplePatterns.keywordToPattern, Count.valueOf(keyword));
                        if (samplePattern != null) {
                            DecimalQuantity sampleDecimal =
                                    PluralInfo.getNonZeroSampleIfPossible(exampleList);
                            sample = sampleMaker.getSample(sampleDecimal, samplePattern);
                            if (exampleList2 != null) {
                                sampleDecimal = PluralInfo.getNonZeroSampleIfPossible(exampleList2);
                                sample +=
                                        "<br>"
                                                + sampleMaker.getSample(
                                                        sampleDecimal, samplePattern);
                            }
                        }
                    }
                    tablePrinter
                            .addRow()
                            .addCell(name)
                            .addCell(locale)
                            .addCell(pluralType.toString())
                            .addCell(count.toString())
                            .addCell(examples.toString())
                            .addCell(sample)
                            .addCell(rule)
                            .finishRow();
                }
            }
            List<RangeSample> rangeInfoList = null;
            try {
                rangeInfoList =
                        new GeneratePluralRanges(supplementalDataInfo).getRangeInfo(cldrFile2);
            } catch (Exception e) {
            }
            if (rangeInfoList != null) {
                for (RangeSample item : rangeInfoList) {
                    tablePrinter
                            .addRow()
                            .addCell(name)
                            .addCell(locale)
                            .addCell("range")
                            .addCell(item.start + "+" + item.end)
                            .addCell(item.min + "–" + item.max)
                            .addCell(item.resultExample.replace(". ", ".<br>"))
                            .addCell(item.start + " + " + item.end + " → " + item.result)
                            .finishRow();
                }
            } else {
                String message =
                        supplementalDataInfo
                                                .getPlurals(PluralType.cardinal, locale)
                                                .getCounts()
                                                .size()
                                        == 1
                                ? NO_PLURAL_DIFFERENCES
                                : NOT_AVAILABLE;
                tablePrinter
                        .addRow()
                        .addCell(name)
                        .addCell(locale)
                        .addCell("range")
                        .addCell("<i>n/a</i>")
                        .addCell("<i>n/a</i>")
                        .addCell(message)
                        .addCell("<i>n/a</i>")
                        .finishRow();
            }
        }
        appendable.append(tablePrinter.toTable()).append(System.lineSeparator());
    }

    private String getExamples(DecimalQuantitySamples exampleList) {
        return Joiner.on(", ").join(exampleList.getSamples()) + (exampleList.bounded ? "" : ", …");
    }

    static final class SampleMaker {
        private ICUServiceBuilder icusb = new ICUServiceBuilder();
        private CLDRFile cldrFile;

        private void setCldrFile(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
            icusb.setCldrFile(cldrFile);
        }

        private String getSample(DecimalQuantity numb, String samplePattern) {
            String sample;
            String value;
            if (numb.getExponent() > 0) {
                Set<String> debugCreationErrors = new LinkedHashSet<>();
                String[] debugOriginals = null;
                CompactDecimalFormat cdfCurr =
                        BuildIcuCompactDecimalFormat.build(
                                cldrFile,
                                debugCreationErrors,
                                debugOriginals,
                                CompactStyle.SHORT,
                                ULocale.forLanguageTag(cldrFile.getLocaleID()),
                                CurrencyStyle.PLAIN,
                                null);
                value = cdfCurr.format(numb.toDouble());
            } else {
                NumberFormat nf = icusb.getGenericNumberFormat("latn");
                nf.setMaximumFractionDigits((int) numb.getPluralOperand(Operand.v));
                nf.setMinimumFractionDigits((int) numb.getPluralOperand(Operand.v));
                value = nf.format(numb.toDouble());
            }
            sample =
                    samplePattern
                            .replace('\u00A0', '\u0020')
                            .replace("{0}", value)
                            .replace(". ", ".<br>");
            return sample;
        }
    }
}
