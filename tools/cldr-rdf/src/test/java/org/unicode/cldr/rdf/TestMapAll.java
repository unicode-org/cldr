package org.unicode.cldr.rdf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

@Testable
public class TestMapAll {
	private static final String TEST_XPATH = "//ldml";
	private static final String TEST_URL = "http://cldr.unicode.org";
	public static final boolean CLDR_TEST_ENABLE_NET = Boolean.parseBoolean(System.getProperty("CLDR_TEST_ENABLE_NET", "false"));

	@Test
	void TestNoMappings() throws IOException {
		File root = Files.createTempDirectory("rdf_TestMapAll_TestNoMappings").toFile();
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
	void TestAllMappings() throws ParseException, IOException {
		assumeTrue(CLDR_TEST_ENABLE_NET, "CLDR_TEST_ENABLE_NET not true, not attempting network read");

		File root = Files.createTempDirectory("rdf_TestMapAll_TestAllMappings").toFile();
		AbstractCache tmpCache = new AbstractCache(root);

		int count = new MapAll().addEntries(tmpCache);
		assertNotEquals(0, count, "added count of net entries");

		// write the cache here, for debugging.
		tmpCache.store();

		// Spot
		assertAll("Spot Checks",
				() -> assertLanguage(tmpCache, "fr", "http://dbpedia.org/resource/French_language"),
				() -> assertLanguage(tmpCache, "co", "http://dbpedia.org/resource/Corsican_language"),
				() -> assertLanguage(tmpCache, "ace", "http://dbpedia.org/resource/Acehnese_language"));
	}

	void assertLanguage(AbstractCache cache, String code, String url) {
		assertUrl(cache, "//ldml/localeDisplayNames/languages/language[@type=\"" + code + "\"]", url);
	}

	void assertUrl(AbstractCache cache, String xpath, String url) {
		assertEquals(url, cache.get(xpath),
			() -> xpath.substring(xpath.lastIndexOf('/'))); // leaf element only
	}

}
