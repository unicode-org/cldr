/*
 * Created on Jan 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.unicode.cldr.util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author davis
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestUtilities {
	public static void main(String[] args) {
		StandardCodes sc = StandardCodes.make();
		Set s = sc.getAvailableCodes("currency");
		for (Iterator it = s.iterator(); it.hasNext();) {
			String code = (String)it.next();
			String name = sc.getData("currency", code);
			List data = sc.getFullData("currency", code);
			System.out.println(code + "\t" + name + "\t" + data);
		}
	}
}
