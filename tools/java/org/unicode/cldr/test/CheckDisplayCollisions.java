package org.unicode.cldr.test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.dev.test.util.XEquivalenceMap;
import com.ibm.icu.util.TimeZone;

public class CheckDisplayCollisions extends CheckCLDR {
	String[] typesICareAbout = {
			"//ldml/localeDisplayNames/languages/language[@type=\"",
			"//ldml/localeDisplayNames/scripts/script[@type=\"",
			"//ldml/localeDisplayNames/territories/territory[@type=\"",
			"//ldml/localeDisplayNames/variants/variant[@type=\"",
			"//ldml/numbers/currencies/currency[@type=\"", "\"]/displayName", "currency",
			"//ldml/dates/timeZoneNames/zone[@type=\""	
	};
	boolean[] builtCollisions;;
	
	private transient XEquivalenceMap[] collisions = new XEquivalenceMap[CLDRFile.LIMIT_TYPES];
	private Map hasCollisions = new HashMap();
	{
		for (int i = 0; i < collisions.length; ++i) collisions[i] = new XEquivalenceMap();
		clear();
	}
	
	public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
		for (int i = 0; i < typesICareAbout.length; ++i) {
			if (path.startsWith(typesICareAbout[i])) {
				if (!builtCollisions[i]) buildCollisions(i);
				Set codes = (Set) hasCollisions.get(path);
				if (codes != null) {
					//String code = CLDRFile.getCode(path);
					//Set codes = new TreeSet(s);
					//codes.remove(code); // don't show self
					CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
              .setCheckOnSubmit(false)
              .setMessage("Can't have same translation as {0}", new Object[]{codes.toString()});
					result.add(item);
				}
				break;
			}
		}
		return this;
	}

	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		cldrFileToCheck = cldrFileToCheck.getResolved(); // check resolved cases
		super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		
		// clear old status
		clear();
		return this;
	}

	private void clear() {
		hasCollisions.clear();
		builtCollisions = new boolean[typesICareAbout.length];
		for (int itemType = 0; itemType < collisions.length; ++itemType) collisions[itemType].clear();
	}
	
	// quick rewrite to make it lazy-evaluated
	
	private void buildCollisions(int ii) {
		builtCollisions[ii] = true; // mark done
		// put key,value pairs into equivalence map
		CLDRFile cldrFileToCheck = getCldrFileToCheck();
		
		for (Iterator it2 = cldrFileToCheck.iterator(typesICareAbout[ii]); it2.hasNext();) {
			String xpath = (String) it2.next();
			int nameType = CLDRFile.getNameType(xpath);
			if (nameType < 0) continue;
			// Merge some namespaces
			if (nameType == CLDRFile.CURRENCY_NAME) nameType = CLDRFile.CURRENCY_SYMBOL;
			else if (nameType >= CLDRFile.TZ_START && nameType < CLDRFile.TZ_LIMIT) nameType = CLDRFile.TZ_START;
			String value = cldrFileToCheck.getStringValue(xpath);
			collisions[nameType].add(xpath, value);
		}
		
		// now get just the types, and store them in sets
		HashMap mapItems = new HashMap();
		for (int itemType = 0; itemType < collisions.length; ++itemType) {
			mapItems.clear();
			for (Iterator it = collisions[itemType].iterator(); it.hasNext();) {
				Set equivalence = (Set) it.next();
				if (equivalence.size() == 1) continue;
				
				// this is a tricky bit. If two items are fixed timezones
				// AND they both map to the same offset
				// then they don't collide with each other (but they may collide with others)
				
				// first copy all the equivalence classes, since we are going to modify them
				// remove our own path

				for (Iterator it2 = equivalence.iterator(); it2.hasNext();) {
					String path = (String) it2.next();
//					if (path.indexOf("ERN") >= 0 || path.indexOf("ERB") >= 0) {
//						System.out.println("ERN");
//					}
					Set s = new HashSet(equivalence);
					s.remove(path);
					mapItems.put(path, s);
				}
				
				// now remove any equivalent items, and then remove hole mapping if empty
				for (Iterator it2 = mapItems.keySet().iterator(); it2.hasNext();) {
					String path = (String) it2.next();
					Set pathSet = (Set) mapItems.get(path);
					for (Iterator it3 = pathSet.iterator(); it3.hasNext();) {
						String otherPath = (String) it3.next();
						if (isEquivalent(itemType, path, otherPath)) it3.remove(); // offset == getOffset(otherPath)
					}
					if (pathSet.size() == 0) it2.remove();
				}
				
				// now get just the types, and store them in sets
				for (Iterator it2 = mapItems.keySet().iterator(); it2.hasNext();) {
					String path = (String) it2.next();
					Set pathSet = (Set) mapItems.get(path);
					//if (pathSet.size() == 0) continue;
					Set colliding = new TreeSet();
					hasCollisions.put(path, colliding);
					for (Iterator it3 = pathSet.iterator(); it3.hasNext();) {
						String otherPath = (String) it3.next();
						String codeName = CLDRFile.getCode(otherPath);
						if (itemType == CLDRFile.TZ_START) {
							int type = CLDRFile.getNameType(path);
							codeName += " (" + CLDRFile.getNameName(type) + ")";
						}
						colliding.add(codeName);
					}
				}
			}
		}
		// throw this stuff away so it gets garbage collected.
		for (int i = 0; i < collisions.length; ++i) collisions[i].clear();
	}

	transient static final int[] pathOffsets = new int[2];
	transient static final int[] otherOffsets = new int[2];
	private boolean isEquivalent(int itemType, String path, String otherPath) {
		if (path.equals(otherPath))
			return true;
        // if the paths are the same except for alt-proposed, then they are equivalent.
        if (sameExceptProposed(path, otherPath)) return true;
		switch (itemType) {
		case CLDRFile.CURRENCY_SYMBOL:
			return CLDRFile.getCode(path).equals(CLDRFile.getCode(otherPath));
		case CLDRFile.TZ_START:
//			if (path.indexOf("London") >= 0) {
//				System.out.println("Debug");
//			}
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

    private XPathParts parts1 = new XPathParts(null, null);
    private XPathParts parts2 = new XPathParts(null, null);
	private boolean sameExceptProposed(String path, String otherPath) {
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

    // TODO probably need to fix this to be more accurate over time
	static long year = (long)(365.2425 * 86400 * 1000); // can be approximate
	static long startDate = new Date(1995-1900, 1 - 1, 15).getTime(); // can be approximate
	static long endDate = new Date(2011-1900, 1 - 1, 15).getTime(); // can be approximate
	
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