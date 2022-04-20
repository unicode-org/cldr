// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.unicode.cldr.util.XPathParts;

/**
 * an XPathMapper responsible for language display names
 * @author srl295
 */
public class LanguageMapper implements XPathMapper {

    static final String O_LANGUAGE = "?language";
    static final String DBO_639_3 = "dbo:iso6393Code";
    static final String DBO_639_2 = "dbo:iso6392Code";
    static final String DBO_639_1 = "dbo:iso6391Code";
    static final boolean DEBUG = false;

    public LanguageMapper() {
    }


    @Override
    public int addEntries(AbstractCache cache) throws ParseException {
    	int newAdd = 0;
        ResultSet rs = queryLanguageResources();


        XPathParts xpp = XPathParts.getFrozenInstance("//ldml/localeDisplayNames/languages/language").cloneAsThawed();
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            String code = QueryClient.getLiteralOrNull(qs, "iso6391");
            if(code == null) {
                code = QueryClient.getLiteralOrNull(qs, "iso6392");
            }
            final String res = QueryClient.getResourceOrNull(qs, "language");
            if (code == null) {
                // SPARQL should have filtered out this entry.
                System.err.println("!!! SPARQL error: No code for " + res);
                continue;
            }
            if (code.isEmpty()) {
                if (DEBUG) {
                    System.err.println("!!! Empty code for " + res);
                }
                // This happens for https://dbpedia.org/page/Grebo_language which has a junk extra entry
                continue;
            }
            if(code.length() != 3 && code.length() != 2) {
                if(DEBUG) System.out.println("!!!" + rs.getRowNumber() + " - " + code + " " + res);
                int spaceLoc = code.indexOf(' ');
                if(!Character.isLowerCase(code.charAt(0))
                       || (spaceLoc != 2 && spaceLoc != 3)) {
                    // Happens for Igbo
                    System.err.println("Rejecting wrong-length " + code + " for " + res);
                    continue;
                }
                code = code.substring(0, spaceLoc);
                if(DEBUG) System.out.println("Fixed=> " + code);
            } else {
            	if(DEBUG) System.out.println("== " + rs.getRowNumber() + " - " + code + " " + res);
            }

            xpp.setAttribute(-1, "type", code);
            final String xpath = xpp.toString();
            if(DEBUG) System.out.println("\t"+res+"\t"+xpath);
            if(cache.add(xpath, res) == false) {
            	newAdd ++;
            }
        }
        return newAdd;
    }

    public static ResultSet queryLanguageResources() throws ParseException  {
    	final String resType = "?language";
    	final SelectBuilder builder = new SelectBuilder()
    			.addPrefix("dbo", QueryClient.PREFIX_DBO)
    			.addPrefix("dbr", QueryClient.PREFIX_DBR)
    			.addPrefix("rdf", QueryClient.PREFIX_RDF)
    			//                    .addPrefix("plg", PREFIX_PLG)
    			.addVar("*")
    			.addWhere(resType, "rdf:type", "dbo:Language")
    			.addWhere(resType, "<"+QueryClient.PREFIX_PLG+"hypernym"+">", "dbr:Language") // Only language (not dialect etc)
    			.addOptional(resType, "dbo:iso6392Code", "?iso6392")
    			.addOptional(resType, "dbo:iso6391Code", "?iso6391")
    			//                    .addOptional(resType, "dbo:iso6393Code", "?iso6393")  // Future: 639-3 code


                .addFilter("(bound(?iso6391) || bound(?iso6392))") // At least 639-1 or 639-2, or both
    			;
    	System.out.println(builder.buildString());
    	Query q = builder.build();
    	ResultSet results = QueryClient.getInstance().execSelect(q);
    	return results;
    }
}
