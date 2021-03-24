// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.XPathParts;

/**
 * an XPathMapper responsible for language display names
 * @author srl295
 */
public class ScriptMapper implements XPathMapper {
    
    static final String O_RESOURCE = "?resource";
    static final String O_ISO = "?iso";
    static final boolean DEBUG = false;
    
    public ScriptMapper() {
    }

    
    @Override
    public int addEntries(AbstractCache cache) throws ParseException {
    	int newAdd = 0;
        final CLDRConfig config = CLDRConfig.getInstance();
        ResultSet rs = queryScriptResources();
        
        XPathParts xpp = XPathParts.getFrozenInstance("//ldml/localeDisplayNames/scripts/script").cloneAsThawed();
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            String code = QueryClient.getLiteralOrNull(qs, O_ISO.substring(1));
            final String res = QueryClient.getResourceOrNull(qs, O_RESOURCE.substring(1));
            if(code.length() != 4) {
                if(DEBUG) System.out.println("!!!" + rs.getRowNumber() + " - " + code + " " + res);
                continue;
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

    public static ResultSet queryScriptResources() throws ParseException  {
    	final String resType = O_RESOURCE;
    	final SelectBuilder builder = new SelectBuilder()
    			.addPrefix("dbp", QueryClient.PREFIX_DBP)
    			.addPrefix("rdf", QueryClient.PREFIX_RDF)
    			.addPrefix("yago", QueryClient.PREFIX_YAGO)
    			.addVar("*")
    			.addWhere(resType, "rdf:type", "yago:CharacterSet106488880")
    			.addWhere(resType, "dbp:iso", O_ISO)
    			;
    	System.out.println(builder.buildString());
    	Query q = builder.build();
    	ResultSet results = QueryClient.getInstance().execSelect(q);
    	return results;
    }
}
