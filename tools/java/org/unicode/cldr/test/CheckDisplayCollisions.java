package org.unicode.cldr.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.XEquivalenceClass;

public class CheckDisplayCollisions extends CheckCLDR {
	private transient Map[] maps = new HashMap[CLDRFile.LIMIT_TYPES];
	private transient XEquivalenceClass[] collisions = new XEquivalenceClass[CLDRFile.LIMIT_TYPES];
	private Map hasCollisions = new HashMap();
	{
		for (int i = 0; i < maps.length; ++i) maps[i] = new HashMap();
		for (int i = 0; i < collisions.length; ++i) collisions[i] = new XEquivalenceClass(null);
	}
	
	public CheckCLDR _check(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) {
		Set s = (Set) hasCollisions.get(path);
		if (s != null) {
			String code = CLDRFile.getCode(path);
			Set codes = new TreeSet(s);
			codes.remove(code); // don't show self
			CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
			.setMessage("Collision with {0}", new Object[]{codes.toString()});
			result.add(item);
		}
		return this;
	}

	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, possibleErrors);
		for (int i = 0; i < maps.length; ++i) maps[i].clear();
		for (int i = 0; i < collisions.length; ++i) collisions[i].clear(null);
		for (Iterator it2 = cldrFileToCheck.keySet().iterator(); it2.hasNext();) {
			String xpath = (String) it2.next();
			int nameType = CLDRFile.getNameType(xpath);
			if (nameType < 0) continue;
			String value = cldrFileToCheck.getStringValue(xpath);
			String xpath2 = (String) maps[nameType].get(value);
			if (xpath2 == null) {
				maps[nameType].put(value, xpath);
				continue;
			}
			collisions[nameType].add(xpath, xpath2);
		}
		// now get just the types, and store them in sets
		for (int i = 0; i < collisions.length; ++i) {
			Set equivalences = collisions[i].getEquivalenceSets();
			for (Iterator it = equivalences.iterator(); it.hasNext();) {
				Set equivalence = (Set) it.next();
				Set colliding = new TreeSet();
				for (Iterator it2 = equivalence.iterator(); it2.hasNext();) {
					String path = (String) it2.next();
					hasCollisions.put(path, colliding);
					colliding.add(CLDRFile.getCode(path));	
				}
			}
		}
		// throw this stuff away so it gets garbage collected.
		for (int i = 0; i < maps.length; ++i) maps[i].clear();
		for (int i = 0; i < collisions.length; ++i) collisions[i].clear(null);
		return this;
	}
	


}