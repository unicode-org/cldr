package org.unicode.cldr.util;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.RegexLookup.Finder;

public class PathDescription {
    /** Remember to quote any [ character! */
    public static final String pathDescriptionFileName = "PathDescriptions.md";

    public static final String pathDescriptionHintsFileName = "PathDescriptionHints.md";

    private static final String pathDescriptionString =
            CldrUtility.getUTF8Data(pathDescriptionFileName)
                    .lines()
                    .collect(Collectors.joining("\n"));

    private static final String pathDescriptionHintsString =
            CldrUtility.getUTF8Data(pathDescriptionHintsFileName)
                    .lines()
                    .collect(Collectors.joining("\n"));

    private static final Logger logger = Logger.getLogger(PathDescription.class.getName());

    public enum ErrorHandling {
        SKIP,
        CONTINUE
    }

    public static final Set<String> EXTRA_LANGUAGES =
            new TreeSet<>(
                    Arrays.asList(
                            "ach|af|ak|ak|am|ar|az|be|bem|bg|bh|bn|br|bs|ca|chr|ckb|co|crs|cs|cy|da|de|de_AT|de_CH|ee|el|en|en_AU|en_CA|en_GB|en_US|eo|es|es_419|es_ES|et|eu|fa|fi|fil|fo|fr|fr_CA|fr_CH|fy|ga|gaa|gd|gl|gn|gsw|gu|ha|haw|he|hi|hr|ht|hu|hy|ia|id|ig|io|is|it|ja|jv|ka|kg|kk|km|kn|ko|kri|ku|ky|la|lg|ln|lo|loz|lt|lua|lv|mfe|mg|mi|mk|ml|mn|mr|ms|mt|my|nb|ne|nl|nl_BE|nn|no|nso|ny|nyn|oc|om|or|pa|pcm|pl|ps|pt|pt_BR|pt_PT|qu|rm|rn|ro|ro|ro_MD|ru|rw|sd|si|sk|sl|sn|so|sq|sr|sr_Latn|sr_ME|st|su|sv|sw|ta|te|tg|th|ti|tk|tlh|tn|to|tr|tt|tum|ug|uk|und|ur|uz|vi|wo|xh|yi|yo|zh|zh_Hans|zh_Hant|zh_HK|zu|zxx"
                                    .split("\\|")));

    private static final Pattern METAZONE_PATTERN =
            Pattern.compile("//ldml/dates/timeZoneNames/metazone\\[@type=\"([^\"]*)\"]/(.*)/(.*)");
    private static final Pattern STAR_ATTRIBUTE_PATTERN = PatternCache.get("=\"([^\"]*)\"");

    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
    private static final Map<String, String> ZONE2COUNTRY =
            STANDARD_CODES.zoneParser.getZoneToCountry();

    /** <Description, Markdown> */
    private static final PathDescriptionParser parser = new PathDescriptionParser();

    private static final PathDescriptionParser hintsParser = new PathDescriptionParser();

    private static RegexLookup<Pair<String, String>> pathHandling = null;

    private static RegexLookup<Pair<String, String>> pathHintsHandling = null;

    /** markdown to append */
    private static final String references = parser.getReferences();

    /** for tests, returns the big string */
    static String getBigString(String fileName) {
        switch (fileName) {
            case pathDescriptionFileName:
                return pathDescriptionString;
            case pathDescriptionHintsFileName:
                return pathDescriptionHintsString;
            default:
                throw new IllegalArgumentException();
        }
    }

    /** for tests */
    public static RegexLookup<Pair<String, String>> getPathHandling() {
        if (pathHandling == null) {
            pathHandling = parser.parse(pathDescriptionFileName);
        }
        return pathHandling;
    }

    private static RegexLookup<Pair<String, String>> getPathHintsHandling() {
        if (pathHintsHandling == null) {
            pathHintsHandling = hintsParser.parse(pathDescriptionHintsFileName);
        }
        return pathHintsHandling;
    }

    // set in construction

    private final CLDRFile english;
    private final Map<String, String> extras;
    private final ErrorHandling errorHandling;
    private final Map<String, List<Set<String>>> starredPaths;
    private final Set<String> allMetazones;

    // used on instance

    private final Matcher metazoneMatcher = METAZONE_PATTERN.matcher("");
    private final Output<String[]> pathArguments = new Output<>();
    private final EnumSet<Status> status = EnumSet.noneOf(Status.class);

    public static final String MISSING_DESCRIPTION =
            "Before translating, please see " + CLDRURLS.GENERAL_HELP_URL + ".";

    public PathDescription(
            SupplementalDataInfo supplementalDataInfo,
            CLDRFile english,
            Map<String, String> extras,
            Map<String, List<Set<String>>> starredPaths,
            ErrorHandling errorHandling) {
        this.english = english;
        this.extras = extras == null ? new HashMap<>() : extras;
        this.starredPaths = starredPaths == null ? new HashMap<>() : starredPaths;
        allMetazones = supplementalDataInfo.getAllMetazones();
        this.errorHandling = errorHandling;
    }

    public EnumSet<Status> getStatus() {
        return status;
    }

    public enum Status {
        SKIP,
        NULL_VALUE,
        EMPTY_CONTENT,
        NOT_REQUIRED
    }

    public String getRawDescription(String path, Object context) {
        status.clear();
        final Pair<String, String> entry = getPathHandling().get(path, context, pathArguments);
        if (entry == null) {
            return null;
        }
        return entry.getSecond();
    }

    public String getHintRawDescription(String path, Object context) {

        status.clear();
        final Pair<String, String> entry = getPathHintsHandling().get(path, context, pathArguments);
        if (entry == null) {
            return null;
        }
        return entry.getSecond();
    }

    public String getRawDescription(
            String path, Object context, Output<Finder> matcherFound, Set<String> failures) {
        status.clear();
        final Pair<String, String> entry =
                getPathHandling().get(path, context, pathArguments, matcherFound, failures);
        if (entry == null) {
            return null;
        }
        return entry.getSecond();
    }

    public String getDescription(String path, String value, Object context) {
        status.clear();

        final Pair<String, String> entry = getPathHandling().get(path, context, pathArguments);
        String description;
        String markdown;
        if (entry == null) {
            markdown = MISSING_DESCRIPTION;
            description = null;
        } else {
            description = entry.getFirst();
            markdown = entry.getSecond();
        }

        if (description == null || description.isEmpty()) {
            description = MISSING_DESCRIPTION;
        } else if (description.startsWith("SKIP")) {
            status.add(Status.SKIP);
            if (errorHandling == ErrorHandling.SKIP) {
                return null;
            }
        }
        if (value == null) { // a count item?
            String xpath = extras.get(path);
            if (xpath != null) {
                value = english.getStringValue(xpath);
            } else if (path.contains("/metazone")) {
                if (metazoneMatcher.reset(path).matches()) {
                    String name = metazoneMatcher.group(1);
                    String type = metazoneMatcher.group(3);
                    value =
                            name.replace('_', ' ')
                                    + (type.equals("generic")
                                            ? ""
                                            : type.equals("daylight") ? " Summer" : " Winter")
                                    + " Time";
                }
            }
            if (value == null) {
                status.add(Status.NULL_VALUE);
                if (errorHandling == ErrorHandling.SKIP) {
                    return null;
                }
            }
        }
        if (value != null && value.isEmpty()) {
            status.add(Status.EMPTY_CONTENT);
            if (errorHandling == ErrorHandling.SKIP) {
                return null;
            }
        }

        List<String> attributes = addStarredInfo(starredPaths, path);

        // In special cases, only use if there is a root value (languageNames, ...
        if (description.startsWith("ROOT")) {
            String type = description.substring(4).trim();

            boolean isMetazone = type.equals("metazone");
            String code = attributes.get(0);
            boolean isRootCode = isRootCode(code, allMetazones, type, isMetazone);
            if (!isRootCode) {
                status.add(Status.NOT_REQUIRED);
                if (errorHandling == ErrorHandling.SKIP) {
                    return null;
                }
            }
            if (isMetazone) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String daylightType = parts.getElement(-1);
                daylightType =
                        daylightType.equals("daylight")
                                ? "summer"
                                : daylightType.equals("standard") ? "winter" : daylightType;
                String length = parts.getElement(-2);
                length = length.equals("long") ? "" : "abbreviated ";
                code = code + ", " + length + daylightType + " form";
            } else if (type.equals("timezone")) {
                String country = ZONE2COUNTRY.get(code);
                int lastSlash = code.lastIndexOf('/');
                String codeName =
                        lastSlash < 0 ? code : code.substring(lastSlash + 1).replace('_', ' ');

                boolean found = false;
                if ("001".equals(country)) {
                    code = "the timezone “" + codeName + "”";
                    found = true;
                } else if (country != null) {
                    String countryName =
                            english.nameGetter()
                                    .getNameFromTypeEnumCode(NameType.TERRITORY, country);
                    if (countryName != null) {
                        if (!codeName.equals(countryName)) {
                            code = "the city “" + codeName + "” (in " + countryName + ")";
                        } else {
                            code = "the country “" + codeName + "”";
                        }
                        found = true;
                    }
                }
                if (!found) {
                    logger.warning("Missing country for timezone " + code);
                }
            }
            markdown = MessageFormat.format(MessageFormat.autoQuoteApostrophe(markdown), code);
        } else if (path.contains("exemplarCity")) {
            String regionCode = ZONE2COUNTRY.get(attributes.get(0));
            String englishRegionName =
                    english.nameGetter().getNameFromTypeEnumCode(NameType.TERRITORY, regionCode);
            markdown =
                    MessageFormat.format(
                            MessageFormat.autoQuoteApostrophe(markdown), englishRegionName);
        } else if (entry != null) {
            markdown =
                    MessageFormat.format(
                            MessageFormat.autoQuoteApostrophe(markdown),
                            (Object[]) pathArguments.value);
        }

        // we always append the "References" blob
        return markdown + "\n" + references;
    }

    private static boolean isRootCode(
            String code, Set<String> allMetazones, String type, boolean isMetazone) {
        Set<String> codes =
                isMetazone
                        ? allMetazones
                        : type.equals("timezone")
                                ? STANDARD_CODES.zoneParser.getZoneData().keySet()
                                : STANDARD_CODES.getSurveyToolDisplayCodes(type);
        // end
        boolean isRootCode = codes.contains(code) || code.contains("_");
        if (!isRootCode && type.equals("language") && EXTRA_LANGUAGES.contains(code)) {
            isRootCode = true;
        }
        return isRootCode;
    }

    private List<String> addStarredInfo(Map<String, List<Set<String>>> starredPaths, String path) {
        Matcher starAttributeMatcher = STAR_ATTRIBUTE_PATTERN.matcher(path);
        StringBuilder starredPath = new StringBuilder();
        List<String> attributes = new ArrayList<>();
        int lastEnd = 0;
        while (starAttributeMatcher.find()) {
            int start = starAttributeMatcher.start(1);
            int end = starAttributeMatcher.end(1);
            starredPath.append(path, lastEnd, start);
            starredPath.append(".*");

            attributes.add(path.substring(start, end));
            lastEnd = end;
        }
        starredPath.append(path.substring(lastEnd));
        String starredPathString = starredPath.toString().intern();

        List<Set<String>> attributeList =
                starredPaths.computeIfAbsent(starredPathString, k -> new ArrayList<>());
        int i = 0;
        for (String attribute : attributes) {
            if (attributeList.size() <= i) {
                TreeSet<String> subset = new TreeSet<>();
                subset.add(attribute);
                attributeList.add(subset);
            } else {
                Set<String> subset = attributeList.get(i);
                subset.add(attribute);
            }
            ++i;
        }
        return attributes;
    }
}
