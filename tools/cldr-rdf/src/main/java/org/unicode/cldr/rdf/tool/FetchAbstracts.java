// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf.tool;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.unicode.cldr.util.Pair;

public class FetchAbstracts {
    final static class FetchAbstractsHelper {
        private static FetchAbstracts INSTANCE = new FetchAbstracts();
    }
    
    public static final FetchAbstracts getInstance() {
        return FetchAbstractsHelper.INSTANCE;
    }
    
    public Pair<String,String> getAbstract(final String resType, final String codeKeyword, final String codeValue, final String abstractLang) {
        try {
            SelectBuilder builder = new SelectBuilder()
//                .addPrefixes(PrefixMapping.Extended)
                    .addPrefix("dbo", PREFIX_DBO)
                    .addVar("*")
                    .addWhere(resType, codeKeyword, codeValue)
                    .addWhere( resType, "dbo:abstract", "?abstract")
                    .addFilter("langMatches(lang(?abstract),\""+abstractLang+"\")")
                    ;
            
            Query q = builder.build();
            System.err.println("Query: " + codeKeyword+"="+codeValue);
            //        System.out.println(builder.buildString());
            QueryEngineHTTP qEngine = QueryExecutionFactory.createServiceRequest(SPARQL_SERVER, q);
            //        qEngine.setHttpContext(httpContext);
            ResultSet results = qEngine.execSelect();
            while(results.hasNext()) {
                QuerySolution qs = results.next();
                //            System.out.println(qs.toString());
//            for(Iterator<String> iter = qs.varNames(); iter.hasNext();) {
                //                System.out.println("v=" + iter.next());
                //            }
                String rv = qs.getLiteral("abstract").getString();
                String rs = qs.getResource(resType.substring(1)).getURI();
                //            System.out.println(rv);
                return Pair.of(rs, rv);
            }
            System.err.println(" -> no result");
            return null;
        } catch (ParseException ex) {
            Logger.getLogger(FetchAbstracts.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    public static final String DEFAULT_CLDR_SPARQL_SERVER = "https://dbpedia.org/sparql/";

    public static final String SPARQL_SERVER = System.getProperty("CLDR_SPARQL_SERVER", DEFAULT_CLDR_SPARQL_SERVER);
    
    public ResultSet queryLanguageResources() throws ParseException {
        final String resType = "?language";
            SelectBuilder builder = new SelectBuilder()
                    .addPrefix("dbo", PREFIX_DBO)
                    .addPrefix("dbr", PREFIX_DBR)
                    .addPrefix("rdf", PREFIX_RDF)
//                    .addPrefix("plg", PREFIX_PLG)
                    .addVar("*")
                    .addWhere(resType, "rdf:type", "dbo:Language")
                    .addWhere(resType, "<"+PREFIX_PLG+"hypernym"+">", "dbr:Language") // Only language (not dialect etc)
                    .addOptional(resType, "dbo:iso6392Code", "?iso6392")
                    .addOptional(resType, "dbo:iso6391Code", "?iso6391")
//                    .addOptional(resType, "dbo:iso6393Code", "?iso6393") 
                    
                    .addWhere( resType, "dbo:abstract", "?abstract")
                    .addFilter("langMatches(lang(?abstract),\""+abstractLang+"\")")
                    .addFilter("(?iso6391 || ?iso6392 "
//                            + "|| ?iso6393"
                            + ") && "
                            + "(?iso6391 != 'none' && ?iso6392 != 'none' "
//                            + "&& ?iso6393 != 'none'"
                            + ")")
//                    .setLimit(3)
                    ;
        Query q = builder.build();
        System.out.println(builder.buildString());
        QueryEngineHTTP qEngine = QueryExecutionFactory.createServiceRequest(SPARQL_SERVER, q);
        // qEngine.setHttpContext(httpContext);
        ResultSet results = qEngine.execSelect();
        return results;
    }
    public static final String PREFIX_PLG = "http://purl.org/linguistics/gold/";
    public static final String PREFIX_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String PREFIX_DBO = "http://dbpedia.org/ontology/";
    public static final String PREFIX_DBP = "http://dbpedia.org/property/";
    public static final String PREFIX_DBR = "http://dbpedia.org/resource/";
    public static final String abstractLang = "en"; // for now
    
    public static final String getStringOrNull(final QuerySolution qs, final String k) {
        final Literal l = qs.getLiteral(k);
        if(l == null) return null;
        return l.getString();
    }
    public static final String getResourceOrNull(final QuerySolution qs, final String k) {
        final Resource l = qs.getResource(k);
        if(l == null) return null;
        return l.getURI();
    }
    
    
    /**
     * Update all data
     * @param args
     * @throws ParseException
     */
    public static void main(String args[]) throws ParseException {
    	LanguageProvider.updateLanguages();
    }
}


