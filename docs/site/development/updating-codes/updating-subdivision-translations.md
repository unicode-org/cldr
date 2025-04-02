---
title: Updating Subdivision Translations
---

# Updating Subdivision Translations

1. Make sure that that the subdivisions are updated first as per [Updating Subdivision Codes](/development/updating-codes/updating-subdivision-codes)
2. Make sure you have completed [Maven Setup](/development/maven)
3. Run tool WikiSubdivisionLanguages
4. ~~mvn \-DCLDR\_DIR\=**\_\_\_\_\_\_\_\_/cldr**\-Dexec.mainClass\=org.unicode.cldr.tool.GenerateLanguageContainment exec:java \-pl cldr\-rdf~~
	1. STEVEN LOOMIS 2022\-0829 \- this does not make sense here.
5. Sanity check result, run tests.

### NOTES
1. Should only add values, never change what is there beforehand.
	1. Currently excludes items:
		1. That fail exemplar check (broad test, allows any letters in script).
		2. Many of these are reparable, but need manual work.
	2. Currently renames items that collide *within country*.
		1. Uses superscript 2, 3 for alternates. More than 3 alternates, it excludes since there is probably a more serious problem.
	3. Needs a couple more locales: zh\_Hant, de\_CH, fil not working yet.
	4. The Language List is in the query file **{workspace}cldr/tools/cldr\-rdf/src/main/resources/org/unicode/cldr/rdf/sparql/wikidata\-wikisubdivisionLanguages.sparql**
2. Check in
	1. Make sure you also check in **{workspace}/cldr/tools/cldr\-rdf/external/\*.tsv** ( intermediate tables, for tracking)

