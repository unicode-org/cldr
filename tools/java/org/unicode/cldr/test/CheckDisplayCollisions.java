package org.unicode.cldr.test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.XEquivalenceMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.TimeZone;

public class CheckDisplayCollisions extends FactoryCheckCLDR {
    static final boolean USE_OLD_COLLISION = CldrUtility.getProperty("OLD_COLLISION", false);

    // TODO probably need to fix this to be more accurate over time
    static long year = (long) (365.2425 * 86400 * 1000); // can be approximate
    static long startDate = new Date(1995 - 1900, 1 - 1, 15).getTime(); // can be approximate
    static long endDate = new Date(2011 - 1900, 1 - 1, 15).getTime(); // can be approximate

    /**
     * An enum representing the types of xpaths that we don't want display collisions for.
     */
    private enum Type {
        LANGUAGE("//ldml/localeDisplayNames/languages/language", 0),
        SCRIPT("//ldml/localeDisplayNames/scripts/script", 1),
        TERRITORY("//ldml/localeDisplayNames/territories/territory", 2),
        VARIANT("//ldml/localeDisplayNames/variants/variant", 3),
        CURRENCY("//ldml/numbers/currencies/currency", 4),
        ZONE("//ldml/dates/timeZoneNames/zone", 5),
        METAZONE("//ldml/dates/timeZoneNames/metazone", 6),
        DECIMAL_FORMAT("//ldml/numbers/decimalFormats", 7);

        private String basePrefix;
        private int index;

        private Type(String basePrefix, int index) {
            this.basePrefix = basePrefix;
            this.index = index;
        }

        /**
         * @return the prefix that all XPaths of this type should start with
         */
        public String getPrefix() {
            return basePrefix;
        }

        /**
         * @return the index of this type in the enum
         */
        public int getIndex() {
            return index;
        }

        /**
         * @param path
         *            the path to find the type of
         * @return the type of the path
         */
        public static Type getType(String path) {
            for (Type type : values()) {
                String prefix = type.getPrefix();
                if (path.startsWith(prefix)) {
                    return type;
                }
            }
            return null;
        }
    }

    transient static final int[] pathOffsets = new int[2];
    transient static final int[] otherOffsets = new int[2];
    static final boolean SKIP_TYPE_CHECK = true;

    Matcher exclusions = Pattern.compile("XXXX").matcher(""); // no matches
    Matcher typePattern = Pattern.compile("\\[@type=\"([^\"]*+)\"]").matcher("");
    Matcher attributesToIgnore = Pattern.compile("\\[@(?:count|alt)=\"[^\"]*+\"]").matcher("");
    Matcher compactNumberAttributesToIgnore = Pattern.compile("\\[@(?:alt)=\"[^\"]*+\"]").matcher("");

    boolean[] builtCollisions;
    Set<String> paths = new HashSet<String>();
    Set<String> collidingTypes = new TreeSet<String>();

    private XPathParts parts1 = new XPathParts(null, null);
    private XPathParts parts2 = new XPathParts(null, null);
    private transient Relation<String, String> hasCollisions = Relation.of(new TreeMap<String, Set<String>>(),
        HashSet.class);
    private boolean finalTesting;

    private PathHeader.Factory pathHeaderFactory;

    public CheckDisplayCollisions(Factory factory) {
        super(factory);
        pathHeaderFactory = PathHeader.getFactory(factory.make("en", true));
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
        List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have

        if (USE_OLD_COLLISION) { // don't use this until memory issues are cleaned up.

            for (Type type : Type.values()) {
                if (path.startsWith(type.getPrefix()) && !exclusions.reset(path).find()) {
                    if (!builtCollisions[type.getIndex()]) {
                        buildCollisions(type.getIndex());
                    }
                    Set codes = hasCollisions.getAll(path);
                    if (codes != null) {
                        // String code = CLDRFile.getCode(path);
                        // Set codes = new TreeSet(s);
                        // codes.remove(code); // don't show self

                        CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.displayCollision)
                            .setCheckOnSubmit(false)
                            .setMessage("Can't have same translation as {0}", new Object[] { codes.toString() });
                        result.add(item);
                    }
                    break;
                }
            }
        } else {
            if (value.equals("∅∅∅")) {
                return this;
            }
            if (exclusions.reset(path).find()) {
                return this;
            }

            // find my type; bail if I don't have one.
            Type myType = Type.getType(path);
            if (myType == null) {
                return this;
            }
            String myPrefix = myType.getPrefix();

            // get the paths with the same value. If there aren't duplicates, continue;
            paths.clear();

            Matcher matcher = null;
            String message = "Can't have same translation as {0}";
            Matcher currentAttributesToIgnore = attributesToIgnore;

            if (myType == Type.DECIMAL_FORMAT) {
                if (!path.contains("[@count=") || "0".equals(value)) {
                    return this;
                }
                XPathParts parts = new XPathParts().set(path);
                String type = parts.getAttributeValue(-1, "type");
                myPrefix = parts.removeElement(-1).toString();
                matcher = Pattern.compile(myPrefix.replaceAll("\\[", "\\\\[") +
                    "/pattern\\[@type=(?!\"" + type + "\")\"\\d+\"].*").matcher(path);
                currentAttributesToIgnore = compactNumberAttributesToIgnore;
                message = "Can't have same number pattern as {0}";
            }
            paths = getPathsWithValue(getResolvedCldrFileToCheck(),
                path, value, myType, myPrefix, matcher, currentAttributesToIgnore);
            // Group exemplar cities and territories together for display collisions.
            if (myType == Type.TERRITORY || myType == Type.ZONE) {
                Type otherType = myType == Type.TERRITORY ? Type.ZONE : Type.TERRITORY;
                Set<String> duplicatePaths = getPathsWithValue(
                    getResolvedCldrFileToCheck(), path, value, otherType,
                    otherType.getPrefix(), null, currentAttributesToIgnore);
                String exceptionRegion = getRegionException(getRegion(myType, path));
                if (exceptionRegion != null) {
                    for (String duplicatePath : duplicatePaths) {
                        String duplicateRegion = getRegion(otherType, duplicatePath);
                        if (exceptionRegion.equals(duplicateRegion)) {
                            duplicatePaths.remove(duplicatePath);
                        }
                    }
                }
                paths.addAll(duplicatePaths);
            }

            if (paths.isEmpty()) {
                return this;
            }

            // removeMatches(myType);
            // check again on size
            if (paths.isEmpty()) {
                return this;
            }

            // ok, we probably have a collision! Extract the types
            collidingTypes.clear();
            if (SKIP_TYPE_CHECK) {
                for (String pathName : paths) {
                    currentAttributesToIgnore.reset(pathName);
                    PathHeader pathHeader = pathHeaderFactory.fromPath(pathName);
                    collidingTypes.add(pathHeader.getHeaderCode()); // later make this more readable.
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
                    return this;
                }
            }

            // Check to see if we're colliding between standard and generic within the same metazone.
            // If so, then it should be a warning instead of an error, since such collisions are acceptable
            // as long as the context ( generic/recurring vs. specific time ) is known.
            // ( JCE: 8/7/2012 )

            String thisErrorType = CheckStatus.errorType;

            if (path.contains("timeZoneNames") && collidingTypes.size() == 1) {
                PathHeader pathHeader = pathHeaderFactory.fromPath(path);
                String thisZone = pathHeader.getHeader();
                String thisZoneType = pathHeader.getCode();
                String collisionString = collidingTypes.toString();
                collisionString = collisionString.substring(1, collisionString.length() - 1); // Strip off []
                int delimiter_index = collisionString.indexOf(':');
                String collidingZone = collisionString.substring(0, delimiter_index);
                String collidingZoneType = collisionString.substring(delimiter_index + 2);
                if (thisZone.equals(collidingZone)) {
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
            CheckStatus item = new CheckStatus().setCause(this).setMainType(thisErrorType)
                .setSubtype(Subtype.displayCollision)
                .setCheckOnSubmit(false)
                .setMessage(message, new Object[] { collidingTypes.toString() });
            result.add(item);
        }
        return this;
    }

    private Set<String> getPathsWithValue(CLDRFile file, String path,
        String value, Type myType,
        String myPrefix, Matcher matcher, Matcher currentAttributesToIgnore) {
        Set<String> retrievedPaths = new HashSet<String>();
        file.getPathsWithValue(value, myPrefix, matcher, retrievedPaths);
        // Do first cleanup
        // remove paths with "alt/count"; they can be duplicates
        Set<String> paths = new HashSet<String>();
        for (String pathName : retrievedPaths) {
            if (exclusions.reset(pathName).find()) {
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
            // clean up the pat
            String newPath = currentAttributesToIgnore.reset(pathName).replaceAll("");
            paths.add(newPath);
        }
        String cleanPath = currentAttributesToIgnore.reset(path).replaceAll("");
        paths.remove(cleanPath);
        return paths;
    }

    private void removeMatches(int myType) {
        // filter the paths
        main: for (Iterator<String> it = paths.iterator(); it.hasNext();) {
            String dpath = it.next();
            // make sure it is the winning path
            if (!getResolvedCldrFileToCheck().isWinningPath(dpath)) {
                it.remove();
                continue main;
            }
            // special case languages: don't look at CODE_FALLBACK
            if (dpath.startsWith(Type.LANGUAGE.getPrefix()) && isCodeFallback(dpath)) {
                it.remove();
                continue main;
            }
            // // make sure the collision is with the same type
            // if (dpath.startsWith(typesICareAbout[myType])
            // && !exclusions.reset(dpath).find()) {
            // continue main;
            // }
            // no match, remove
            it.remove();
        }
    }

    private boolean isCodeFallback(String dpath) {
        String locale = getResolvedCldrFileToCheck().getSourceLocaleID(dpath, null);
        return locale.equals(XMLSource.CODE_FALLBACK_ID);
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        finalTesting = Phase.FINAL_TESTING == getPhase();

        // clear old status
        clear();
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
        if (exceptions != null) return exceptions.get(regionCode);

        CLDRFile english = getFactory().make("en", true);
        // Pick up all instances in English where the exemplarCity and territory match
        // and include them as exceptions.
        exceptions = new HashMap<String, String>();
        for (Iterator<String> it = english.iterator(Type.ZONE.getPrefix()); it.hasNext();) {
            String xpath = it.next();
            if (!xpath.endsWith("/exemplarCity")) continue;
            String value = english.getStringValue(xpath);
            Set<String> duplicates = getPathsWithValue(english, xpath, value,
                Type.TERRITORY, Type.TERRITORY.getPrefix(), null, attributesToIgnore);
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

    private void clear() {
        hasCollisions.clear();
        builtCollisions = new boolean[Type.values().length];
    }

    // quick rewrite to make it lazy-evaluated

    private void buildCollisions(int ii) {
        builtCollisions[ii] = true; // mark done
        // put key,value pairs into equivalence map
        CLDRFile cldrFileToCheck = getResolvedCldrFileToCheck();

        XEquivalenceMap collisions = new XEquivalenceMap();

        int itemType = -1;
        String prefix = Type.values()[ii].getPrefix();
        for (Iterator it2 = cldrFileToCheck.iterator(prefix); it2.hasNext();) {
            String xpath = (String) it2.next();
            int thisItemType = CLDRFile.getNameType(xpath);
            if (thisItemType < 0) {
                continue;
            }
            // only check winning paths
            if (!cldrFileToCheck.isWinningPath(xpath) || finalTesting && xpath.contains("proposed")) {
                continue;
            }
            // special case language; exclude codeFallback
            if (ii == Type.LANGUAGE.getIndex() && isCodeFallback(xpath)) {
                continue;
            }
            itemType = thisItemType;
            // Merge some namespaces
            if (itemType == CLDRFile.CURRENCY_NAME)
                itemType = CLDRFile.CURRENCY_SYMBOL;
            else if (itemType >= CLDRFile.TZ_START && itemType < CLDRFile.TZ_LIMIT) itemType = CLDRFile.TZ_START;
            String value = cldrFileToCheck.getStringValue(xpath);
            String skeleton = getSkeleton(value);
            collisions.add(xpath, skeleton);
        }

        // now get just the types, and store them in sets
        // HashMap<String,String> mapItems = new HashMap<String>();
        for (Iterator it = collisions.iterator(); it.hasNext();) {
            Set equivalence = (Set) it.next();
            if (equivalence.size() == 1) continue;

            // this is a tricky bit. If two items are fixed timezones
            // AND they both map to the same offset
            // then they don't collide with each other (but they may collide with others)

            // first copy all the equivalence classes, since we are going to modify them
            // remove our own path

            for (Iterator<String> it2 = equivalence.iterator(); it2.hasNext();) {
                String path = it2.next();
                // if (path.indexOf("ERN") >= 0 || path.indexOf("ERB") >= 0) {
                // System.out.println("ERN");
                // }
                // now recored any non equivalent paths
                for (Iterator it3 = equivalence.iterator(); it3.hasNext();) {
                    String otherPath = (String) it3.next();
                    if (otherPath.equals(path)) {
                        continue;
                    }
                    if (!isEquivalent(itemType, path, otherPath)) {
                        String codeName = CLDRFile.getCode(otherPath);
                        if (itemType == CLDRFile.TZ_START) {
                            int type = CLDRFile.getNameType(path);
                            codeName += " (" + CLDRFile.getNameName(type) + ")";
                        } else {
                            String english = getDisplayInformation().getStringValue(otherPath);
                            if (english != null) {
                                codeName += " (" + english + ")";
                            }
                        }
                        hasCollisions.put(path, codeName);
                    }
                }
            }
        }
    }

    private String getSkeleton(String value) {
        value = Normalizer.normalize(value, Normalizer.NFKC);
        value = UCharacter.foldCase(value, true);
        value = Normalizer.normalize(value, Normalizer.NFKC);
        value = value.replace(".", "");
        value = value.replace("₤", "£");
        value = value.replace("₨", "Rs");
        // TODO Remove other punctuation: etc.
        return value;
    }

    private boolean isEquivalent(int itemType, String path, String otherPath) {
        // if the paths are the same except for alt-proposed, then they are equivalent.
        if (sameExceptProposed(path, otherPath)) return true;

        // check for special equivalences among types
        switch (itemType) {
        case CLDRFile.CURRENCY_SYMBOL:
            return CLDRFile.getCode(path).equals(CLDRFile.getCode(otherPath));
        case CLDRFile.TZ_START:
            // if (path.indexOf("London") >= 0) {
            // System.out.println("Debug");
            // }
            // if they are fixed, constant values and identical, they are ok
            getOffset(path, pathOffsets);
            getOffset(otherPath, otherOffsets);

            if (pathOffsets[0] == otherOffsets[0]
                && pathOffsets[0] == pathOffsets[1]
                && otherOffsets[0] == otherOffsets[1]) return true;

            // if they are short/long variants of the same path, they are ok
            if (CLDRFile.getCode(path).equals(CLDRFile.getCode(otherPath))) {
                int nameType = CLDRFile.getNameType(path);
                int otherType = CLDRFile.getNameType(otherPath);
                switch (nameType) {
                case CLDRFile.TZ_GENERIC_LONG:
                    return otherType == CLDRFile.TZ_GENERIC_SHORT;
                case CLDRFile.TZ_GENERIC_SHORT:
                    return otherType == CLDRFile.TZ_GENERIC_LONG;
                }
            }
        }
        return false;
    }

    private boolean sameExceptProposed(String path, String otherPath) {
        if (!path.contains("alt") && !otherPath.contains("alt")) {
            return path.equals(otherPath);
        }
        parts1.set(path);
        parts2.set(otherPath);
        if (parts1.size() != parts2.size()) return false;
        for (int i = 0; i < parts1.size(); ++i) {
            if (!parts1.getElement(i).equals(parts2.getElement(i))) return false;
            if (parts1.getAttributeCount(i) == 0 && parts2.getAttributeCount(i) == 0) continue;
            Map attributes1 = parts1.getAttributes(i);
            Map attributes2 = parts2.getAttributes(i);
            Set s1 = attributes1.keySet();
            Set s2 = attributes2.keySet();
            if (s1.contains("alt")) { // WARNING: we have to copy so as to not modify map
                s1 = new HashSet(s1);
                s1.remove("alt");
            }
            if (s2.contains("alt")) { // WARNING: we have to copy so as to not modify map
                s2 = new HashSet(s2);
                s2.remove("alt");
            }
            if (!s1.equals(s2)) return false;
            for (Iterator it = s1.iterator(); it.hasNext();) {
                Object key = it.next();
                Object v1 = attributes1.get(key);
                Object v2 = attributes2.get(key);
                if (!v1.equals(v2)) return false;
            }
        }
        return true;
    }

    private void getOffset(String path, int[] standardAndDaylight) {
        String code = CLDRFile.getCode(path);
        TimeZone tz = TimeZone.getTimeZone(code);
        int daylight = Integer.MIN_VALUE; // is the max offset
        int standard = Integer.MAX_VALUE; // is the min offset
        for (long date = startDate; date < endDate; date += year / 2) {
            // Date d = new Date(date);
            int offset = tz.getOffset(date);
            if (daylight < offset) daylight = offset;
            if (standard > offset) standard = offset;
        }
        if (path.indexOf("/daylight") >= 0)
            standard = daylight;
        else if (path.indexOf("/standard") >= 0) daylight = standard;
        standardAndDaylight[0] = standard;
        standardAndDaylight[1] = daylight;
    }

    private boolean isFixedTZ(String xpath) {
        return (xpath.indexOf("/standard") >= 0 || xpath.indexOf("/daylight") >= 0);
    }
}