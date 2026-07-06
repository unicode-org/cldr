package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

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

    private static Set<String> getNumberingSystems(CLDRFile cldrFile) {
        Set<String> systems = new HashSet<>();
        for (String path : cldrFile) {
            Matcher m = NS_PATTERN.matcher(path);
            if (m.find()) {
                systems.add(m.group(1));
            }
        }
        return systems;
    }

    public static void main(String[] args) throws IOException {
        CLDRConfig config = CLDRConfig.getInstance();
        Factory factory = config.getCldrFactory();
        Set<String> locales = new TreeSet<>(factory.getAvailableLanguages());

        System.out.println("Locale\tNS\tDecimalPattern\tStdCurrPattern\tStdCurrAltAlpha\tStdCurrAltNoCurr\tAccCurrPattern\tAccCurrAltAlpha\tAccCurrAltNoCurr\tDecimalInteger\tStdInteger\tAlphaInteger\tNoCurrInteger\tAccInteger\tAccAlphaInteger\tAccNoCurrInteger\tAllEqual\tCurrencyEqual");

        for (String locale : locales) {
            CLDRFile cldrFile = factory.make(locale, true);
            Set<String> numberingSystems = getNumberingSystems(cldrFile);
            
            String defaultNS = cldrFile.getStringValue("//ldml/numbers/defaultNumberingSystem");
            if (defaultNS != null) {
                numberingSystems.add(defaultNS);
            }
            if (numberingSystems.isEmpty()) {
                numberingSystems.add("latn");
            }

            for (String ns : numberingSystems) {
                if (ns.equals("finance") || ns.equals("traditional")) {
                    continue;
                }

                String decimalPath = "//ldml/numbers/decimalFormats[@numberSystem=\"" + ns + "\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                
                String stdCurrPath = "//ldml/numbers/currencyFormats[@numberSystem=\"" + ns + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String stdCurrAlphaPath = "//ldml/numbers/currencyFormats[@numberSystem=\"" + ns + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"alphaNextToNumber\"]";
                String stdCurrNoCurrPath = "//ldml/numbers/currencyFormats[@numberSystem=\"" + ns + "\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"noCurrency\"]";
                
                String accCurrPath = "//ldml/numbers/currencyFormats[@numberSystem=\"" + ns + "\"]/currencyFormatLength/currencyFormat[@type=\"accounting\"]/pattern[@type=\"standard\"]";
                String accCurrAlphaPath = "//ldml/numbers/currencyFormats[@numberSystem=\"" + ns + "\"]/currencyFormatLength/currencyFormat[@type=\"accounting\"]/pattern[@type=\"standard\"][@alt=\"alphaNextToNumber\"]";
                String accCurrNoCurrPath = "//ldml/numbers/currencyFormats[@numberSystem=\"" + ns + "\"]/currencyFormatLength/currencyFormat[@type=\"accounting\"]/pattern[@type=\"standard\"][@alt=\"noCurrency\"]";

                String decimalPattern = cldrFile.getStringValue(decimalPath);
                String stdCurrPattern = cldrFile.getStringValue(stdCurrPath);
                String stdCurrAlphaPattern = cldrFile.getStringValue(stdCurrAlphaPath);
                String stdCurrNoCurrPattern = cldrFile.getStringValue(stdCurrNoCurrPath);
                String accCurrPattern = cldrFile.getStringValue(accCurrPath);
                String accCurrAlphaPattern = cldrFile.getStringValue(accCurrAlphaPath);
                String accCurrNoCurrPattern = cldrFile.getStringValue(accCurrNoCurrPath);

                if (decimalPattern == null && stdCurrPattern == null && stdCurrAlphaPattern == null &&
                    stdCurrNoCurrPattern == null && accCurrPattern == null && accCurrAlphaPattern == null &&
                    accCurrNoCurrPattern == null) {
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
                for (String s : new String[]{stdInt, alphaInt, noCurrInt, accInt, accAlphaInt, accNoCurrInt}) {
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
                
                System.out.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%b\t%b\n",
                        locale, ns,
                        decimalPattern, stdCurrPattern, stdCurrAlphaPattern, stdCurrNoCurrPattern,
                        accCurrPattern, accCurrAlphaPattern, accCurrNoCurrPattern,
                        decimalInt, stdInt, alphaInt, noCurrInt, accInt, accAlphaInt, accNoCurrInt,
                        allEqual, currencyEqual);
            }
        }
    }
}
