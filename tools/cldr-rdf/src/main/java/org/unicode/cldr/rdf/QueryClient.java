// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.lang.sparql_11.ParseException;

/**
 * Class to aid in SPARQL queries
 * @author srl295
 *
 */
public class QueryClient {
    final static class QueryClientHelper {
        private static QueryClient INSTANCE = new QueryClient();
    }
    
    public static final QueryClient getInstance() {
        return QueryClientHelper.INSTANCE;
    }
    
    public static final String DEFAULT_CLDR_SPARQL_SERVER = "https://dbpedia.org/sparql/";

    public static final String SPARQL_SERVER = System.getProperty("CLDR_SPARQL_SERVER", DEFAULT_CLDR_SPARQL_SERVER);
    
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
     * Run a query
     * @param q
     * @return
     */
	public ResultSet execSelect(Query q) {
		QueryEngineHTTP qEngine = QueryExecutionFactory.createServiceRequest(QueryClient.SPARQL_SERVER, q);
    	// qEngine.setHttpContext(httpContext);
    	ResultSet results = qEngine.execSelect();
		return results;
	}

}


