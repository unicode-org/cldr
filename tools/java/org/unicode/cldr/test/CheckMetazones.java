package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.XPathParts;

public class CheckMetazones extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Metazones.*
    
    XPathParts parts = new XPathParts(); // used to parse out a path
    
    private static final String[] metazonesUsingDST = { 
        "Acre", "Africa_Western", "Aktyubinsk", "Alaska", "Alaska_Hawaii", "Almaty", "Amazon",
        "America_Central", "America_Eastern", "America_Mountain", "America_Pacific", "Anadyr", 
        "Aqtau", "Aqtobe", "Arabian", "Argentina", "Argentina_Western", "Armenia", "Ashkhabad",
        "Atlantic", "Australia_Central", "Australia_CentralWestern", "Australia_Eastern", "Australia_Western",
        "Azerbaijan", "Azores", "Baku", "Bangladesh", "Bering", "Borneo", "Brasilia", "Cape_Verde",
        "Chatham", "Chile", "China", "Choibalsan", "Colombia", "Cook", "Cuba", "Dushanbe", "Easter", 
        "Europe_Central", "Europe_Eastern", "Europe_Western", "Falkland", "Fiji", "Frunze", "Georgia", 
        "Greenland_Central", "Greenland_Eastern", "Greenland_Western", "Hawaii_Aleutian", "Hong_Kong", "Hovd",
        "Iran", "Irkutsk", "Israel", "Japan", "Kamchatka", "Kizilorda", "Korea", "Krasnoyarsk", "Kuybyshev",
        "Lord_Howe", "Macau", "Magadan", "Mauritius", "Mongolia", "Moscow", "New_Caledonia", "New_Zealand",
        "Newfoundland", "Noronha", "Novosibirsk", "Omsk", "Pakistan", "Paraguay", "Peru", "Philippines",
        "Pierre_Miquelon", "Qyzylorda", "Sakhalin", "Samara", "Samarkand", "Shevchenko", "Sverdlovsk",
        "Taipei", "Tashkent", "Tbilisi", "Tonga", "Turkey", "Turkmenistan", "Uralsk", "Uruguay", "Uzbekistan",
        "Vanuatu", "Vladivostok", "Volgograd", "Yakutsk", "Yekaterinburg", "Yerevan", "Yukon" };
    private static final List<String> metazonesDSTList = Arrays.asList(metazonesUsingDST);
    
    // If you don't need any file initialization or postprocessing, you only need this one routine
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        // it helps performance to have a quick reject of most paths
      if (fullPath == null) return this; // skip paths that we don't have
      if (value == null) return this; // skip empty values
      if (path.indexOf("/metazone") < 0) return this;
        
      // we're simply going to test to make sure that metazone values don't contain any digits
        if (value.matches(".*\\p{Nd}.*")) {
            if (!getCldrFileToCheck().getSourceLocaleID(path,null).equals(getCldrFileToCheck().getLocaleID())) { // skip if inherited -- we only need parent instance
                return this;
            }
            // the following is how you signal an error or warning (or add a demo....)
            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.metazoneContainsDigit) // typically warningType or errorType
                    .setMessage("Metazone name contains digits - translate only the name")); // the message; can be MessageFormat with arguments
        }
        
        if (path.indexOf("/long") >= 0) {
            parts.set(path);
            String metazoneName = parts.getAttributeValue(3, "type");
            if (metazoneUsesDST(metazoneName)) {
                String pathPrefix = path.substring(0,path.lastIndexOf('/'));
                Set<String> foundPaths = new TreeSet<String>();
                getCldrFileToCheck().getPaths(pathPrefix , null, foundPaths);
                if (foundPaths.size() != 3) {
                    String showError;
                    if ( this.getPhase().equals(Phase.SUBMISSION)) {
                        showError = CheckStatus.warningType;
                    } else {
                        showError = CheckStatus.errorType;
                    }
                    result.add(new CheckStatus().setCause(this).setMainType(showError).setSubtype(Subtype.missingMetazoneString) // typically warningType or errorType
                        .setMessage("Missing metazone string(s) - must contain a value for generic, standard, and daylight")); // the message; can be MessageFormat with arguments
                }
                if (this.getPhase().equals(Phase.FINAL_TESTING)) {
                    Set<String> draftStatuses = new TreeSet<String>();
                    for ( String apath : foundPaths) {
                        parts.set(getCldrFileToCheck().getFullXPath(apath));
                        String draftStatus = parts.findFirstAttributeValue("draft");
                        if ( draftStatus == null ) {
                            draftStatus = "approved";
                        }
                        draftStatuses.add(draftStatus);
                    }
                    if ( draftStatuses.size() != 1 ) {
                        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.inconsistentDraftStatus) // typically warningType or errorType
                            .setMessage("Inconsistent draft status within a group")); // the message; can be MessageFormat with arguments
                    }
                }
            } else { // if this is a non-DST metazone, then only the standard string should be present, not generic or daylight
                if (path.indexOf("/standard") < 0) {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.extraMetazoneString) // typically warningType or errorType
                        .setMessage("Extra metazone string - should only contain standard value for a non-DST metazone")); // the message; can be MessageFormat with arguments                   
                }
            }
        }
        return this;
    }
    
    private boolean metazoneUsesDST(String name) {
        return metazonesDSTList.contains(name);
    }
}
