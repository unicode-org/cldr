package org.unicode.cldr.test;

import java.util.Date;
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
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.XEquivalenceMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.TimeZone;

public class CheckDisplayCollisions extends FactoryCheckCLDR {
    static final boolean USE_OLD_COLLISION = CldrUtility.getProperty("OLD_COLLISION", false);

    // TODO probably need to fix this to be more accurate over time
    static long year = (long)(365.2425 * 86400 * 1000); // can be approximate
    static long startDate = new Date(1995-1900, 1 - 1, 15).getTime(); // can be approximate
    static long endDate = new Date(2011-1900, 1 - 1, 15).getTime(); // can be approximate

    static final String LANGUAGE_PREFIX = "//ldml/localeDisplayNames/languages/language";
    static final int LANGUAGE_PREFIX_INDEX = 0; // sync with typesICareAbout
    static final int CURRENCY_PREFIX_INDEX = 4; // sync with typesICareAbout
    static final String DECIMAL_FORMAT_PREFIX = "//ldml/numbers/decimalFormats";

    static final String[] typesICareAbout = {
        LANGUAGE_PREFIX,
        "//ldml/localeDisplayNames/scripts/script",
        "//ldml/localeDisplayNames/territories/territory",
        "//ldml/localeDisplayNames/variants/variant",
        "//ldml/numbers/currencies/currency",
        //"\"]/displayName", "currency",
        "//ldml/dates/timeZoneNames/zone",	
        "//ldml/dates/timeZoneNames/metazone",
        DECIMAL_FORMAT_PREFIX
    };

    transient static final int[] pathOffsets = new int[2];
    transient static final int[] otherOffsets = new int[2];
    static final boolean SKIP_TYPE_CHECK = true;

    Matcher exclusions = Pattern.compile("XXXX").matcher(""); // no matches
    Matcher typePattern = Pattern.compile("\\[@type=\"([^\"]*+)\"]").matcher("");
    Matcher attributesToIgnore = Pattern.compile("\\[@(?:count|alt)=\"[^\"]*+\"]").matcher("");
    Matcher compactNumberAttributesToIgnore = Pattern.compile("\\[@(?:alt)=\"[^\"]*+\"]").matcher("");

    boolean[] builtCollisions;
    Set<String> paths = new HashSet<String>();
    Set<String> retrievedPaths = new HashSet<String>();
    Set<String> collidingTypes = new TreeSet<String>();

    private XPathParts parts1 = new XPathParts(null, null);
    private XPathParts parts2 = new XPathParts(null, null);
    private transient Relation<String,String> hasCollisions = Relation.of(new TreeMap<String,Set<String>>(), HashSet.class);
    private boolean finalTesting;
    private Set<Count> pluralTypes;

    private PathHeader.Factory pathHeaderFactory;

    public CheckDisplayCollisions(Factory factory) {
        super(factory);
        pathHeaderFactory = PathHeader.getFactory(factory.make("en", true));
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have

        if (USE_OLD_COLLISION) { // don't use this until memory issues are cleaned up.

            for (int i = 0; i < typesICareAbout.length; ++i) {
                if (path.startsWith(typesICareAbout[i]) && !exclusions.reset(path).find()) {
                    if (!builtCollisions[i]) {
                        buildCollisions(i);
                    }
                    Set codes = hasCollisions.getAll(path);
                    if (codes != null) {
                        //String code = CLDRFile.getCode(path);
                        //Set codes = new TreeSet(s);
                        //codes.remove(code); // don't show self

                        CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.displayCollision)
                            .setCheckOnSubmit(false)
                            .setMessage("Can't have same translation as {0}", new Object[]{codes.toString()});
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
            int myType = -1;
            String myPrefix = null;
            for (int i = 0; i < typesICareAbout.length; ++i) {
                if (path.startsWith(typesICareAbout[i])) {
                    myType = i;
                    myPrefix = typesICareAbout[i];
                    break;
                }
            }
            if (myType < 0) {
                return this;
            }

            // get the paths with the same value. If there aren't duplicates, continue;
            retrievedPaths.clear();
            paths.clear();

            Matcher matcher = null;
            String message = "Can't have same translation as {0}";
            Matcher currentAttributesToIgnore = attributesToIgnore;

            if (myPrefix.equals(DECIMAL_FORMAT_PREFIX)) {
                if (!path.contains("[@count=")) {
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
            getResolvedCldrFileToCheck().getPathsWithValue(value, myPrefix, matcher, retrievedPaths);
            // Do first cleanup
            // remove paths with "alt/count"; they can be duplicates
            for (String pathName : retrievedPaths) {
                if (exclusions.reset(pathName).find()) {
                    continue;
                }
                // we only care about winning paths
                if (!getResolvedCldrFileToCheck().isWinningPath(path)) {
                    continue ;
                }
                // special cases: don't look at CODE_FALLBACK
                if ((myType == CURRENCY_PREFIX_INDEX || myType == CURRENCY_PREFIX_INDEX) && isCodeFallback(path)) {
                    continue;
                }
                // clean up the pat
                String newPath = currentAttributesToIgnore.reset(pathName).replaceAll("");
                paths.add(newPath);
            }
            String cleanPath = currentAttributesToIgnore.reset(path).replaceAll("");
            paths.remove(cleanPath);

            if (paths.isEmpty()) {
                return this;
            }

            //removeMatches(myType);
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
                        throw new IllegalArgumentException("Internal error: " + dpath + " doesn't match " + typePattern.pattern());
                    }
                    collidingTypes.add(typePattern.group(1));
                }

                // remove my type, and check again
                if (!typePattern.reset(path).find()) {
                    throw new IllegalArgumentException("Internal error: " + path + " doesn't match " + typePattern.pattern());
                } else {
                    collidingTypes.remove(typePattern.group(1));
                }
                // check one last time...
                if (collidingTypes.isEmpty()) {
                    return this;
                }}

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
                collisionString = collisionString.substring(1,collisionString.length()-1); // Strip off []
                int delimiter_index = collisionString.indexOf(':');
                String collidingZone = collisionString.substring(0,delimiter_index);
                String collidingZoneType = collisionString.substring(delimiter_index+2);
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
            CheckStatus item = new CheckStatus().setCause(this).setMainType(thisErrorType).setSubtype(Subtype.displayCollision)
                .setCheckOnSubmit(false)
                .setMessage(message, new Object[]{collidingTypes.toString()});
            result.add(item);
        }
        return this;
    }

    private void removeMatches(int myType) {
        // filter the paths
        main:
            for (Iterator<String> it = paths.iterator(); it.hasNext();) {
                String dpath = it.next();
                // make sure it is the winning path
                if (!getResolvedCldrFileToCheck().isWinningPath(dpath)) {
                    it.remove();
                    continue main;
                }
                // special case languages: don't look at CODE_FALLBACK
                if (dpath.startsWith(LANGUAGE_PREFIX) && isCodeFallback(dpath)) {
                    it.remove();
                    continue main;
                }
                //                // make sure the collision is with the same type
                //                if (dpath.startsWith(typesICareAbout[myType]) 
                //                        && !exclusions.reset(dpath).find()) {
                //                    continue main;
                //                }
                // no match, remove
                it.remove();
            }
    }

    private boolean isCodeFallback(String dpath) {
        String locale = getResolvedCldrFileToCheck().getSourceLocaleID(dpath, null);
        return locale.equals(XMLSource.CODE_FALLBACK_ID);
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        finalTesting  = Phase.FINAL_TESTING == getPhase();
        SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(
            getFactory().getSupplementalDirectory()); 
        PluralInfo pluralInfo = supplementalData.getPlurals(PluralType.cardinal, cldrFileToCheck.getLocaleID());
        pluralTypes = pluralInfo.getCountToExamplesMap().keySet();

        // clear old status
        clear();
        return this;
    }

    private void clear() {
        hasCollisions.clear();
        builtCollisions = new boolean[typesICareAbout.length];
    }

    // quick rewrite to make it lazy-evaluated

    private void buildCollisions(int ii) {
        builtCollisions[ii] = true; // mark done
        // put key,value pairs into equivalence map
        CLDRFile cldrFileToCheck = getResolvedCldrFileToCheck();

        XEquivalenceMap collisions = new XEquivalenceMap();

        int itemType = -1;
        for (Iterator it2 = cldrFileToCheck.iterator(typesICareAbout[ii]); it2.hasNext();) {
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
            if (ii == LANGUAGE_PREFIX_INDEX && isCodeFallback(xpath)) {
                continue;
            }
            itemType = thisItemType;
            // Merge some namespaces
            if (itemType == CLDRFile.CURRENCY_NAME) itemType = CLDRFile.CURRENCY_SYMBOL;
            else if (itemType >= CLDRFile.TZ_START && itemType < CLDRFile.TZ_LIMIT) itemType = CLDRFile.TZ_START;
            String value = cldrFileToCheck.getStringValue(xpath);
            String skeleton = getSkeleton(value);
            collisions.add(xpath, skeleton);
        }

        // now get just the types, and store them in sets
        //HashMap<String,String> mapItems = new HashMap<String>();
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
                //      if (path.indexOf("ERN") >= 0 || path.indexOf("ERB") >= 0) {
                //      System.out.println("ERN");
                //      }
                // now recored any non equivalent paths
                for (Iterator it3 = equivalence.iterator(); it3.hasNext();) {
                    String otherPath = (String) it3.next();
                    if (otherPath.equals(path)) {
                        continue;
                    }
                    if (!isEquivalent(itemType, path, otherPath)){
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
        value = value.replace("₤","£");
        value = value.replace("₨","Rs");
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
            //      if (path.indexOf("London") >= 0) {
            //      System.out.println("Debug");
            //      }
            // if they are fixed, constant values and identical, they are ok
            getOffset(path,pathOffsets);
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
        for (long date = startDate; date < endDate; date += year/2) {
            //Date d = new Date(date);
            int offset = tz.getOffset(date);
            if (daylight < offset) daylight = offset;
            if (standard > offset) standard = offset;
        }
        if (path.indexOf("/daylight") >= 0) standard = daylight;
        else if (path.indexOf("/standard") >= 0) daylight = standard;
        standardAndDaylight[0] = standard;
        standardAndDaylight[1] = daylight;
    }

    private boolean isFixedTZ(String xpath) {
        return (xpath.indexOf("/standard") >= 0 || xpath.indexOf("/daylight") >= 0);
    }
}