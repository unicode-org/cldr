package org.unicode.cldr.rdf;

import org.apache.jena.sparql.lang.sparql_11.ParseException;

/**
 * An interface for classes capable of mapping from an XPath to a Resource URI
 * where the Resource can provide an abstract
 * 
 * @author srl295
 *
 */
public interface XPathMapper {

	/**
	 * Add all entries to the cache
	 * @param cache
	 * @return number of new entries, or zero
	 * @throws ParseException
	 */
	int addEntries(AbstractCache cache) throws ParseException;

}
