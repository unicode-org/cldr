---
title: Updating Language Groups
---

1. (prerequisite: being able to build CLDR locally with [Maven](/development/maven)
2. Run GenerateLanguageContainment, through eclipse or maven.
   Here is how you can run it with Maven:
   1. cd cldr/tools
   2. mvn \-DCLDR\_DIR=*/path/to/***cldr**  \-Dexec.mainClass=org.unicode.cldr.tool.**GenerateLanguageContainment** exec:java \-pl cldr-rdf
3. This will create {workspace}/cldr/common/supplemental/languageGroup.xml
   1. Copy the console log into debugLog.txt to help in debugging problems. (Should modify tool to do this.)
   2. Run TestLanguageGroup and fix problems if necessary:
   3. OVERRIDES: If a language code moves or is deleted, consider adding override to GenerateLanguageContainment
      1. Additions go in EXTRA\_PARENT\_CHILDREN
         1. If you add something, you might have to remove it someplace else. You'll get a "duplicate parent" error in TestLanguageGroup
         2. Removals go in REMOVE\_PARENT\_CHILDREN
            1. "\*" for value means all.
   4. Example: pcm \[Nigerian Pidgin\] \[pcm\] \- not in languages/isolates.json nor languageGroup.xml
      1. Go to [https://en.wikipedia.org/wiki/Nigerian\_Pidgin](https://en.wikipedia.org/wiki/Nigerian_Pidgin) (by searching)
         2. Under language family, click on the ancestor. Keep clicking until you find a language group with an "[**ISO 639-2**](https://en.wikipedia.org/wiki/ISO_639-2) **/ [5](https://en.wikipedia.org/wiki/ISO_639-5)**" code.
         3. Get the ancestor chain (see below), we find kri
         4. Go to GenerateLanguageContainment.EXTRA\_PARENT\_CHILDREN, add .put("kri", "pcm")
   5. Example: inc \[Indic\] is not an ancestor of trw \[Torwali\]: expected true
      1. Go to [https://en.wikipedia.org/wiki/Torwali\_language](https://en.wikipedia.org/wiki/Torwali_language) (find by searching).
         1. Under language family, click on the ancestor. Keep clicking until you find a language group with an "[**ISO 639-2**](https://en.wikipedia.org/wiki/ISO_639-2) **/ [5](https://en.wikipedia.org/wiki/ISO_639-5)**" code.
         2. That says 'inc', so we have a case where wikidata is out of sync with wikipedia.
         3. Go to GenerateLanguageContainment.EXTRA\_PARENT\_CHILDREN, add .put("inc", "trw")
   6. Occasionally LanguageGroup.java will need some fixes instead, once you have done the research.
      1. Once you are done, rerun GenerateLanguageContainment and TestLanguageGroup
         1. You may need to repeat the process to get a full chain of ancestors.
         2. Example: For X Creoles, we use the X, so for the first example above we needed .put("en", "kri")
4. Run the tool **ChartLanguageGroups**
   1. Review {workspace}/../cldr-staging/docs/charts/*\<release\>*/supplemental/language\_groups.html
   2. Check in
      1. {workspace}/cldr/common/supplemental/languageGroup.xml
      2. {workspace}/cldr/tools/cldr-rdf/external/\*.tsv *( intermediate tables, for tracking)*
      3. Chart: {workspace}/../cldr-staging/docs/charts/*\<release\>*/supplemental/language\_groups.html
