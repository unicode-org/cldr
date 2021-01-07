package org.unicode.cldr.rdf;

import java.util.LinkedList;
import java.util.List;

import org.apache.jena.sparql.lang.sparql_11.ParseException;

public class MapAll implements XPathMapper {
	
	final List<XPathMapper> mappers = new LinkedList<>();
	
	public MapAll() {
		// add all mappers here
		mappers.add(new LanguageMapper());
		mappers.add(new ScriptMapper());
		mappers.add(new CurrencyMapper());
	}

	@Override
	public int addEntries(AbstractCache cache) throws ParseException {
		int total = 0;
		System.out.println("Begin mapping");
		for(final XPathMapper m : mappers) {
			String mapName = m.getClass().getSimpleName();
			System.out.println(mapName + " - ");
			try {
				int thisAdded = m.addEntries(cache);
				System.out.println(mapName + " + " + thisAdded);
				total += thisAdded;
			} catch(Throwable t) {
				t.printStackTrace();
				System.err.println("Problem running mapper " + mapName + " - " + t);
			}
		}
		return total;
	}
}
