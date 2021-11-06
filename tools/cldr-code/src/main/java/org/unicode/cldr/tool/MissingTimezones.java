package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.TimeZone;

public class MissingTimezones {
    public static void main(String[] args) {
        Multimap<String, String> countryToZones = TreeMultimap.create();
        SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
        Set<String> territories = Validity.getInstance().getStatusToCodes(LstrType.zone).get(Status.regular);
        for (String territory : territories) {
            String[] zones = TimeZone.getAvailableIDs(territory);
            if (zones.length == 0) {
                countryToZones.put(territory, territory);
            } else {
                countryToZones.putAll(territory, new HashSet(Arrays.asList(zones)));
            }
        }
        for (Entry<String, String> entry : countryToZones.entries()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
    }
}
