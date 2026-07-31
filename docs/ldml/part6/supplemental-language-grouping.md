## <a name="Supplemental_Language_Grouping" id="Supplemental_Language_Grouping" href="#Supplemental_Language_Grouping">Supplemental Language Grouping</a>

```dtd
<!ELEMENT languageGroups ( languageGroup* ) >
<!ELEMENT languageGroup ( #PCDATA ) >
<!ATTLIST languageGroup parent NMTOKEN #REQUIRED >
```

The language groups supply language containment. For example, the following indicates that aav is the Unicode language code for a language group that contains caq, crv, etc.

```xml
<languageGroup parent="fiu">chm et fi fit fkv hu izh kca koi krl kv liv mdf mns mrj myv smi udm vep vot vro</languageGroup>
```

* The vast majority of the languageGroup data is extracted from Wikidata, but may be overridden in some cases. The Wikidata information is more fine-grained, but makes use of language groups that don't have ISO or Unicode language codes. Those language groups are omitted from the data. For example, Wikidata has the following child-parent chain: only the first and last elements are present in the language groups.


| Name                      | Wikidata Code                                    | Language Code |
| ------------------------- | ------------------------------------------------ | ------------- |
| Finnish                   | [Q1412](https://www.wikidata.org/wiki/Q1412)     | fi |
| Finnic languages          | [Q33328](https://www.wikidata.org/wiki/Q33328)   |
| Finno-Samic languages     | [Q163652](https://www.wikidata.org/wiki/Q163652) |
| Finno-Volgaic languages   | [Q161236](https://www.wikidata.org/wiki/Q161236) |
| Finno-Permic languages    | [Q161240](https://www.wikidata.org/wiki/Q161240) |
| Finno-Ugric languages     | [Q79890](https://www.wikidata.org/wiki/Q79890)   | fiu |

