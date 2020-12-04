package org.unicode.cldr.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;

import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import com.google.common.io.Files;

@Testable
public class TestMapAll {
	private static final String TEST_XPATH = "//ldml";
	private static final String TEST_URL = "http://cldr.unicode.org";
	public static final boolean CLDR_TEST_ENABLE_NET = Boolean.parseBoolean(System.getProperty("CLDR_TEST_ENABLE_NET", "false"));
	
	@Test
	void TestNoMappings() {
		File root = Files.createTempDir();
		AbstractCache tmpCache = new AbstractCache(root);
		assertEquals(0,  tmpCache.size(), "Cache should be empty when created.");
		assertNull(tmpCache.get("//cldr"));

		assertFalse(tmpCache.add(TEST_XPATH, TEST_URL), "expected this value was not already there");
		assertTrue(tmpCache.add(TEST_XPATH, TEST_URL), "expected this value was already there");
		
		assertEquals(TEST_URL, tmpCache.get(TEST_XPATH));
		
		{
			AbstractCache cache2 = new AbstractCache(root);
			assertEquals(0, cache2.size(), "Cache was not written yet");
			assertNull(cache2.get("//cldr"));
			assertNull(cache2.get(TEST_XPATH));
		}
		
		tmpCache.store();
		
		assertEquals(TEST_URL, tmpCache.get(TEST_XPATH));
		
		{
			AbstractCache cache3 = new AbstractCache(root);
			assertEquals(1, cache3.size(), "Cache was written and was re-read");
			assertNull(cache3.get("//cldr"));
			assertEquals(TEST_URL, cache3.get(TEST_XPATH));
		}
	}
	
	
	
	@Test
	void TestAllMappings() throws ParseException {
		assumeTrue(CLDR_TEST_ENABLE_NET, "CLDR_TEST_ENABLE_NET not true, not attempting network read");
		
		File root = Files.createTempDir();
		AbstractCache tmpCache = new AbstractCache(root);

		int count = new MapAll().addEntries(tmpCache);
		assertNotEquals(0, count, "added count of net entries");
		
		// Spot check
		
		assertEquals("http://dbpedia.org/resource/French_language", tmpCache.get("//ldml/localeDisplayNames/languages/language[@type=\"fr\"]"));
		
		tmpCache.store();
	}

}
