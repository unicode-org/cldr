package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.GenerateXMB;
import org.unicode.cldr.util.CldrUtility.Output;

import com.ibm.icu.text.MessageFormat;

public class PathDescription {

    public enum ErrorHandling {SKIP, CONTINUE}
    public static final Set<String> EXTRA_LANGUAGES = new TreeSet<String>(Arrays.asList("ace en zh ja de ru es ko fr pl ar pt it tr nl cs id th sv vi ro nb hu fi he bg da et sk sr el lt hr lv fa sl uk ca is ms fil az sq eu ka gl hi be af mk la hy mn fy kk ta bn kn mt lb ur uz eo si km ky cy ne ku sw oc mi fo jv te ml ug pa mr ga ps gu my lo yo su tg ht am tt gd qu bo dv to sd sa iu or bho gn".split(" ")));

    private static final Pattern METAZONE_PATTERN = Pattern.compile("//ldml/dates/timeZoneNames/metazone\\[@type=\"([^\"]*)\"]/(.*)/(.*)");
    private static final Pattern STAR_ATTRIBUTE_PATTERN = Pattern.compile("=\"([^\"]*)\"");

    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
    private static Map<String, String> ZONE2COUNTRY = STANDARD_CODES.getZoneToCounty();
    private static RegexLookup<String> pathHandling = new RegexLookup<String>().loadFromFile(GenerateXMB.class, "xmbHandling.txt");

    // set in construction

    private final CLDRFile english;
    private final Map<String, String> extras;
    private final ErrorHandling errorHandling;
    private final Map<String, List<Set<String>>> starredPaths;
    private final Set<String> allMetazones;

    // used on instance

    private Matcher metazoneMatcher = METAZONE_PATTERN.matcher("");
    private XPathParts parts = new XPathParts();
    private String starredPathOutput;
    private Output<String[]> pathArguments = new Output<String[]>();
    private EnumSet<Status> status = EnumSet.noneOf(Status.class);

    public static final String MISSING_DESCRIPTION = "Before translating, please see cldr.org/translation.";

    public PathDescription(SupplementalDataInfo supplementalDataInfo, 
            CLDRFile english, 
            Map<String, String> extras, 
            Map<String, List<Set<String>>> starredPaths, 
            ErrorHandling errorHandling) {
        this.english = english;
        this.extras = extras == null ? new HashMap<String, String>() : extras;
        this.starredPaths = starredPaths == null ? new HashMap<String, List<Set<String>>>() : starredPaths;
        allMetazones = supplementalDataInfo.getAllMetazones();
        this.errorHandling = errorHandling;
    }

    public String getStarredPathOutput() {
        return starredPathOutput;
    }

    public EnumSet<Status> getStatus() {
        return status;
    }

    public enum Status {SKIP, NULL_VALUE, EMPTY_CONTENT, NOT_REQUIRED}

    public String getDescription(String path, String value, Level level, Object context) {
        status.clear();

        String description = pathHandling.get(path, context, pathArguments);
        if (description == null) {
            description = MISSING_DESCRIPTION;
        } else if ("SKIP".equals(description)) {
            status.add(Status.SKIP);
            if (errorHandling == ErrorHandling.SKIP) {
                return null;
            }
        }

        //                String localeWhereFound = english.getSourceLocaleID(path, status);
        //                if (!status.pathWhereFound.equals(path)) {
        //                    reasonsToPaths.put("alias", path + "  " + value);
        //                    continue;
        //                }
        if (value == null) { // a count item?
            String xpath = extras.get(path);
            if (xpath != null) {
                value = english.getStringValue(xpath);
            } else {
                if (path.contains("/metazone")) {
                    if (metazoneMatcher.reset(path).matches()) {
                        String name = metazoneMatcher.group(1);
                        String type = metazoneMatcher.group(3);
                        value = name.replace('_', ' ') + (type.equals("generic") ? "" : type.equals("daylight") ? " Summer" : " Winter") + " Time";
                        // System.out.println("Missing:    " + path + " :    " + value);
                    }
                }
            }
            if (value == null) {
                status.add(Status.NULL_VALUE);
                return null;
            }
        }
        if (value.length() == 0) {
            status.add(Status.EMPTY_CONTENT);
            if (errorHandling == ErrorHandling.SKIP) {
                return null;
            }
        }
        //        if (GenerateXMB.contentMatcher != null && !GenerateXMB.contentMatcher.reset(value).find()) {
        //            PathDescription.addSkipReasons(reasonsToPaths, "content-parameter", level, path, value);
        //            return null;
        //        }

        List<String> attributes = addStarredInfo(starredPaths, path);

        // In special cases, only use if there is a root value (languageNames, ...
        if (description.startsWith("ROOT")) {
            int typeEnd = description.indexOf(';');
            String type = description.substring(4,typeEnd).trim();
            description = description.substring(typeEnd+1).trim();

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
                parts.set(path);
                String daylightType = parts.getElement(-1);
                daylightType =  daylightType.equals("daylight") ? "summer" : daylightType.equals("standard") ? "winter" : daylightType;
                String length = parts.getElement(-2);
                length = length.equals("long") ? "" : "abbreviated ";
                code = code + ", " + length + daylightType + " form";
            } else if (type.equals("timezone")) {
                String country = (String) ZONE2COUNTRY.get(code);
                int lastSlash = code.lastIndexOf('/');
                String codeName = lastSlash< 0 ? code : code.substring(lastSlash+1).replace('_', ' ');

                boolean found = false;
                if ("001".equals(country)) {
                    code = "the timezone \"" + codeName + '"';
                    found = true;
                } else if (country != null) {
                    String countryName = english.getName("territory", country);
                    if (countryName != null) {
                        if (!codeName.equals(countryName)) {
                            code = "the city \"" + codeName + "\" (in " + countryName + ")";
                        } else {
                            code = "the country \"" + codeName + '"';
                        }
                        found = true;
                    }
                }
                if (!found) {
                    System.out.println("Missing country for timezone " + code);
                }
            }
            description = MessageFormat.format(MessageFormat.autoQuoteApostrophe(description), new Object[]{code});
        } else if (path.contains("exemplarCity")) {
            String regionCode = ZONE2COUNTRY.get(attributes.get(0));
            String englishRegionName = english.getName(CLDRFile.TERRITORY_NAME, regionCode);
            description = MessageFormat.format(MessageFormat.autoQuoteApostrophe(description), new Object[]{englishRegionName});
        } else if (description != MISSING_DESCRIPTION){
            description = MessageFormat.format(MessageFormat.autoQuoteApostrophe(description), pathArguments.value);
        }
        return description;
    }

    private static boolean isRootCode(String code, Set<String> allMetazones, String type, boolean isMetazone) {
        Set<String> codes = isMetazone ? allMetazones  
                : type.equals("timezone") ? STANDARD_CODES.getCanonicalTimeZones() 
                        : STANDARD_CODES.getSurveyToolDisplayCodes(type); 
                // end
                boolean isRootCode = codes.contains(code) || code.contains("_");
                if (!isRootCode && type.equals("language") 
                        && EXTRA_LANGUAGES.contains(code)) {
                    isRootCode = true;
                }
                return isRootCode;
    }

    private List<String> addStarredInfo(Map<String, List<Set<String>>> starredPaths, String path) {
        Matcher starAttributeMatcher = STAR_ATTRIBUTE_PATTERN.matcher(path);
        StringBuilder starredPath = new StringBuilder();
        List<String> attributes = new ArrayList<String>();
        int lastEnd = 0;
        while (starAttributeMatcher.find()) {
            int start = starAttributeMatcher.start(1);
            int end = starAttributeMatcher.end(1);
            starredPath.append(path.substring(lastEnd, start));
            starredPath.append(".*");

            attributes.add(path.substring(start, end));
            lastEnd = end;
        }
        starredPath.append(path.substring(lastEnd));
        String starredPathString = starredPath.toString().intern();
        starredPathOutput = starredPathString;

        List<Set<String>> attributeList = starredPaths.get(starredPathString);
        if (attributeList == null) {
            starredPaths.put(starredPathString, attributeList = new ArrayList<Set<String>>());
        }
        int i = 0;
        for (String attribute : attributes) {
            if (attributeList.size() <= i) {
                TreeSet<String> subset = new TreeSet<String>();
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