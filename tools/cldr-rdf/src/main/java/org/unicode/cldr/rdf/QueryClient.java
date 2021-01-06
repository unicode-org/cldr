// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.rdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.apache.jena.ext.com.google.common.io.Resources;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.unicode.cldr.util.Timer;

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

    public static final String DEFAULT_CLDR_DBPEDIA_SPARQL_SERVER = "https://dbpedia.org/sparql/";
    public static final String DBPEDIA_SPARQL_SERVER = System.getProperty("CLDR_DBPEDIA_SPARQL_SERVER", DEFAULT_CLDR_DBPEDIA_SPARQL_SERVER);

    public static final String DEFAULT_CLDR_WIKIDATA_SPARQL_SERVER = "https://query.wikidata.org/sparql";
    public static final String WIKIDATA_SPARQL_SERVER = System.getProperty("CLDR_WIKIDATA_SPARQL_SERVER", DEFAULT_CLDR_WIKIDATA_SPARQL_SERVER);

    public static final String PREFIX_PLG = "http://purl.org/linguistics/gold/";
    public static final String PREFIX_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String PREFIX_DBO = "http://dbpedia.org/ontology/";
    public static final String PREFIX_DBP = "http://dbpedia.org/property/";
    public static final String PREFIX_DBR = "http://dbpedia.org/resource/";
    public static final String PREFIX_YAGO = "http://dbpedia.org/class/yago/";
    public static final String abstractLang = "en"; // for now

    public static final String getLiteralOrNull(final QuerySolution qs, final String k) {
        final Literal l = qs.getLiteral(k);
        return getLiteralOrNull(l);
    }

    private static String getLiteralOrNull(final Literal l) {
        if(l == null) return null;
        return l.getString();
    }

    /**
     * Convert the specified parameter into a String, or null if not found.
     * @param qs
     * @param k
     * @return
     */
    public static final String getStringOrNull(final QuerySolution qs, final String k) {
        RDFNode node = qs.get(k);
        return getStringOrNull(node);
    }

    private static String getStringOrNull(RDFNode node) {
        if(node == null) {
            return null; // not found
        } else if(node.isLiteral()) {
            return getLiteralOrNull(node.asLiteral());
        } else if(node.isResource()) {
            return getResourceOrNull(node.asResource());
        } else {
            throw new UnsupportedOperationException("Not supported: node type " + node.toString());
        }
    }

    public static final String getResourceOrNull(final QuerySolution qs, final String k) {
        final Resource l = qs.getResource(k);
        return getResourceOrNull(l);
    }
    private static String getResourceOrNull(final Resource l) {
        if(l == null) return null;
        return l.getURI();
    }

    /**
     * Run a query
     * @param q
     * @return
     */
    public ResultSet execSelect(Query q) {
        return execSelect(q, QueryClient.DBPEDIA_SPARQL_SERVER);
    }

    public ResultSet execSelect(Query q, final String server) {
        Timer t = new Timer();
        QueryEngineHTTP qEngine = QueryExecutionFactory.createServiceRequest(server, q);
        // qEngine.setHttpContext(httpContext);
        ResultSet results = qEngine.execSelect();
        System.out.println("SparQL query complete in " + t);
        return results;
    }

    /**
     * @param resName resource such as "wikidata-childToParent.sparql"
     * @param server server name
     * @return
     * @throws IOException 
     */
    public ResultSet execSelectFromSparql(final String resName, final String server) throws IOException {
        final Query q = loadSparql(resName);
        return execSelect(q, server);
    }

    /**
     * A little routine to dump a ResultSet out to the command line.
     * Note that it is destructive to the ResultSet, so can't be combined with other processing.
     * @param rs
     */
    public static void dumpResults(ResultSet rs) {
        System.out.println("RESULTS:" + rs.getResultVars());
        for(;rs.hasNext();) {
            QuerySolution qs = rs.next();
            Iterator<String> vn = qs.varNames();
            for(;vn.hasNext();) {
                final String k = vn.next();
                System.out.println(k+"="+qs.get(k).toString());
            }
            System.out.println();
        }
    }
    /**
     * Load a query from a resource
     * @param resName
     * @return
     * @throws IOException
     */
    public static Query loadSparql(final String resName) throws IOException {
        final String str = Resources.toString(Resources.getResource(QueryClient.class, "sparql/"+resName+".sparql"), StandardCharsets.UTF_8);  
        return QueryFactory.create(str);
    }
}


