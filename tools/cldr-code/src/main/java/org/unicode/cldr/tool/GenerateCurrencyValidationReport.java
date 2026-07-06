package org.unicode.cldr.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;

public class GenerateCurrencyValidationReport {

    private static final Pattern NUMERIC_PART_PATTERN = Pattern.compile("([#0,]+(?:\\.[#0]+)?)");
    private static final Pattern NS_PATTERN = Pattern.compile("\\[@numberSystem=\"([^\"]+)\"\\]");

    public static String getIntegerStructure(String pattern) {
        if (pattern == null) return null;
        String[] parts = pattern.split(";");
        String posPattern = parts[0];

        String numberPart = extractNumberPart(posPattern);
        if (numberPart == null) return null;

        String integerPart = numberPart.split("\\.")[0];
        return integerPart;
    }

    private static String extractNumberPart(String pattern) {
        Matcher matcher = NUMERIC_PART_PATTERN.matcher(pattern);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Set<String> getNumberingSystems(CLDRFile resolvedFile, CLDRFile unresolvedFile) {
        Set<String> systems = new HashSet<>();

        String defaultNS = resolvedFile.getStringValue("//ldml/numbers/defaultNumberingSystem");
        if (defaultNS != null) {
            systems.add(defaultNS);
        }

        for (String type : new String[] {"native", "traditional", "financial"}) {
            String ns = resolvedFile.getStringValue("//ldml/numbers/otherNumberingSystems/" + type);
            if (ns != null) {
                systems.add(ns);
            }
        }

        for (String path : unresolvedFile) {
            if (path.contains("decimalFormats") || path.contains("currencyFormats")) {
                Matcher m = NS_PATTERN.matcher(path);
                if (m.find()) {
                    systems.add(m.group(1));
                }
            }
        }

        if (systems.isEmpty()) {
            systems.add("latn");
        }
        return systems;
    }

    public static void main(String[] args) throws IOException {
        CLDRConfig config = CLDRConfig.getInstance();
        Factory factory = config.getCldrFactory();
        Set<String> locales = new TreeSet<>(factory.getAvailableLanguages());

        String outputPath = "../currency_validation_report.tsv";
        if (args.length > 0) {
            outputPath = args[0];
        }
        System.out.println("Writing report to " + outputPath);

        try (PrintWriter out = new PrintWriter(new FileWriter(outputPath))) {
            out.println(
                    "Locale\tNS\tDecimalPattern\tStdCurrPattern\tStdCurrAltAlpha\tStdCurrAltNoCurr\tAccCurrPattern\tAccCurrAltAlpha\tAccCurrAltNoCurr\tDecimalInteger\tStdInteger\tAlphaInteger\tNoCurrInteger\tAccInteger\tAccAlphaInteger\tAccNoCurrInteger\tAllEqual\tCurrencyEqual");

            StandardCodes sc = StandardCodes.make();
            for (String locale : locales) {
                Level cov = sc.getLocaleCoverageLevel("cldr", locale);
                if (!cov.isAtLeast(Level.MODERN)) {
                    continue;
                }
                CLDRFile resolvedCldrFile = factory.make(locale, true);
                CLDRFile unresolvedCldrFile = factory.make(locale, false);
                Set<String> numberingSystems =
                        getNumberingSystems(resolvedCldrFile, unresolvedCldrFile);

                for (String ns : numberingSystems) {
                    if (ns.equals("finance") || ns.equals("traditional")) {
                        continue;
                    }

                    String decimalPath =
                            "//ldml/numbers/decimalFormats[@numberSystem=\""
                                    + ns
                                    + "\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";

                    String stdCurrPath =
                            "//ldml/numbers/currencyFormats[@numberSystem=\""
                                    + ns
                                    + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                    String stdCurrAlphaPath =
                            "//ldml/numbers/currencyFormats[@numberSystem=\""
                                    + ns
                                    + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"alphaNextToNumber\"]";
                    String stdCurrNoCurrPath =
                            "//ldml/numbers/currencyFormats[@numberSystem=\""
                                    + ns
                                    + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"noCurrency\"]";

                    String accCurrPath =
                            "//ldml/numbers/currencyFormats[@numberSystem=\""
                                    + ns
                                    + "\"]/currencyFormatLength/currencyFormat[@type=\"accounting\"]/pattern[@type=\"standard\"]";
                    String accCurrAlphaPath =
                            "//ldml/numbers/currencyFormats[@numberSystem=\""
                                    + ns
                                    + "\"]/currencyFormatLength/currencyFormat[@type=\"accounting\"]/pattern[@type=\"standard\"][@alt=\"alphaNextToNumber\"]";
                    String accCurrNoCurrPath =
                            "//ldml/numbers/currencyFormats[@numberSystem=\""
                                    + ns
                                    + "\"]/currencyFormatLength/currencyFormat[@type=\"accounting\"]/pattern[@type=\"standard\"][@alt=\"noCurrency\"]";

                    String decimalPattern = resolvedCldrFile.getStringValue(decimalPath);
                    String stdCurrPattern = resolvedCldrFile.getStringValue(stdCurrPath);
                    String stdCurrAlphaPattern = resolvedCldrFile.getStringValue(stdCurrAlphaPath);
                    String stdCurrNoCurrPattern =
                            resolvedCldrFile.getStringValue(stdCurrNoCurrPath);
                    String accCurrPattern = resolvedCldrFile.getStringValue(accCurrPath);
                    String accCurrAlphaPattern = resolvedCldrFile.getStringValue(accCurrAlphaPath);
                    String accCurrNoCurrPattern =
                            resolvedCldrFile.getStringValue(accCurrNoCurrPath);

                    if (decimalPattern == null
                            && stdCurrPattern == null
                            && stdCurrAlphaPattern == null
                            && stdCurrNoCurrPattern == null
                            && accCurrPattern == null
                            && accCurrAlphaPattern == null
                            && accCurrNoCurrPattern == null) {
                        continue;
                    }

                    String decimalInt = getIntegerStructure(decimalPattern);
                    String stdInt = getIntegerStructure(stdCurrPattern);
                    String alphaInt = getIntegerStructure(stdCurrAlphaPattern);
                    String noCurrInt = getIntegerStructure(stdCurrNoCurrPattern);
                    String accInt = getIntegerStructure(accCurrPattern);
                    String accAlphaInt = getIntegerStructure(accCurrAlphaPattern);
                    String accNoCurrInt = getIntegerStructure(accCurrNoCurrPattern);

                    boolean currencyEqual = true;
                    String firstNonNull = null;
                    for (String s :
                            new String[] {
                                stdInt, alphaInt, noCurrInt, accInt, accAlphaInt, accNoCurrInt
                            }) {
                        if (s != null) {
                            if (firstNonNull == null) {
                                firstNonNull = s;
                            } else if (!firstNonNull.equals(s)) {
                                currencyEqual = false;
                                break;
                            }
                        }
                    }

                    boolean allEqual = currencyEqual;
                    if (allEqual && decimalInt != null && firstNonNull != null) {
                        allEqual = decimalInt.equals(firstNonNull);
                    }

                    out.printf(
                            "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%b\t%b\n",
                            locale,
                            ns,
                            decimalPattern,
                            stdCurrPattern,
                            stdCurrAlphaPattern,
                            stdCurrNoCurrPattern,
                            accCurrPattern,
                            accCurrAlphaPattern,
                            accCurrNoCurrPattern,
                            decimalInt,
                            stdInt,
                            alphaInt,
                            noCurrInt,
                            accInt,
                            accAlphaInt,
                            accNoCurrInt,
                            allEqual,
                            currencyEqual);
                }
            }
        }
    }
}
