// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf.tool;

import java.util.List;
import org.unicode.cldr.util.abstracts.AbstractResult;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.abstracts.AbstractCache;

/**
 *
 * @author srl295
 */
public class LanguageProvider  {
    
    static final String O_LANGUAGE = "?language";
    static final String DBO_639_3 = "dbo:iso6393Code";
    static final String DBO_639_2 = "dbo:iso6392Code";
    static final String DBO_639_1 = "dbo:iso6391Code";

    
    public static void main(String args[]) throws Throwable {
        System.out.println("Begin update of Languages");
        updateLanguages();
    }
    
    public static void updateLanguages() throws ParseException {
        AbstractCache cache = AbstractCache.getInstance();
        final CLDRConfig config = CLDRConfig.getInstance();
        CLDRFile english = config.getEnglish();
        CoverageInfo ci = config.getCoverageInfo();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();

        ResultSet rs = FetchAbstracts.getInstance().queryLanguageResources();
        
        
        final List<String> vars = rs.getResultVars();
        XPathParts xpp = XPathParts.getFrozenInstance("//ldml/localeDisplayNames/languages/language").cloneAsThawed();
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            String code = FetchAbstracts.getStringOrNull(qs, "iso6391");
            if(code == null) {
                code = FetchAbstracts.getStringOrNull(qs, "iso6392");
            }
            final String res = FetchAbstracts.getResourceOrNull(qs, "language");
            final String abs = FetchAbstracts.getStringOrNull(qs, "abstract");
            if(code.length() != 3 && code.length() != 2) {
                System.out.println("!!!" + rs.getRowNumber() + " - " + code + " " + res + " (" + abs.length() +" chars)");
                int spaceLoc = code.indexOf(' ');
                if(!Character.isLowerCase(code.charAt(0))
                       || (spaceLoc != 2 && spaceLoc != 3)) {
                    System.err.println("Rejecting: " + code + " for " + res);
                    continue;
                }
                code = code.substring(0, spaceLoc);
                System.out.println("Fixed=> " + code);
            } else {
                System.out.println("== " + rs.getRowNumber() + " - " + code + " " + res + " (" + abs.length() +" chars)");
            }
            
            xpp.setAttribute(-1, "type", code);
            final String xpath = xpp.toString();
            Level cl = ci.getCoverageLevel(xpath, "en");
            if(cl.getLevel() >= Level.COMPREHENSIVE.getLevel()) {
                 System.out.println("SKIP:\t"+cl+"\t"+xpath);
                 continue;
            }
            System.out.println("\t"+cl+"\t"+xpath);
//            System.out.println();
            cache.add(xpath, new AbstractResult(Pair.of(res, abs)));
        }
        cache.store();
    }
}
