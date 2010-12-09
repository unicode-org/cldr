/*
 ******************************************************************************
 * Copyright (C) 2005, 2007 International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.util.TimeZone;

public class CheckZones extends CheckCLDR {
	//private final UnicodeSet commonAndInherited = new UnicodeSet(CheckExemplars.Allowed).complement(); 
	// "[[:script=common:][:script=inherited:][:alphabetic=false:]]");

	private TimezoneFormatter timezoneFormatter;
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFile, Map<String, String> options, List<CheckStatus> possibleErrors) {
		if (cldrFile == null) return this;
    if (Phase.FINAL_TESTING == getPhase()) {
      setSkipTest(false); // ok
    } else {
      setSkipTest(true);
      return this;
    }

    super.setCldrFileToCheck(cldrFile, options, possibleErrors);
		try {
			timezoneFormatter = new TimezoneFormatter(getResolvedCldrFileToCheck());
		} catch (RuntimeException e) {
			possibleErrors.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.cannotCreateZoneFormatter)
					.setMessage("Checking zones: " + e.getMessage()));
		}
		return this;
	}
	
	XPathParts parts = new XPathParts(null, null);
    String previousZone = new String();
    String previousFrom = new String("1970-01-01");
    String previousTo = new String("present");
    
	public CheckCLDR handleCheck(String path, String fullPath, String value,
			Map<String, String> options, List<CheckStatus> result) {
    if (fullPath == null) return this; // skip paths that we don't have
    if (path.indexOf("timeZoneNames") < 0 || path.indexOf("usesMetazone") < 0)
			return this;
		if (timezoneFormatter == null) {
      if (true) return this;
			throw new InternalCldrException("This should not occur: setCldrFileToCheck must create a TimezoneFormatter.");
		}
		parts.set(path);

                String zone = parts.getAttributeValue(3,"type");
                String from;
                if (parts.containsAttribute("from"))
		   from=parts.getAttributeValue(4,"from");
                else
                   from="1970-01-01";
                String to;
                if (parts.containsAttribute("to"))
		   to=parts.getAttributeValue(4,"to");
                else
                   to="present";
		   
                if ( zone.equals(previousZone) ) {
		   if ( from.compareTo(previousTo) < 0 ) {
				result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.multipleMetazoneMappings)
				      .setMessage("Multiple metazone mappings between {1} and {0}",
                                                   new Object[] {previousTo,from} ));
                   }
		   if ( from.compareTo(previousTo) > 0 ) {
				result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.noMetazoneMapping)
				      .setMessage("No metazone mapping between {0} and {1}",
                                                   new Object[] {previousTo,from} ));
                   }
                }
                else {
                   if ( previousFrom.compareTo("1970-01-01") != 0 ) {
				result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.noMetazoneMappingAfter1970)
				      .setMessage("Zone {0} has no metazone mapping between 1970-01-01 and {1}",
                                                   new Object[] {previousZone,previousFrom} ));
                   } 
                   if ( previousTo.compareTo("present") != 0 ) {
				result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.noMetazoneMappingBeforeNow)
				      .setMessage("Zone {0} has no metazone mapping between {1} and present.",
                                                   new Object[] {previousZone,previousTo} ));
                   }
                   previousFrom = from;
                }

                previousTo = to;
                previousZone = zone;
 
		return this;
	}

    public static String exampleTextForXpath(XPathParts parts, TimezoneFormatter timezoneFormatter, 
            String path) {
		parts.set(path);
		if (parts.containsElement("zone")) {
			String id = (String) parts.getAttributeValue(3,"type");
			TimeZone tz = TimeZone.getTimeZone(id);
			String pat = "vvvv";
			if (parts.containsElement("exemplarCity")) {
                int delim = id.indexOf('/');
                if ( delim >= 0 ) {
                    String formatted = id.substring(delim+1).replaceAll("_"," ");
                    return formatted;
                }
            } else if ( !parts.containsElement("usesMetazone") ){
               if ( parts.containsElement("generic") ) {
				pat = "vvvv";
				if (parts.containsElement("short")) pat = "v";
               } else {
				pat = "zzzz";
				if (parts.containsElement("short")) pat = "z";
               }
                boolean daylight = parts.containsElement("daylight");
                int offset = tz.getRawOffset();
                if ( daylight )
                   offset += tz.getDSTSavings();
				String formatted = timezoneFormatter.getFormattedZone(id, pat,
						daylight, offset, true);
                return formatted;
			}
		}
        return null; // unknown
    }

	public CheckCLDR handleGetExamples(String path, String fullPath, String value,
			Map options, List result) {
		if (path.indexOf("timeZoneNames") < 0) {
			return this;
        }
		if (timezoneFormatter == null) {
			throw new InternalCldrException("This should not occur: setCldrFileToCheck must create a TimezoneFormatter.");
		}
        String formatted = exampleTextForXpath(parts, timezoneFormatter, path);

		if(formatted != null) {
            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.exampleType)
                    .setMessage("Formatted value (if removed): \"{0}\"",
                new Object[] { formatted }));
        }
		return this;
	}
}
