package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.lang.UCharacter;

public class CheckCurrencies extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Currencies.*
    
    XPathParts parts = new XPathParts(); // used to parse out a path
    
    // If you don't need any file initialization or postprocessing, you only need this one routine
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        // it helps performance to have a quick reject of most paths
      if (fullPath == null) return this; // skip paths that we don't have
      if (path.indexOf("/currency") < 0 || path.indexOf("/symbol") < 0) return this;
        
        // parts.set(path); // normally you have to parse out a path to get the exact one, but in this case the quick reject suffices
        
        // we're simply going to test the length. might do something more complicated later
        if (value.length() > 5) {
            // The following test no longer applies, choice format is not used for INR
            //if (path.indexOf("[@type=\"INR\"]") >= 0) { // skip INR, since it is typically a choice (could do more sophisticated check later)
            //    return this;
            //}
            if (!getCldrFileToCheck().getSourceLocaleID(path,null).equals(getCldrFileToCheck().getLocaleID())) { // skip if inherited -- we only need parent instance
                return this;
            }
            // Don't include Cf format chars in length test
            int adjustedLength = value.length();
            for (int idx = 0; idx < value.length(); idx++) {
                if ( UCharacter.getType(value.charAt(idx)) == UCharacter.FORMAT ) {
                    if ( --adjustedLength <= 5 ) {
                         return this;
                    }
                }
            }

            // the following is how you signal an error or warning (or add a demo....)
            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.currencySymbolTooWide) // typically warningType or errorType
                    .setMessage("Currency symbol length > 5")); // the message; can be MessageFormat with arguments
        }
        return this;
    }
}
