package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.StandardCodes;
import com.ibm.icu.impl.CollectionUtilities;

public class TestMetadata {
	public static void main(String[] args) {
		Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
        CLDRFile metadata = cldrFactory.make("supplementalMetadata", false);
//        Set allKeys = new TreeSet();
//        CollectionUtilities.addAll(metadata.iterator(), allKeys);
//        System.out.println("Keys: " + allKeys);
        
        String zoneList = null;
        for (Iterator it = metadata.iterator(); it.hasNext();) {
        	String key = (String) it.next();
        	if (key.indexOf("\"$tzid\"") >= 0) {
        		zoneList = metadata.getStringValue(key);
        		break;
        	}
        }
        
        String[] zones = zoneList.split("\\s+");
        Set metaZoneSet = new TreeSet();
        metaZoneSet.addAll(Arrays.asList(zones));

        StandardCodes sc = StandardCodes.make();
        Map new_oldZones = sc.getZoneData();
        Set stdZoneSet = new TreeSet();
        stdZoneSet.addAll(new_oldZones.keySet());
        
        if (metaZoneSet.equals(stdZoneSet)) {
        	System.out.println("Zone Set is up-to-date");
        } else {
        	Set diff = new TreeSet();
        	diff.addAll(metaZoneSet);
        	diff.removeAll(stdZoneSet);
        	System.out.println("Meta Zones - Std Zones: " + diff);
        	diff.clear();
        	diff.addAll(stdZoneSet);
        	diff.removeAll(metaZoneSet);
        	System.out.println("Std Zones - Meta Zones: " + diff);
        	
           	System.out.println("Meta Zones: " + metaZoneSet);
           	System.out.println("Std Zones: " + stdZoneSet);

        }
        System.out.println("Done");
	}
}