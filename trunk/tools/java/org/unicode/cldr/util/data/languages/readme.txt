OLD: see cldr.org Upgrading Subdivision Names

childToParent.tsv
entityToLabel.tsv
entityToCode.tsv

Goto https://query.wikidata.org/ and use the following queries

childToParent.tsv
entityToLabel.tsv

SELECT DISTINCT ?parent ?child WHERE {
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
  {
    ?parent wdt:P31 wd:Q25295.
    ?child wdt:P279 ?parent.
  } UNION {
    ?child wdt:P31 wd:Q34770.
    ?child wdt:P279 ?parent.
  } UNION {
    ?parent wdt:P31 wd:Q34770.
    ?child wdt:P279 ?parent.
  }
}

entityToCode.tsv

SELECT DISTINCT ?lang ?langCode WHERE {
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
  {
      ?lang wdt:P305 ?langCode.
  }
}