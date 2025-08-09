package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.Patterns;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.Splitters;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;

public class GenerateDerivedMain {
    private static final boolean DEBUG = System.getProperty("GenerateDerivedMain:DEBUG") != null;
    ;
    private static final SimpleFormatter CURRENCY_FORMAT =
            SimpleFormatter.compile(
                    "//ldml/numbers/currencyFormats[@numberSystem=\"{0}\"]/currencyFormatLength/currencyFormat[@type=\"{1}\"]/pattern[@type=\"standard\"]");
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static Factory cldrFactory = CLDR_CONFIG.getCldrFactory();
    static PathHeader.Factory phf = PathHeader.getFactory();

    public static void main(String[] args) {
        // later move to test
        System.out.println(
                Joiners.TAB.join(
                        "locale",
                        "numberSystem",
                        "code",
                        "status",
                        "oldValue",
                        "newValue",
                        "es.oldValue",
                        "es.newValue"));

        Map<String, Level> cldrLocaleLevels =
                StandardCodes.make().getLocalesToLevelsFor(Organization.cldr);
        Set<String> locales = DEBUG ? Set.of("sw") : cldrFactory.getAvailable();
        Set<String> noDifference = new LinkedHashSet<>();
        Set<String> wsDifference = new LinkedHashSet<>();
        for (String locale : locales) {
            Level level = cldrLocaleLevels.getOrDefault(locale, Level.BASIC);
            if (DEBUG) System.out.println("\nLocale " + locale);
            Set<XPathParts> basis = new TreeSet<>();
            CLDRFile resolvedCldrFile = cldrFactory.make(locale, true);
            Map<String, String> results = getPathValuesToAdd(resolvedCldrFile, basis);
            if (DEBUG) System.out.println("Results");
            Set<PathHeader> paths = new TreeSet<>(FIXED_PH);
            basis.stream().forEach(x -> paths.add(phf.fromPath(x.toString())));
            results.keySet().stream().forEach(x -> paths.add(phf.fromPath(x)));

            Set<String> toPrint = new LinkedHashSet<>();
            Output<Status> maxDifference = new Output<>(Status.notGenerated);
            paths.stream()
                    .forEach(
                            ph -> {
                                String path = ph.getOriginalPath();
                                XPathParts parts = XPathParts.getFrozenInstance(path);
                                String oldValue = resolvedCldrFile.getStringValue(path);
                                String newValue = results.get(path);
                                boolean newIsNull = newValue == null;
                                if (newIsNull) {
                                    newValue = "";
                                }
                                String eOldValue = escape(oldValue);
                                String eNewValue = escape(newValue);

                                String numberSystem = parts.getAttributeValue(2, "numberSystem");

                                Status status = getStatus(oldValue, newValue, newIsNull);
                                if (status.compareTo(maxDifference.value) > 0) {
                                    maxDifference.value = status;
                                }
                                PathHeader ph2 = phf.fromPath(path);
                                boolean isNoCurrencyCompact =
                                        ph2.getHeader().equals("Short Formats ");
                                String modCode =
                                        isNoCurrencyCompact
                                                ? ph2.getCode() + " (noCurrency)"
                                                : ph2.getCode();
                                toPrint.add(
                                        Joiners.TAB.join(
                                                locale,
                                                numberSystem,
                                                modCode,
                                                status.symbol,
                                                oldValue,
                                                showEquals(oldValue, newValue),
                                                eOldValue,
                                                eNewValue));
                            });
            switch (maxDifference.value) {
                case differentOther:
                    toPrint.stream().forEach(System.out::println);
                    break;
                case differentOnlyWhitespace:
                    wsDifference.add(locale);
                    break;
                case same:
                    noDifference.add(locale);
                    break;
            }
        }
        System.out.println("NoDiff:\t" + noDifference);
        System.out.println("WSODiff:\t" + wsDifference);
    }

    static final UnicodeSet EXCLUDE_NBSP =
            new UnicodeSet(CodePointEscaper.FORCE_ESCAPE)
                    .remove(CodePointEscaper.NBSP.getCodePoint())
                    .freeze();

    private static String escape(String oldValue) {
        String result = CodePointEscaper.toEscaped(oldValue, EXCLUDE_NBSP);
        return showEquals(oldValue, result);
    }

    private static String showEquals(String a, String b) {
        return b.equals(a) ? "🟰" : b;
    }

    enum Status {
        notGenerated("NG"),
        same(""),
        differentOnlyWhitespace("␣"),
        differentOther("≠");
        final String symbol;

        private Status(String symbol) {
            this.symbol = symbol;
        }
    }

    private static Status getStatus(String oldValue, String newValue, boolean newIsNull) {
        return newIsNull
                ? Status.notGenerated
                : Objects.equal(oldValue, newValue)
                        ? Status.same
                        : Objects.equal(
                                        Patterns.WSC.matcher(oldValue).replaceAll(""),
                                        Patterns.WSC.matcher(newValue).replaceAll(""))
                                ? Status.differentOnlyWhitespace
                                : Status.differentOther;
    }

    static Comparator<PathHeader> FIXED_PH =
            new Comparator<>() {
                @Override
                public int compare(PathHeader o1, PathHeader o2) {
                    o1.getPageId();
                    if (o1.getPageId() == PageId.Compact_Decimal_Formatting
                            && o1.getPageId() == o2.getPageId()) {
                        // .... pattern[@type="1000"][@count="one"]
                        String type1 =
                                XPathParts.getFrozenInstance(o1.getOriginalPath())
                                        .getAttributeValue(-1, "type");
                        String type2 =
                                XPathParts.getFrozenInstance(o2.getOriginalPath())
                                        .getAttributeValue(-1, "type");
                        int diff = Integer.compare(type1.length(), type2.length());
                        if (diff != 0) {
                            return diff;
                        }
                    }
                    return o1.compareTo(o2);
                }
            };

    static final Map<String, String> getPathValuesToAdd(
            CLDRFile resolvedCldrFile, Set<XPathParts> basis) {
        if (!resolvedCldrFile.isResolved()) {
            throw new IllegalArgumentException();
        }
        CLDRFile unresolvedCldrFile = resolvedCldrFile.getUnresolved();

        // first gather the data to use

        // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength/currencyFormat[@type="standard"]/pattern[@type="standard"]
        // to
        // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength/currencyFormat[@type="standard"]/pattern[@type="standard"][@alt="alphaNextToNumber"]
        // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength/currencyFormat[@type="standard"]/pattern[@type="standard"][@alt="noCurrency"]

        // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength/currencyFormat[@type="accounting"]/pattern[@type="standard"]
        // to
        // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength/currencyFormat[@type="accounting"]/pattern[@type="standard"][@alt="alphaNextToNumber"]
        // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength/currencyFormat[@type="accounting"]/pattern[@type="standard"][@alt="alphaNextToNumber"]

        Map<String, CurrencyData> numberSystemToCurrencyData = new TreeMap<>();

        // we only get the unresolved paths, so we don't get all the aliased number systems, etc.

        for (String path : unresolvedCldrFile.iterableWithoutExtras()) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (parts.size() < 6
                    || !parts.getElement(-1).equals("pattern")
                    || !parts.getElement(1).equals("numbers")) {
                continue;
            }
            String numberSystem = parts.getAttributeValue(2, "numberSystem");
            // String patternType = parts.getAttributeValue(-1, "type");
            String formatType = parts.getAttributeValue(-2, "type");
            String length = parts.getAttributeValue(3, "type");
            String format = parts.getElement(4);

            if (length == null) { // main stuff
                String alt = parts.getAttributeValue(-1, "alt");
                if (alt != null || !format.equals("currencyFormat")) {
                    continue;
                }
                CurrencyData currencyData =
                        CurrencyData.get(numberSystemToCurrencyData, numberSystem);
                switch (formatType) {
                    case "standard":
                        currencyData.setStandardPath(parts);
                        break;
                    case "accounting":
                        currencyData.setAccountingPath(parts);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            } else if (length.equals("short") && format.equals("decimalFormat")) {
                CurrencyData currencyData =
                        CurrencyData.get(numberSystemToCurrencyData, numberSystem);
                currencyData.addToCompactPaths(parts);
            }
        }
        if (numberSystemToCurrencyData.isEmpty()) {
            return Map.of();
        }

        if (DEBUG) {
            System.out.println("Unresolved file paths");
            numberSystemToCurrencyData.entrySet().stream()
                    .forEach(x -> System.out.println(valuesFor(x, unresolvedCldrFile)));
        }

        Map<String, String> result = new TreeMap<>();

        // process; using the resolved values

        for (Entry<String, CurrencyData> entry : numberSystemToCurrencyData.entrySet()) {
            String numberSystem = entry.getKey();
            CurrencyData currencyData = entry.getValue();

            // Handle strange case, where one of the patterns in currencyData is fleshed out, and
            // the other isn't.
            if (currencyData.standardPath == null) {
                currencyData.setStandardPath(numberSystem);
            } else if (currencyData.accountingPath == null) {
                currencyData.setAccountingPath(numberSystem);
            }

            final XPathParts standardPath = currencyData.getStandardPath();
            PartsForCompact partsForCompact = new PartsForCompact();
            if (standardPath != null) {
                basis.add(standardPath);
                final String standardValue =
                        resolvedCldrFile.getStringValue(standardPath.toString());
                if (DEBUG) {
                    System.out.println("standardValue " + standardValue);
                }
                addPathValue(result, standardPath, standardValue, partsForCompact);
            }
            if (partsForCompact.currencyPattern == null) {
                throw new IllegalArgumentException(
                        Joiners.TAB.join(
                                "Failed to find currency pattern: ",
                                resolvedCldrFile.getLocaleID(),
                                entry.getKey(),
                                entry.getValue()));
            }
            final XPathParts accountingPath = currencyData.getAccountingPath();
            if (accountingPath != null) {
                basis.add(accountingPath);
                final String accountingValue =
                        resolvedCldrFile.getStringValue(accountingPath.toString());
                if (DEBUG) {
                    System.out.println("accountingValue " + accountingValue);
                }
                addPathValue(result, accountingPath, accountingValue, null);
            }

            for (XPathParts shortCompactPath : currencyData.compactPaths) {
                basis.add(shortCompactPath);
                final String shortCompactValue =
                        resolvedCldrFile.getStringValue(shortCompactPath.toString());
                if (DEBUG) {
                    System.out.println("shortCompactPath " + shortCompactPath);
                }
                XPathParts modPath = shortCompactPath.cloneAsThawed();
                // ldml/numbers/decimalFormats[@numberSystem="latn"]/decimalFormatLength[@type="short"]/decimalFormat[@type="standard"]/pattern[@type="1000"][@count="one"]
                // to
                // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="1000"][@count="one"]
                // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="1000"][@count="one"][@alt="alphaNextToNumber"]

                modPath =
                        modPath.setElement(2, "currencyFormats")
                                .setElement(3, "currencyFormatLength")
                                .setElement(4, "currencyFormat");
                result.put(
                        modPath.toString(),
                        shortCompactValue.equals("0")
                                ? "0"
                                : partsForCompact.currencyPattern.replace(
                                        "{0}", shortCompactValue));

                modPath.setAttribute(-1, "alt", "alphaNextToNumber");
                result.put(
                        modPath.toString(),
                        shortCompactValue.equals("0")
                                ? "0"
                                : partsForCompact.currencyAlphaPattern.replace(
                                        "{0}", shortCompactValue));
            }
        }
        return result;
    }

    private static String valuesFor(Entry<String, CurrencyData> x, CLDRFile unresolvedCldrFile) {
        return Joiners.N.join(x.getKey(), x.getValue().toStringWithValue(unresolvedCldrFile));
    }

    private static class PartsForCompact {
        String currencyPattern;
        String currencyAlphaPattern;

        @Override
        public String toString() {
            return Joiners.COMMA_SP.join(
                    "currencyPattern",
                    currencyPattern,
                    "currencyAlphaPattern",
                    currencyAlphaPattern);
        }
    }

    private static void addPathValue(
            Map<String, String> result,
            final XPathParts standardOrAccountingPath,
            final String value,
            PartsForCompact partsForCompact) {
        Matcher matcher = Patterns.CURRENCY_PLACEHOLDER_AND_POSSIBLE_WS.matcher(value);
        String noCurrencyPattern = matcher.replaceAll("");
        XPathParts modPath =
                standardOrAccountingPath.cloneAsThawed().setAttribute(-1, "alt", "noCurrency");
        result.put(modPath.toString(), noCurrencyPattern);

        List<String> valuePieces = Splitters.SEMI.splitToList(value);
        List<String> alphaPieces = new ArrayList<>();

        int patternNumber = -1;
        for (String valueN : valuePieces) {
            patternNumber++;
            boolean fixed = false;
            matcher = Patterns.NUMBER_PATTERN.matcher(valueN);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        RegexUtilities.showMismatch(Patterns.NUMBER_PATTERN, valueN)
                                + "\t\t"
                                + Patterns.NUMBER_PATTERN);
            }
            // EXAMPLE: <FILLER1><CURRENCY2><FILLER3><NUMBER_PATTERN4><FILLER5><CURRENCY6><FILLER7>
            String f1 = matcher.group(1);
            String cur2 = matcher.group(2);
            String f3 = matcher.group(3);
            String np4 = matcher.group(4);
            String f5 = matcher.group(5);
            String cur6 = matcher.group(6);
            String f7 = matcher.group(7);
            boolean hasCur2 = cur2 != null;
            boolean hasCur6 = cur6 != null;
            if (hasCur2 == hasCur6) { // failure, must have exactly 1
                throw new IllegalArgumentException();
            }
            if (partsForCompact != null && patternNumber == 0) {
                partsForCompact.currencyPattern =
                        Joiners.ES_BLANK_NULLS.join(f1, cur2, f3, "{0}", f5, cur6, f7);
            }
            // see if we need to add a space to the currency pattern
            if (hasCur2) {
                if (f3.isEmpty()) {
                    f3 = " ";
                    fixed = true;
                }
            } else { // cur6, so
                if (f5.isEmpty()) {
                    f5 = " ";
                    fixed = true;
                }
            }
            if (partsForCompact != null && patternNumber == 0) {
                partsForCompact.currencyAlphaPattern =
                        Joiners.ES_BLANK_NULLS.join(f1, cur2, f3, "{0}", f5, cur6, f7);
            }

            if (fixed) {
                valueN = Joiners.ES_BLANK_NULLS.join(f1, cur2, f3, np4, f5, cur6, f7);
            }
            alphaPieces.add(valueN);
        }
        String alpha =
                alphaPieces.size() == 1 || alphaPieces.get(1).equals("-" + alphaPieces.get(0))
                        ? alphaPieces.get(0)
                        : alphaPieces.get(0) + ";" + alphaPieces.get(1);
        modPath = modPath.setAttribute(-1, "alt", "alphaNextToNumber");
        result.put(modPath.toString(), alpha);
    }

    static final class CurrencyData {
        enum Type {
            standard,
            accounting
        }

        public XPathParts getStandardPath() {
            return standardPath;
        }

        public void setStandardPath(String numberSystem) {
            setStandardPath(
                    XPathParts.getFrozenInstance(
                            CURRENCY_FORMAT.format(numberSystem, Type.standard.toString())));
        }

        public void setAccountingPath(String numberSystem) {
            setAccountingPath(
                    XPathParts.getFrozenInstance(
                            CURRENCY_FORMAT.format(numberSystem, Type.accounting.toString())));
        }

        public void setStandardPath(XPathParts standardPath) {
            if (this.standardPath != null) {
                throw new IllegalArgumentException();
            }
            this.standardPath = standardPath;
        }

        public XPathParts getAccountingPath() {
            return accountingPath;
        }

        public void setAccountingPath(XPathParts accountingPath) {
            if (this.accountingPath != null) {
                throw new IllegalArgumentException();
            }
            this.accountingPath = accountingPath;
        }

        public Set<XPathParts> getCompactPaths() {
            return compactPaths;
        }

        public void addToCompactPaths(XPathParts compactPath) {
            this.compactPaths.add(compactPath);
        }

        XPathParts standardPath;
        XPathParts accountingPath;
        Set<XPathParts> compactPaths = new TreeSet<>();

        @Override
        public String toString() {
            return Joiners.N.join(
                    "standardPath",
                    standardPath,
                    "accountingPath",
                    accountingPath,
                    "compactPaths",
                    compactPaths.size(),
                    Joiners.N.join(compactPaths));
        }

        public String toStringWithValue(CLDRFile cldrFile) {
            return Joiners.N.join(
                    Joiners.TAB.join(
                            "standardPath",
                            standardPath,
                            cldrFile.getStringValue(standardPath.toString())),
                    Joiners.TAB.join(
                            "accountingPath",
                            accountingPath,
                            cldrFile.getStringValue(accountingPath.toString())),
                    Joiners.TAB.join("compactPaths", compactPaths.size()),
                    compactPaths.stream()
                            .map(x -> Joiners.TAB.join(x, cldrFile.getStringValue(x.toString())))
                            .collect(Collectors.joining("\n")));
        }

        private static CurrencyData get(
                Map<String, CurrencyData> numberSystemToCurrencyData, String numberSystem) {
            CurrencyData result = numberSystemToCurrencyData.get(numberSystem);
            if (result == null) {
                numberSystemToCurrencyData.put(numberSystem, result = new CurrencyData());
            }
            return result;
        }
    }
}
