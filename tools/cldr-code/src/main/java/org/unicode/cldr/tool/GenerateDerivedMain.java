package org.unicode.cldr.tool;

import com.google.common.base.Objects;
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
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
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final boolean DEBUG = false;
    static Factory cldrFactory = CLDR_CONFIG.getCldrFactory();
    static PathHeader.Factory phf = PathHeader.getFactory();

    public static void main(String[] args) {
        // later move to test
        System.out.println(
                Joiners.TAB.join(
                        "locale",
                        "section",
                        "page",
                        "header",
                        "code",
                        "oldValue",
                        "newValue",
                        "status"));

        Map<String, Level> items = StandardCodes.make().getLocalesToLevelsFor(Organization.cldr);
        Set<String> noDifference = new LinkedHashSet<>();
        for (Entry<String, Level> item : items.entrySet()) {
            String locale = item.getKey();
            if (DEBUG) System.out.println("\nLocale " + locale);
            Set<XPathParts> basis = new TreeSet<>();
            CLDRFile resolvedCldrFile = cldrFactory.make(locale, true);
            Map<String, String> results = getPathValuesToAdd(resolvedCldrFile, basis);
            if (DEBUG) System.out.println("Results");
            Set<PathHeader> paths = new TreeSet<>(FIXED_PH);
            basis.stream().forEach(x -> paths.add(phf.fromPath(x.toString())));
            results.keySet().stream().forEach(x -> paths.add(phf.fromPath(x)));

            Set<String> toPrint = new LinkedHashSet<>();
            Output<Boolean> foundDifference = new Output<>(Boolean.FALSE);
            paths.stream()
                    .forEach(
                            ph -> {
                                String path = ph.getOriginalPath();
                                String oldValue = resolvedCldrFile.getStringValue(path);
                                String newValue = results.get(path);
                                boolean newIsNull = newValue == null;
                                if (newIsNull) {
                                    newValue = oldValue;
                                }
                                String status = getStatus(oldValue, newValue, newIsNull);
                                if (!newIsNull && !status.isEmpty()) {
                                    foundDifference.value = Boolean.TRUE;
                                }
                                toPrint.add(
                                        Joiners.TAB.join(
                                                locale,
                                                phf.fromPath(path),
                                                oldValue,
                                                newValue,
                                                status));
                            });
            if (foundDifference.value) {
                toPrint.stream().forEach(System.out::println);
            }
        }
        System.out.println("NoDiff:\t" + noDifference);
    }

    private static @Nullable String getStatus(String oldValue, String newValue, boolean newIsNull) {
        return newIsNull
                ? "NG"
                : Objects.equal(oldValue, newValue)
                        ? ""
                        : Objects.equal(
                                        Patterns.WS.matcher(oldValue).replaceAll(" "),
                                        Patterns.WS.matcher(newValue).replaceAll(" "))
                                ? "≠WS"
                                : Objects.equal(
                                                Patterns.WSC.matcher(oldValue).replaceAll(""),
                                                Patterns.WSC.matcher(newValue).replaceAll(""))
                                        ? "≠WSC"
                                        : "≠";
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

    static final Map<String, String> getPathValuesToAdd(CLDRFile resolvedCldrFile, Set<XPathParts> basis) {
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

        // we get the unresolved paths, so we don't get all the aliased number systems, etc.
        
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
            numberSystemToCurrencyData.entrySet().stream().forEach(System.out::println);
        }

        Map<String, String> result = new TreeMap<>();

        // process; using the resolved values

        for (Entry<String, CurrencyData> entry : numberSystemToCurrencyData.entrySet()) {
            CurrencyData currencyData = entry.getValue();

            final XPathParts standardPath = currencyData.getStandardPath();
            PartsForCompact partsForCompact = new PartsForCompact();
            if (standardPath != null) {
                basis.add(standardPath);
                final String standardValue = resolvedCldrFile.getStringValue(standardPath.toString());
                if (DEBUG) {
                    System.out.println("standardValue " + standardValue);
                }
                addPathValue(result, standardPath, standardValue, partsForCompact);
            }

            final XPathParts accountingPath = currencyData.getAccountingPath();
            if (accountingPath != null) {
                basis.add(accountingPath);
                final String accountingValue = resolvedCldrFile.getStringValue(accountingPath.toString());
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
                        partsForCompact.currencyPattern.replace("{0}", shortCompactValue));

                modPath.setAttribute(-1, "alt", "alphaNextToNumber");
                result.put(
                        modPath.toString(),
                        partsForCompact.currencyAlphaPattern.replace("{0}", shortCompactValue));
            }
        }
        return result;
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
        public XPathParts getStandardPath() {
            return standardPath;
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
