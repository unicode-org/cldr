// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf;

import java.util.regex.Pattern;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.unicode.cldr.util.XPathParts;

import com.google.common.io.Files;

/**
 * an XPathMapper responsible for language display names
 * @author srl295
 */
public class CurrencyMapper implements XPathMapper {
    
    static final String O_RESOURCE = "?resource";
    static final String O_ISO = "?iso";
    static final boolean DEBUG = false;
    
    public CurrencyMapper() {
    }

    
    @Override
    public int addEntries(AbstractCache cache) throws ParseException {
        Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z][A-Z][A-Z]");
        int newAdd = 0;
        ResultSet rs = queryCurrencyResources();
        
        XPathParts xpp = XPathParts.getFrozenInstance("//ldml/numbers/currencies/currency[@type=\"XXX\"]/displayName").cloneAsThawed();
        while(rs.hasNext()) {
            final QuerySolution qs = rs.next();
            String code = QueryClient.getStringOrNull(qs, O_ISO.substring(1));
            final String res = QueryClient.getResourceOrNull(qs, O_RESOURCE.substring(1));
            if(!CURRENCY_PATTERN.matcher(code).matches()) {
                if(DEBUG) System.out.println("!!!" + rs.getRowNumber() + " - " + code + " " + res);
                continue;
            } else {
            	if(DEBUG) System.out.println("== " + rs.getRowNumber() + " - " + code + " " + res);
            }
            
            xpp.setAttribute(-2, "type", code);
            final String xpath = xpp.toString();
            if(DEBUG) System.out.println("\t"+res+"\t"+xpath);
            if(cache.add(xpath, res) == false) {
            	newAdd ++;
            }
        }
        if(DEBUG) System.out.println("Currency returned " + rs.getRowNumber() + " rows");
        return newAdd;
    }

    public static ResultSet queryCurrencyResources() throws ParseException  {
    	final String resType = O_RESOURCE;
    	final SelectBuilder builder = new SelectBuilder()
    			.addPrefix("dbp", QueryClient.PREFIX_DBP)
    			.addPrefix("dbr", QueryClient.PREFIX_DBR)
    			.addPrefix("rdf", QueryClient.PREFIX_RDF)
    			.addPrefix("dbo", QueryClient.PREFIX_DBO)
    			.addVar("*")
    			.addWhere(resType, "rdf:type", "dbo:Currency")
    			.addWhere(resType, "<"+QueryClient.PREFIX_PLG+"hypernym"+">", "dbr:Currency") // avoid "Gold as an investment" etc.
    			.addWhere(resType, "dbp:isoCode", O_ISO)
    			;
    	System.out.println(builder.buildString());
    	Query q = builder.build();
    	ResultSet results = QueryClient.getInstance().execSelect(q);
    	return results;
    }
    
    public static void main(String args[]) throws ParseException {
    	AbstractCache cache = new AbstractCache(Files.createTempDir());
		new CurrencyMapper().addEntries(cache);
    }
}
