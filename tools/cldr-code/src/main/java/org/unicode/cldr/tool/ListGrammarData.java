package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.BestMinimalPairSamples;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StandardCodes;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Output;

public class ListGrammarData {
    static CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final CLDRFile ENGLISH = CONFIG.getEnglish();

    private enum MyOptions {
        fileFilter(new Params().setHelp("filter files by locale").setMatch(".*").setDefault(".*")),
        orgFilter(new Params().setHelp("filter files by organization").setDefault("cldr")),
        coverageSkip(new Params().setHelp("filter files by coverage").setMatch(".*").setDefault(Level.UNDETERMINED.toString())),
        exampleHtml(new Params().setHelp("display exampleHtml")),
        verbose(new Params().setHelp("verbose debugging messages")),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        MyOptions.parse(args);
        Matcher fileFilter = Pattern.compile(MyOptions.fileFilter.option.getValue()).matcher("");
        String orgFilter = MyOptions.orgFilter.option.getValue();
        Boolean exampleHtml = MyOptions.exampleHtml.option.doesOccur();
        Matcher coverageSkip = Pattern.compile(MyOptions.coverageSkip.option.getValue()).matcher("");
        Boolean verbose = MyOptions.verbose.option.doesOccur();


        Factory factory = CONFIG.getCldrFactory();
        PathHeader.Factory phf = PathHeader.getFactory();
        LinkedHashSet<String> errors = new LinkedHashSet<>();
        Multimap<String,String> lines = LinkedHashMultimap.create();
        LikelySubtags likelySubtags = new LikelySubtags();

        for (String localeRaw : factory.getAvailableLanguages()) {
            if (!fileFilter.reset(localeRaw).matches()) {
                continue;
            }
            Level coverage = StandardCodes.make().getLocaleCoverageLevel(Organization.valueOf(orgFilter), localeRaw);
            if (coverageSkip.reset(coverage.toString()).matches()) {
                // System.out.println("skipping " + localeRaw);
                continue;
            }
            String locale = likelySubtags.minimize(localeRaw);
            if (!localeRaw.equals(locale)) {
                // System.out.println("Skipping " + locale);
                continue;
            }
            CLDRFile cldrFile = factory.make(locale, true);
            ExampleGenerator exampleGenerator = null;
            if (exampleHtml) {
                exampleGenerator = new ExampleGenerator(cldrFile, CONFIG.getEnglish(), locale);
            }
            BestMinimalPairSamples bestMinimalPairSamples = new BestMinimalPairSamples(cldrFile, null);
            Map<PathHeader, String> pathHeaderToValue = new TreeMap<>();
            Multimap<String, String> sectionPageHeaderToCodes = TreeMultimap.create();
            for (String path : cldrFile.fullIterable()) {
                if (!path.startsWith("//ldml/numbers/minimalPairs")) {
                    continue;
                }
                PathHeader ph = phf.fromPath(path);
                String value = cldrFile.getStringValue(path);
                pathHeaderToValue.put(ph, value);
                sectionPageHeaderToCodes.put(sectionPageHeader(ph), ph.getCode());
            }
            if (pathHeaderToValue.isEmpty()) {
                continue;
            }
            // System.out.println(locale);
            final String names = locale + "\t" + ENGLISH.getName(locale);
            for (Entry<PathHeader, String> entry : pathHeaderToValue.entrySet()) {

                final PathHeader ph = entry.getKey();
                Collection<String> codes = sectionPageHeaderToCodes.get(sectionPageHeader(ph));
                if (codes.size() == 1) {
                    errors.add("*" + names + "\t" + ph + "\t" + "singlular!");
                    continue;
                }
                final String minimalPattern = entry.getValue();
                Output<String> shortUnitId = new Output<>();
                String sample = bestMinimalPairSamples.getBestValue(ph.getHeader(), ph.getCode(), shortUnitId);
                String example;
                lines.put(ph.getHeader(), names
                    + "\t" + coverage
                    + "\t" + ph
                    + "\t" + minimalPattern
                    + "\t" + sample
                    + "\t" + shortUnitId
                    + (exampleHtml ? "\t" + ExampleGenerator.simplify(exampleGenerator.getExampleHtml(ph.getOriginalPath(), pathHeaderToValue.get(ph))) : ""));
            }
        }
        for (String key : new TreeSet<>(lines.keySet())) {
            String lastLocale = "";
            for (String line : lines.get(key)) {
                String locale = line.substring(0,line.indexOf('\t'));
                if (!locale.equals(lastLocale)) {
                    lastLocale = locale;
                    System.out.println();
                }
                System.out.println(line);
            }
        }

        if (false) for (String error : errors) {
            System.out.println(error);
        }
    }

    public static String sectionPageHeader(PathHeader ph) {
        return ph.getSectionId() + "|" + ph.getPageId() + "|" + ph.getHeader();
    }

}
