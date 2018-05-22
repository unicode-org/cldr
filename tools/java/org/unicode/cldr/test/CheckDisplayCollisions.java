package org.unicode.cldr.test;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

public class CheckDisplayCollisions extends FactoryCheckCLDR {
    private static final String DEBUG_PATH = null; // example: "//ldml/dates/fields/field[@type=\"sun-narrow\"]/relative[@type=\"-1\"]";
    /**
     * Set to true to get verbose logging of path removals
     */
    private static final boolean LOG_PATH_REMOVALS = false;

    // Get Date-Time in milliseconds
    private static long getDateTimeinMillis(int year, int month, int date) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, date);
        return cal.getTimeInMillis();
    }

    // TODO probably need to fix this to be more accurate over time
    static long year = (long) (365.2425 * 86400 * 1000); // can be approximate
    static long startDate = getDateTimeinMillis(1995, 1 - 1, 15); // can be approximate
    static long endDate = getDateTimeinMillis(2011, 1 - 1, 15); // can be approximate

    /**
     * An enum representing the types of xpaths that we don't want display collisions for.
     */
    private static enum MatchType {
        PREFIX, REGEX
    }

    private static enum Type {
        LANGUAGE("//ldml/localeDisplayNames/languages/language", MatchType.PREFIX, 0),
        SCRIPT("//ldml/localeDisplayNames/scripts/script", MatchType.PREFIX, 1),
        TERRITORY("//ldml/localeDisplayNames/territories/territory", MatchType.PREFIX, 2),
        VARIANT("//ldml/localeDisplayNames/variants/variant", MatchType.PREFIX, 3),
        CURRENCY("//ldml/numbers/currencies/currency", MatchType.PREFIX, 4),
        ZONE("//ldml/dates/timeZoneNames/zone", MatchType.PREFIX, 5),
        METAZONE("//ldml/dates/timeZoneNames/metazone", MatchType.PREFIX, 6),
        DECIMAL_FORMAT("//ldml/numbers/decimalFormats", MatchType.PREFIX, 7),
        UNITS_COMPOUND_LONG("//ldml/units/unitLength[@type=\"long\"]/compoundUnit", MatchType.PREFIX, 8),
        UNITS_COMPOUND_SHORT("//ldml/units/unitLength[@type=\"short\"]/compoundUnit", MatchType.PREFIX, 9),
        UNITS_COORDINATE( "//ldml/units/unitLength\\[@type=\".*\"\\]/coordinateUnit/", MatchType.REGEX, 10),
        UNITS_IGNORE("//ldml/units/unitLength[@type=\"narrow\"]", MatchType.PREFIX, 11),
        UNITS("//ldml/units/unitLength", MatchType.PREFIX, 12),
        FIELDS_NARROW("//ldml/dates/fields/field\\[@type=\"(sun|mon|tue|wed|thu|fri|sat)-narrow\"\\]/relative", MatchType.REGEX, 13),
        FIELDS_RELATIVE("//ldml/dates/fields/field\\[@type=\".*\"\\]/relative\\[@type=\"(-1|0|1)\"\\]", MatchType.REGEX, 14),
        ANNOTATIONS("//ldml/annotations/annotation\\[@cp=\".*\"\\]\\[@type=\"tts\"\\]", MatchType.REGEX, 15),
        CARDINAL_MINIMAL("//ldml/numbers/minimalPairs/pluralMinimalPairs", MatchType.PREFIX, 16),
        ORDINAL_MINIMAL("//ldml/numbers/minimalPairs/ordinalMinimalPairs", MatchType.PREFIX, 17), 
        ;

        private MatchType matchType;
        private String basePrefix;
        private Pattern basePattern;

        private Type(String basePrefix, MatchType matchType, int index) {
            this.matchType = matchType;
            this.basePrefix = basePrefix;
            this.basePattern = PatternCache.get("^" + basePrefix + ".*");
        }

        /**
         * @return the prefix that all XPaths of this type should start with
         */
        public String getPrefix() {
            return basePrefix;
        }

        /**
         * @return the regex that matches all XPaths of this type
         */
        public Pattern getPattern() {
            return basePattern;
        }

        /**
         * @param path
         *            the path to find the type of
         * @return the type of the path
         */
        public static Type getType(String path) {
            for (Type type : values()) {
                if (type.matchType == MatchType.PREFIX) {
                    if (path.startsWith(type.getPrefix())) {
                        return type;
                    }
                } else {
                    Matcher m = type.getPattern().matcher(path);
                    if (m.matches()) {
                        return type;
                    }
                }
            }
            return null;
        }
    }

    static final boolean SKIP_TYPE_CHECK = true;

    private final Matcher exclusions = PatternCache.get("=\"narrow\"]").matcher(""); // no matches
    private final Matcher typePattern = PatternCache.get("\\[@type=\"([^\"]*+)\"]").matcher("");
    private final Matcher ignoreAltAndCountAttributes = PatternCache.get("\\[@(?:count|alt)=\"[^\"]*+\"]").matcher("");
    private final Matcher ignoreAltAttributes = PatternCache.get("\\[@(?:alt)=\"[^\"]*+\"]").matcher("");
    private final Matcher compoundUnitPatterns = PatternCache.get("compoundUnitPattern").matcher("");

    // map unique path fragment to set of unique fragments for other
    // paths with which it is OK to have a value collision
    private static final Map<String, Set<String>> mapPathPartsToSetsForDupOK = createMapPathPartsToSets();

    private static Map<String, Set<String>> createMapPathPartsToSets() {
        Map<String, Set<String>> mapPathPartsToSets = new HashMap<String, Set<String>>();

        // Add OK collisions for /unit[@type=\"energy-calorie\"]
        Set<String> set1 = new HashSet<String>();
        set1.add("/unit[@type=\"energy-foodcalorie\"]");
        mapPathPartsToSets.put("/unit[@type=\"energy-calorie\"]", set1);

        // Add OK collisions for /unit[@type=\"energy-foodcalorie\"]
        Set<String> set2 = new HashSet<String>();
        set2.add("/unit[@type=\"energy-calorie\"]");
        set2.add("/unit[@type=\"energy-kilocalorie\"]");
        mapPathPartsToSets.put("/unit[@type=\"energy-foodcalorie\"]", set2);

        // Add OK collisions for /unit[@type=\"energy-kilocalorie\"]
        Set<String> set3 = new HashSet<String>();
        set3.add("/unit[@type=\"energy-foodcalorie\"]");
        mapPathPartsToSets.put("/unit[@type=\"energy-kilocalorie\"]", set3);

        // Add OK collisions for /unit[@type=\"mass-carat\"]
        Set<String> set4 = new HashSet<String>();
        set4.add("/unit[@type=\"concentr-karat\"]");
        mapPathPartsToSets.put("/unit[@type=\"mass-carat\"]", set4);

        // Add OK collisions for /unit[@type=\"concentr-karat\"]
        Set<String> set5 = new HashSet<String>();
        set5.add("/unit[@type=\"mass-carat\"]");
        set5.add("/unit[@type=\"temperature-kelvin\"]");
        mapPathPartsToSets.put("/unit[@type=\"concentr-karat\"]", set5);

        // Add OK collisions for /unit[@type=\"digital-byte\"]
        Set<String> set6 = new HashSet<String>();
        set6.add("/unit[@type=\"mass-metric-ton\"]");
        mapPathPartsToSets.put("/unit[@type=\"digital-byte\"]", set6);

        // Add OK collisions for /unit[@type=\"mass-metric-ton\"]
        Set<String> set7 = new HashSet<String>();
        set7.add("/unit[@type=\"digital-byte\"]");
        mapPathPartsToSets.put("/unit[@type=\"mass-metric-ton\"]", set7);

        // delete the exceptions allowing acceleration-g-force and mass-gram to have the same symbol, see #7561

        // Add OK collisions for /unit[@type=\"length-foot\"]
        Set<String> set10 = new HashSet<String>();
        set10.add("/unit[@type=\"angle-arc-minute\"]");
        mapPathPartsToSets.put("/unit[@type=\"length-foot\"]", set10);

        // Add OK collisions for /unit[@type=\"angle-arc-minute\"]
        Set<String> set11 = new HashSet<String>();
        set11.add("/unit[@type=\"length-foot\"]");
        mapPathPartsToSets.put("/unit[@type=\"angle-arc-minute\"]", set11);

        // Add OK collisions for /unit[@type=\"temperature-kelvin\"]
        Set<String> set12 = new HashSet<String>();
        set12.add("/unit[@type=\"concentr-karat\"]");
        mapPathPartsToSets.put("/unit[@type=\"temperature-kelvin\"]", set12);

        // Add OK collisions for /unit[@type=\"temperature-generic\"]
        Set<String> set13 = new HashSet<String>();
        set13.add("/unit[@type=\"angle-degree\"]");
        mapPathPartsToSets.put("/unit[@type=\"temperature-generic\"]", set13);

        // Add OK collisions for /unit[@type=\"angle-degree\"]
        Set<String> set14 = new HashSet<String>();
        set14.add("/unit[@type=\"temperature-generic\"]");
        mapPathPartsToSets.put("/unit[@type=\"angle-degree\"]", set14);

        // Add OK collisions for /unit[@type=\"length-point\"]
        Set<String> set15 = new HashSet<String>();
        set15.add("/unit[@type=\"volume-pint\"]");
        set15.add("/unit[@type=\"mass-pound\"]");
        mapPathPartsToSets.put("/unit[@type=\"length-point\"]", set15);

        // Add OK collisions for /unit[@type=\"volume-pint\"]
        Set<String> set16 = new HashSet<String>();
        set16.add("/unit[@type=\"length-point\"]");
        mapPathPartsToSets.put("/unit[@type=\"volume-pint\"]", set16);

        // Add OK collisions for /unit[@type=\"pressure-hectopascal\"]
        Set<String> set17 = new HashSet<String>();
        set17.add("/unit[@type=\"pressure-millibar\"]");
        mapPathPartsToSets.put("/unit[@type=\"pressure-hectopascal\"]", set17);

        // Add OK collisions for /unit[@type=\"pressure-millibar\"]
        Set<String> set18 = new HashSet<String>();
        set18.add("/unit[@type=\"pressure-hectopascal\"]");
        mapPathPartsToSets.put("/unit[@type=\"pressure-millibar\"]", set18);

        // Add OK collisions for /unit[@type=\"mass-pound\"]
        Set<String> set19 = new HashSet<String>();
        set19.add("/unit[@type=\"length-point\"]");
        mapPathPartsToSets.put("/unit[@type=\"mass-pound\"]", set19);

        // all done, return immutable version
        return Collections.unmodifiableMap(mapPathPartsToSets);
    }

    public CheckDisplayCollisions(Factory factory) {
        super(factory);
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have

        if (path.contains("minimalPairs")) {
            int debug = 0;
        }
        if (value == null || value.length() == 0) {
            return this;
        }
        if (value.equals(CldrUtility.NO_INHERITANCE_MARKER) || value.equals(CldrUtility.INHERITANCE_MARKER)) {
            return this;
        }

        // find my type; bail if I don't have one.
        Type myType = Type.getType(path);
        if (myType == null || myType == Type.UNITS_IGNORE) {
            return this;
        }
        String myPrefix = myType.getPrefix();

        if (exclusions.reset(path).find() && myType != Type.UNITS_COORDINATE) {
            return this;
        }

        // get the paths with the same value. If there aren't duplicates, continue;
        if (path.equals(DEBUG_PATH)) {
            int debug = 0;
        }

        Matcher matcher = null;
        String message = "Can't have same translation as {0}. Please change either this name or the other one. "
            + "See <a target='doc' href='http://cldr.unicode.org/translation/short-names-and-keywords#TOC-Unique-Names'>Unique-Names</a>.";
        Matcher currentAttributesToIgnore = ignoreAltAndCountAttributes;
        Set<String> paths;
        if (myType == Type.DECIMAL_FORMAT) {
            if (!path.contains("[@count=") || "0".equals(value)) {
                return this;
            }
            XPathParts parts = new XPathParts().set(path);
            String type = parts.getAttributeValue(-1, "type");
            myPrefix = parts.removeElement(-1).toString();
            matcher = PatternCache.get(myPrefix.replaceAll("\\[", "\\\\[") +
                "/pattern\\[@type=(?!\"" + type + "\")\"\\d+\"].*").matcher(path);
            currentAttributesToIgnore = ignoreAltAttributes;
            message = "Can't have same number pattern as {0}";
            paths = getPathsWithValue(getResolvedCldrFileToCheck(), path, value, myType, myPrefix, matcher, currentAttributesToIgnore, Equivalence.exact);
        } else if (myType == Type.UNITS) {
            paths = getPathsWithValue(getResolvedCldrFileToCheck(), path, value, myType, myPrefix, matcher, currentAttributesToIgnore, Equivalence.unit);
        } else if (myType == Type.CARDINAL_MINIMAL || myType == Type.ORDINAL_MINIMAL) {
            if (value.equals("{0}?")) {
                return this; // special root 'other' value
            }
            currentAttributesToIgnore = ignoreAltAttributes;
            paths = getPathsWithValue(getResolvedCldrFileToCheck(), path, value, myType, myPrefix, matcher, currentAttributesToIgnore, Equivalence.normal);
        } else {
            paths = getPathsWithValue(getResolvedCldrFileToCheck(), path, value, myType, myPrefix, matcher, currentAttributesToIgnore, Equivalence.normal);
        }

        // Group exemplar cities and territories together for display collisions.
        if (myType == Type.TERRITORY || myType == Type.ZONE) {
            Type otherType = myType == Type.TERRITORY ? Type.ZONE : Type.TERRITORY;
            Set<String> duplicatePaths = getPathsWithValue(
                getResolvedCldrFileToCheck(), path, value, otherType,
                otherType.getPrefix(), null, currentAttributesToIgnore, Equivalence.normal);
            String exceptionRegion = getRegionException(getRegion(myType, path));
            if (exceptionRegion != null) {
                for (String duplicatePath : duplicatePaths) {
                    String duplicateRegion = getRegion(otherType, duplicatePath);
                    if (exceptionRegion.equals(duplicateRegion)) {
                        duplicatePaths.remove(duplicatePath);
                        log("Removed duplicate path: '" + duplicatePath + "'");
                    }
                }
            }
            // account for collisions with England and the UK. Error message is a bit off for now.
            String subdivisionPath = nameToSubdivisionId.get(value);
            if (subdivisionPath != null) {
                paths.add(subdivisionPath);
            }
            paths.addAll(duplicatePaths);
        }

        // Group territories into emoji (but asymmetric! — if a territory has the same name as an emoji, it is the emoji's fault!
        // If the new format for synthesized flag names is accepted, this is no longer a problem, but leaving the code here for reference.
//        if (myType == Type.ANNOTATIONS) {
//            Set<String> duplicatePaths = getPathsWithValue(
//                getResolvedCldrFileToCheck(), path, value, Type.TERRITORY,
//                Type.TERRITORY.getPrefix(), null, currentAttributesToIgnore, Equivalence.normal);
//            paths.addAll(duplicatePaths);
//            // NOTE: this is slightly overdone, in that it also counts items like 019 colliding with 🌎. But doesn't hurt.
//            // TODO, add 3 subdivisions
//        }


        if (paths.isEmpty()) {
//            System.out.println("Paths is empty");
//            log("Paths is empty");
            return this;
        }

        // Collisions between display names and symbols for the same currency are allowed.
        if (myType == Type.CURRENCY) {
            if (path.contains("/decimal") || path.contains("/group")) {
                return this;
            }
            XPathParts parts = new XPathParts().set(path);
            String currency = parts.getAttributeValue(-2, "type");
            Iterator<String> iterator = paths.iterator();
            while (iterator.hasNext()) {
                String curVal = iterator.next();
                parts.set(curVal);
                if (currency.equals(parts.getAttributeValue(-2, "type")) ||
                    curVal.contains("/decimal") || curVal.contains("/group")) {
                    iterator.remove();
                    log("Removed '" + curVal + "': COLLISON WITH CURRENCY " + currency);
                }
            }
        }

        // Collisions between different lengths and counts of the same unit are allowed
        // Collisions between 'narrow' forms are allowed (the current is filtered by UNITS_IGNORE)
        //ldml/units/unitLength[@type="narrow"]/unit[@type="duration-day-future"]/unitPattern[@count="one"]
        if (myType == Type.UNITS) {
            XPathParts parts = XPathParts.getInstance(path);
            int typeLocation = 3;
            String myUnit = parts.getAttributeValue(typeLocation, "type");
            boolean isDuration = myUnit.startsWith("duration");
            Iterator<String> iterator = paths.iterator();
            while (iterator.hasNext()) {
                String curVal = iterator.next();
                parts.set(curVal);
                String unit = parts.getAttributeValue(typeLocation, "type");
                // we also break the units into two groups: durations and others. Also never collide with a compoundUnitPattern.
                if (myUnit.equals(unit) || unit != null && isDuration != unit.startsWith("duration") ||
                    compoundUnitPatterns.reset(curVal).find()) {
                    iterator.remove();
                    log("Removed '" + curVal + "': COLLISON WITH UNIT  " + unit);
                } else {
                    // Remove allowed collisions, such as between carats and karats (same in many languages), or
                    // between foodcalories and either calories or kilocalories, or
                    // between hectopascal and millibar (physically the same unit, see #10425)
                    for (Map.Entry<String, Set<String>> mapPathPartToSet : mapPathPartsToSetsForDupOK.entrySet()) {
                        if (path.contains(mapPathPartToSet.getKey())) {
                            for (String pathPart : mapPathPartToSet.getValue()) {
                                if (curVal.contains(pathPart)) {
                                    iterator.remove();
                                    log("Removed '" + curVal + "': COLLISON WITH UNIT  " + unit);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        // Collisions between different lengths and counts of the same field are allowed
        if (myType == Type.FIELDS_RELATIVE) {
            XPathParts parts = new XPathParts().set(path);
            String myFieldType = parts.getAttributeValue(3, "type").split("-")[0];
            Iterator<String> iterator = paths.iterator();
            while (iterator.hasNext()) {
                String curVal = iterator.next();
                parts.set(curVal);
                String fieldType = parts.getAttributeValue(3, "type").split("-")[0];
                if (myFieldType.equals(fieldType)) {
                    iterator.remove();
                    log("Removed '" + curVal + "': COLLISON WITH FIELD  " + fieldType);
                }
            }
        }
        // Collisions between different lengths of the same field are allowed
        if (myType == Type.UNITS_COORDINATE) {
            XPathParts parts = new XPathParts().set(path);
            String myFieldType = (parts.containsElement("displayName"))? "displayName": parts.findAttributeValue("coordinateUnitPattern", "type");
            Iterator<String> iterator = paths.iterator();
            while (iterator.hasNext()) {
                String curVal = iterator.next();
                parts.set(curVal);
                String fieldType = (parts.containsElement("displayName"))? "displayName": parts.findAttributeValue("coordinateUnitPattern", "type");
                if (myFieldType.equals(fieldType)) {
                    iterator.remove();
                    log("Removed '" + curVal + "': COLLISON WITH FIELD  " + fieldType);
                }
            }
        }

        // removeMatches(myType);
        // check again on size
        if (paths.isEmpty()) {
            return this;
        }

        // ok, we probably have a collision! Extract the types
        Set<String> collidingTypes = new TreeSet<String>();

        if (SKIP_TYPE_CHECK) {
            for (String pathName : paths) {
                currentAttributesToIgnore.reset(pathName);
                PathHeader pathHeader = getPathHeaderFactory().fromPath(pathName);
                if (getPhase() == Phase.FINAL_TESTING) {
                    collidingTypes.add(pathHeader.getHeaderCode()); // later make this more readable.
                } else {
                    collidingTypes.add("<a href=\"" + CLDRConfig.getInstance().urls().forPathHeader(getCldrFileToCheck().getLocaleID(), pathHeader)
                        + "\">" +
                        pathHeader.getHeaderCode() + "</a>");

                }
            }
        } else {
            for (String dpath : paths) {
                if (!typePattern.reset(dpath).find()) {
                    throw new IllegalArgumentException("Internal error: " + dpath + " doesn't match "
                        + typePattern.pattern());
                }
                collidingTypes.add(typePattern.group(1));
            }

            // remove my type, and check again
            if (!typePattern.reset(path).find()) {
                throw new IllegalArgumentException("Internal error: " + path + " doesn't match "
                    + typePattern.pattern());
            } else {
                collidingTypes.remove(typePattern.group(1));
            }

            // check one last time...
            if (collidingTypes.isEmpty()) {
                log("CollidingTypes is empty");
                return this;
            }
        }

        log("CollidingTypes has a size of " + collidingTypes.size());
        CheckStatus.Type thisErrorType;
        // Also only do warnings during the build phase, so that SmokeTest will build.
        if (getPhase() == Phase.BUILD) {
            thisErrorType = CheckStatus.warningType;
        } else {
            thisErrorType = CheckStatus.errorType;
        }

        // Check to see if we're colliding between standard and generic within the same metazone.
        // If so, then it should be a warning instead of an error, since such collisions are acceptable
        // as long as the context ( generic/recurring vs. specific time ) is known.
        // ( JCE: 8/7/2012 )

        // When long/short standard names for Etc/UTC is added to locale's <zone> items,
        // a collision between exemplarCity and short-standard format was detected.
        //
        // CLDR tool code automatically generate exemplarCity value from zone ID algorithmically,
        // but it should not be used for zones not associated with a location. For Etc/UTC,
        // exemplarCity should be undefined. The value is generated by TimeZoneFormatter#getFallbackName().
        // There are many calling sites in CLDR tool code, and it looks all of them expect non-null
        // value is returned. So, at this point, it's dangerous to touch the code to return
        // null, or throw an exception.
        //
        // In addition to above, collisions between exemplarCity and other zone display name
        // values should be accepted, because exmemplarCity is always formatted with <regionFormat>
        // pattern. However, the collision issue only occurs for the special case - Etc/UTC,
        // we handle the specific case as exception. We may revisit this issue later.
        // (Yoshito 2017-01-27)

        if (path.contains("timeZoneNames") && collidingTypes.size() == 1) {
            PathHeader pathHeader = getPathHeaderFactory().fromPath(path);
            String thisZone = pathHeader.getHeader();
            String thisZoneType = pathHeader.getCode();
            String collisionString = collidingTypes.toString();
            int csStart, csEnd;
            if (collisionString.startsWith("[<a")) {
                csStart = collisionString.indexOf('>') + 1;
                csEnd = collisionString.indexOf('<', csStart);
            } else {
                csStart = collisionString.indexOf('[') + 1;
                csEnd = collisionString.indexOf(']', csStart);
            }
            collisionString = collisionString.substring(csStart, csEnd);
            int delimiter_index = collisionString.indexOf(':');
            String collidingZone = collisionString.substring(0, delimiter_index);
            String collidingZoneType = collisionString.substring(delimiter_index + 2);
            if (thisZone.equals(collidingZone)) {
                if (thisZone.startsWith("Etc/")
                    && (thisZoneType.equals("exemplarCity") || collidingZoneType.equals("exemplarCity"))) {
                    log("Ignore a collision between exemplarCity and another name for Etc/* zones");
                    return this;
                }
                Set<String> collidingZoneTypes = new TreeSet<String>();
                collidingZoneTypes.add(thisZoneType);
                collidingZoneTypes.add(collidingZoneType);
                if (collidingZoneTypes.size() == 2 &&
                    collidingZoneTypes.contains("standard-short") &&
                    collidingZoneTypes.contains("generic-short")) {
                    thisErrorType = CheckStatus.warningType;
                }
            }
        }
        CheckStatus item = new CheckStatus().setCause(this)
            .setMainType(thisErrorType)
            .setSubtype(Subtype.displayCollision)
            .setCheckOnSubmit(false)
            .setMessage(message, new Object[] { collidingTypes.toString() });
        result.add(item);
        return this;
    }

    /*
     * Log a message
     */
    private void log(String string) {
        if (LOG_PATH_REMOVALS) {
            System.out.println(string);
        }
    }

    enum Equivalence {
        normal, exact, unit
    }

    private Set<String> getPathsWithValue(CLDRFile file, String path,
        String value, Type myType,
        String myPrefix, Matcher matcher,
        Matcher currentAttributesToIgnore,
        Equivalence equivalence) {

        if (path.equals(DEBUG_PATH)) {
            int debug = 0;
        }

        Set<String> retrievedPaths = new HashSet<String>();
        if (myType.matchType == MatchType.PREFIX) {
            file.getPathsWithValue(value, myPrefix, matcher, retrievedPaths);
        } else {
            file.getPathsWithValue(value, "//ldml", myType.getPattern().matcher(""), retrievedPaths);
        }

        String normValue = null;
        if (equivalence == Equivalence.unit) {
            normValue = SimpleXMLSource.normalizeCaseSensitive(value);
            //            System.out.println("DEBUG:\t" + "units");
            //            for (String s : retrievedPaths) {
            //                System.out.println("DEBUG:\t" + file.getStringValue(s) + "\t" + s);
            //            }
        }

        // Do first cleanup
        // remove paths with "alt/count"; they can be duplicates
        Set<String> paths = new HashSet<String>();
        for (String pathName : retrievedPaths) {
            Type thisPathType = Type.getType(pathName);
            // If the colliding path is of a different type than the original,
            // then it can't be a collision we care about.
            if (myType != thisPathType) {
                continue;
            }
            ;
            if (exclusions.reset(pathName).find() && thisPathType != Type.UNITS_COORDINATE) {
                continue;
            }
            // we only care about winning paths
            if (!getResolvedCldrFileToCheck().isWinningPath(path)) {
                continue;
            }
            // special cases: don't look at CODE_FALLBACK
            if (myType == Type.CURRENCY && isCodeFallback(path)) {
                continue;
            }
            if (equivalence == Equivalence.exact) {
                String otherValue = file.getWinningValue(pathName);
                if (!otherValue.equals(value)) {
                    continue;
                }
            } else if (equivalence == Equivalence.unit) {
                String otherValue = SimpleXMLSource.normalizeCaseSensitive(file.getWinningValue(pathName));
                if (!otherValue.equals(normValue)) {
                    continue;
                }
            }
            // clean up the pat
            String newPath = currentAttributesToIgnore.reset(pathName).replaceAll("");
            paths.add(newPath);
        }
        //   System.out.println("Paths has a size of:"+paths.size());
        String cleanPath = currentAttributesToIgnore.reset(path).replaceAll("");
        paths.remove(cleanPath);
        //  System.out.println("Removed path: '"+cleanPath+"'");
        //System.out.println("Paths returned has a size of "+paths.size());
        return paths;
    }

    private boolean isCodeFallback(String dpath) {
        String locale = getResolvedCldrFileToCheck().getSourceLocaleID(dpath, null);
        return locale.equals(XMLSource.CODE_FALLBACK_ID);
    }

    private Map<String,String> nameToSubdivisionId = Collections.emptyMap();

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        // pick up the 3 subdivisions
        nameToSubdivisionId = EmojiSubdivisionNames.getNameToSubdivisionPath(cldrFileToCheck.getLocaleID());
        return this;
    }

    /**
     * @param type
     *            the type of the xpath
     * @param xpath
     * @return the region code of the xpath
     */
    private String getRegion(Type type, String xpath) {
        int index = type == Type.ZONE ? -2 : -1;
        return new XPathParts().set(xpath).getAttributeValue(index, "type");
    }

    /**
     * Map with the exceptions
     */
    //private Map<String, String> exceptions;
    private Map<String, String> exceptions;

    /**
     * Checks if the specified region code has any exceptions to the requirement
     * that all exemplar cities and territory names have to be unique.
     *
     * @param regionCode
     *            the region code to be checked
     * @return the corresponding region code that can have a value identical to
     *         the specified region code
     */
    public String getRegionException(String regionCode) {
        if (exceptions != null) {
            String lookup = exceptions.get(regionCode);
            return lookup;
        }

        CLDRFile english = getEnglishFile();
        // Pick up all instances in English where the exemplarCity and territory match
        // and include them as exceptions.
        exceptions = new HashMap<String, String>();
        for (Iterator<String> it = english.iterator(Type.ZONE.getPrefix()); it.hasNext();) {
            String xpath = it.next();
            if (!xpath.endsWith("/exemplarCity")) continue;
            String value = english.getStringValue(xpath);
            Set<String> duplicates = getPathsWithValue(english, xpath, value,
                Type.TERRITORY, Type.TERRITORY.getPrefix(), null, ignoreAltAndCountAttributes, Equivalence.normal);
            if (duplicates.size() > 0) {
                // Assume only 1 duplicate.
                String duplicatePath = duplicates.iterator().next();
                String exemplarCity = getRegion(Type.ZONE, xpath);
                String territory = getRegion(Type.TERRITORY, duplicatePath);
                addRegionException(exemplarCity, territory);
            }
        }

        // Add hardcoded exceptions
        addRegionException("America/Antigua", "AG"); // Antigua and Barbados
        addRegionException("Atlantic/Canary", "IC"); // Canary Islands
        addRegionException("America/Cayman", "KY"); // Cayman Islands
        addRegionException("Indian/Christmas", "CX"); // Christmas Islands
        addRegionException("Indian/Cocos", "CC"); // Cocos [Keeling] Islands
        addRegionException("Indian/Comoro", "KM"); // Comoros Islands, Eastern Africa
        addRegionException("Atlantic/Faeroe", "FO"); // Faroe Islands
        addRegionException("Pacific/Pitcairn", "PN"); // Pitcairn Islands
        addRegionException("Atlantic/St_Helena", "SH"); // Saint Helena
        addRegionException("America/St_Kitts", "KN"); // Saint Kitts and Nevis
        addRegionException("America/St_Lucia", "LC"); // Saint Lucia
        addRegionException("Europe/Vatican", "VA"); // Vatican City
        addRegionException("Pacific/Norfolk", "NF"); // Norfolk Island
        // Some languages don't distinguish between the following city/territory
        // pairs because the city is in the territory and sounds too similar.
        addRegionException("Africa/Algiers", "DZ"); // Algeria
        addRegionException("Africa/Tunis", "TN"); // Tunisia
        return exceptions.get(regionCode);
    }

    /**
     * Adds an exemplarCity/territory pair to the list of region exceptions.
     */
    private void addRegionException(String exemplarCity, String territory) {
        exceptions.put(exemplarCity, territory);
        exceptions.put(territory, exemplarCity);
    }
}