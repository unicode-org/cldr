## <a name="Features" id="Features" href="#Features">Features</a>

```dtd
<!ELEMENT grammaticalData ( grammaticalFeatures*, grammaticalDerivations*) >
<!ELEMENT grammaticalFeatures ( grammaticalCase*, grammaticalGender*, grammaticalDefiniteness* ) >
<!ATTLIST grammaticalFeatures targets NMTOKENS #REQUIRED >
<!ATTLIST grammaticalFeatures locales NMTOKENS #REQUIRED >

<!ELEMENT grammaticalCase EMPTY>
<!ATTLIST grammaticalCase scope NMTOKENS #IMPLIED >
<!ATTLIST grammaticalCase values NMTOKENS #REQUIRED >

<!ELEMENT grammaticalGender EMPTY>
<!ATTLIST grammaticalGender scope NMTOKENS #IMPLIED >
<!ATTLIST grammaticalGender values NMTOKENS #REQUIRED >

<!ELEMENT grammaticalDefiniteness EMPTY>
<!ATTLIST grammaticalDefiniteness scope NMTOKENS #IMPLIED >
<!ATTLIST grammaticalDefiniteness values NMTOKENS #REQUIRED >
```

* The @targets attribute contains the specific grammatical entities to which the features apply, such as ```nominal``` when they apply to nouns only. The @locales attribute contains the specific locales to which the features apply, such as ```de fr``` for German and French.


* The @scope attribute, if present, indicates that the values are limited to a specific subset for certain kinds of entities. For example, a particular language might have an animate gender for nouns, but no units of measurement ever have that case; in another language, the language might have a rich set of grammatical cases, but units are invariant. If the @scope attribute is not present, then that has the meaning of "everything else".


* The @scope attributes are targeted at messages created by computers, thus a feature may have a narrower scope if for all practical purposes the feature value is not used in messages created by computers. For example, it may be possible in theory for a kilogram to be in the vocative case (English poetry might have “O Captain! my Captain!/ our fearful trip is done”, but on computers you have little call to need the message “O kilogram! my kilogram! …”).


**Constraints:**

* a scope attribute is only used when there is a corresponding “general” element, one for the same language and target without a scope attribute.
* the scope attribute values must be narrower (a proper subset, possibly empty) of those in the corresponding general element.

### <a name="Gender" id="Gender" href="#Gender">Gender</a>

Feature that classifies nouns in classes.
This is grammatical gender, which may be assigned on the basis of sex in some languages, but may be completely separate in others.
Also used to tag elements in CLDR that should agree with a particular gender of an associated noun.
* **adapted from: [linguistics-ontology.org/gold/2010/GenderProperty](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/GenderProperty**: (adapted from: [linguistics-ontology.org/gold/2010/GenderProperty](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/GenderProperty) - the links below go to an archived version. The original site is no longer available, as explained at <https://linguistlist.org/gold/>.)


The term "gender" is somewhat of a misnomer, because CLDR treats "gender" as a broad term, equivalent to "noun class".
Thus it bundles noun class categories such as gender and animacy into a single identifier, such as "feminine-animate".

#### <a name="Example" id="Example" href="#Example">Example</a>

```xml
<grammaticalFeatures targets="nominal" locales="es fr it pt">
   <grammaticalGender values="masculine feminine"/>
```

#### <a name="Table_Values" id="Table_Values" href="#Table_Values">Table: Values</a>

| Value     | Definition | References |
| --------- | ---------- | ---------- |
| animate   | In an animate/inanimate gender system, gender that denotes human or animate entities. | description adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/AnimateGender](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/AnimateGender) |
| inanimate | In an animate/inanimate gender system, gender that denotes object or inanimate entities .| adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/InanimateGender](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/InanimateGender) |
| personal  | In an animate/inanimate gender system in some languages, gender that specifies the masculine gender of animate entities. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/HumanGender](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/HumanGender) |
| common    | In a common/neuter gender system, gender that denotes human entities. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender) |
| feminine  | In a masculine/feminine or in a masculine/feminine/neuter gender system, gender that denotes specifically female persons (or animals) or that is assigned arbitrarily to object. | adapted from: https://en.wikipedia.org/wiki/Grammatical_gender, [linguistics-ontology.org/gold/2010/FeminineGender](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/FeminineGender) |
| masculine | In a masculine/feminine or in a masculine/feminine/neuter gender system, gender that denotes specifically male persons (or animals) or that is assigned arbitrarily to object. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/MasculineGender](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/MasculineGender) |
| neuter    | In a masculine/feminine/neuter or common/neuter gender system, gender that generally denotes an object. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/NeuterGender](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/NeuterGender) |

There are further simplifications in the identifiers.
For example, consider a language that has 3 genders, and two levels of animacy, but only for masculine.
The set of combinations would be:

* masculine-animate
* masculine-inanimate
* feminine-unspecified
* neuter-unspecified

In such a case as this, CLDR abbreviates these as the following identifiers:

* masculine
* inanimate
* feminine
* neuter

That is:
* unspecified and animate are dropped.
* if there is only a single gender with inanimate, then the gender is dropped.

### <a name="Case" id="Case" href="#Case">Case</a>

#### <a name="Table_Case" id="Table_Case" href="#Table_Case">Table: Case</a>

* Feature that encodes the syntactic (and sometimes semantic) relationship of a noun with the other constituents of the sentence. (adapted from [linguistics-ontology.org/gold/2010/CaseProperty](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/CaseProperty))


#### <a name="Example" id="Example" href="#Example">Example</a>

```xml
<grammaticalFeatures targets="nominal" locales="de">
   <grammaticalCase values="nominative accusative genitive dative"/>
```

###### <a name="Table_Values" id="Table_Values" href="#Table_Values">Table: Values</a>

| Value              | Definition | References |
| ------------------ | ---------- | ---------- |
| abessive          | The abessive case expresses the absence of the referent it marks. It has the meaning of 'without'. | [purl.org/olia/olia.owl#AbessiveCase](https://purl.org/olia/olia.owl#AbessiveCase) [linguistics-ontology.org/gold/2010/AbessiveCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/AbessiveCase)|
| ablative           | The ablative case expresses that the referent of the noun it marks is the location from which another referent is moving. It has the meaning 'from'. | [purl.org/olia/olia.owl#AblativeCase](https://purl.org/olia/olia.owl#AblativeCase), [linguistics-ontology.org/gold/2010/AblativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/AblativeCase) |
| accusative         | Accusative case marks certain syntactic functions, usually direct objects. | [purl.org/olia/olia.owl#Accusative](https://purl.org/olia/olia.owl#Accusative), [linguistics-ontology.org/gold/2010/AccusativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/AccusativeCase) |
| adessive  | The adessive case expresses that the referent of the noun it marks is the location near/at which another referent exists. It has the meaning of 'at' or 'near'. | [purl.org/olia/olia.owl#AdessiveCase](https://purl.org/olia/olia.owl#AdessiveCase), [linguistics-ontology.org/gold/2010/AdessiveCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/AdessiveCase) |
| allative | The allative case expresses motion to or toward the referent of the noun it marks. | [purl.org/olia/olia.owl#AllativeCase](https://purl.org/olia/olia.owl#AllativeCase), [linguistics-ontology.org/gold/2010/AllativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/AllativeCase) |
| causal | The causal (causal-final, not causative) case expresses that the marked noun is the objective or reason for something. It carries the meaning of 'for the purpose of'. | https://en.wikipedia.org/wiki/Causative#Causal-final_case, http://www.hungarianreference.com/Nouns/%C3%A9rt-causal-final.aspx |
| comitative         | Comitative Case expresses accompaniment. It carries the meaning 'with' or 'accompanied by' . | [purl.org/olia/olia.owl#ComitativeCase](https://purl.org/olia/olia.owl#ComitativeCase), [linguistics-ontology.org/gold/2010/ComitativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/ComitativeCase) |
| dative             | Dative case marks indirect objects (for languages in which they are held to exist), or nouns having the role of a recipient (as of things given), a beneficiary of an action, or a possessor of an item. | [purl.org/olia/olia.owl#DativeCase](https://purl.org/olia/olia.owl#DativeCase), [linguistics-ontology.org/gold/2010/DativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/DativeCase) |
| delative | The delative case expresses motion downward from the referent of the noun it marks. | [purl.org/olia/olia.owl#DelativeCase](https://purl.org/olia/olia.owl#DelativeCase), [linguistics-ontology.org/gold/2010/DelativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/DelativeCase) |
| elative | The elative case expresses that the referent of the noun it marks is the location out of which another referent is moving. It has the meaning 'out of'. | [purl.org/olia/olia.owl#ElativeCase](https://purl.org/olia/olia.owl#ElativeCase), [linguistics-ontology.org/gold/2010/ElativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/ElativeCase) |
| ergative           | In ergative-absolutive languages, the ergative case identifies the subject of a transitive verb. | [purl.org/olia/olia.owl#ErgativeCase](https://purl.org/olia/olia.owl#ErgativeCase), [linguistics-ontology.org/gold/2010/ErgativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/ErgativeCase) |
| essive | The essive case expresses that the referent of the noun it marks is the location at which another referent exists. | [purl.org/olia/olia.owl#EssiveCase](https://purl.org/olia/olia.owl#EssiveCase), [linguistics-ontology.org/gold/2010/EssiveCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/EssiveCase) |
| genitive           | Genitive case signals that the referent of the marked noun is the possessor of the referent of another noun, e.g. "the man's foot". In some languages, genitive case may express an associative relation between the marked noun and another noun. | [purl.org/olia/olia.owl#GenitiveCase](https://purl.org/olia/olia.owl#GenitiveCase), [linguistics-ontology.org/gold/2010/GenitiveCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/GenitiveCase) |
| illative | The illative case expresses that the referent of the noun it marks is the location into which another referent is moving. It has the meaning 'into'. | [purl.org/olia/olia.owl#IllativeCase](https://purl.org/olia/olia.owl#IllativeCase), [linguistics-ontology.org/gold/2010/IllativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/IllativeCase) |
| inessive  | The inessive case expresses that the referent of the noun it marks is the location within which another referent exists. It has the meaning of 'within' or 'inside'.  | [purl.org/olia/olia.owl#InessiveCase](https://purl.org/olia/olia.owl#InessiveCase), [linguistics-ontology.org/gold/2010/InessiveCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/InessiveCase) |
| instrumental       | The instrumental case indicates that the referent of the noun it marks is the means of the accomplishment of the action expressed by the clause. | [purl.org/olia/olia.owl#InstrumentalCase](https://purl.org/olia/olia.owl#InstrumentalCase), [linguistics-ontology.org/gold/2010/InstrumentalCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/InstrumentalCase) |
| locative           | Case that indicates a final location of action or a time of the action. | [purl.org/olia/olia.owl#LocativeCase](https://purl.org/olia/olia.owl#LocativeCase), [linguistics-ontology.org/gold/2010/LocativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/LocativeCase) |
| locativecopulative | Copulative Case marker that indicates a location. | TBD Add reference, example |
| nominative         | In nominative-accusative languages, nominative case marks clausal subjects and is applied to nouns in isolation | [purl.org/olia/olia.owl#Nominative](https://purl.org/olia/olia.owl#Nominative), [linguistics-ontology.org/gold/2010/NominativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/NominativeCase) |
| oblique            | Case that is used when a noun is the object of a verb or a proposition, except for nominative and vocative case. | [purl.org/olia/olia.owl#ObliqueCase](https://purl.org/olia/olia.owl#ObliqueCase) |
| partitive          | The partitive case is a grammatical case which denotes 'partialness', 'without result', or 'without specific identity'. | [purl.org/olia/olia.owl#PartitiveCase](https://purl.org/olia/olia.owl#PartitiveCase), [linguistics-ontology.org/gold/2010/PartitiveCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/PartitiveCase) |
| prepositional      | Prepositional case refers to case marking that only occurs in combination with prepositions. | [purl.org/olia/olia.owl#PrepositionalCase](https://purl.org/olia/olia.owl#PrepositionalCase) |
| sociative          | Case related to the person in whose company the action is carried out, or to any belongings of people which take part in the action. | [purl.org/olia/olia.owl#SociativeCase](https://purl.org/olia/olia.owl#SociativeCase) |
| sublative  | The sublative case expresses that the referent of the noun it marks is the location under which another referent is moving toward. It has the meaning 'towards the underneath of'. | [purl.org/olia/olia.owl#SublativeCase](https://purl.org/olia/olia.owl#SublativeCase), [linguistics-ontology.org/gold/2010/SublativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/SublativeCase) |
| superessive  | The superessive case expresses that the referent of the noun it marks is the location on which another referent exists. It has the meaning of 'on' or 'upon'. | [purl.org/olia/olia.owl#SuperessiveCase](https://purl.org/olia/olia.owl#SuperessiveCase), [linguistics-ontology.org/gold/2010/SuperessiveCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/SuperessiveCase) |
| terminative  | The terminative case expresses the motion of something into but not further than (ie, not through) the referent of the noun it marks. It has the meaning 'into but not through'.  | [purl.org/olia/olia.owl#TerminativeCase](https://purl.org/olia/olia.owl#TerminativeCase), [linguistics-ontology.org/gold/2010/TerminativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/TerminativeCase) |
| translative  | The translative case expresses that the referent of the noun that it marks is the result of a process of change. It has the meaning of 'becoming' or 'changing into'.  | [purl.org/olia/olia.owl#TranslativeCase](https://purl.org/olia/olia.owl#TranslativeCase), [linguistics-ontology.org/gold/2010/TranslativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/TranslativeCase) |
| vocative           | Vocative case marks a noun whose referent is being addressed. | [purl.org/olia/olia.owl#VocativeCase](https://purl.org/olia/olia.owl#VocativeCase), [linguistics-ontology.org/gold/2010/VocativeCase](https://web.archive.org/20200115041525/linguistics-ontology.org/gold/2010/VocativeCase) |

### <a name="Definiteness" id="Definiteness" href="#Definiteness">Definiteness</a>

* Feature that encodes the fact that a noun has been already mentioned, or is familiar in the discourse. (adapted from [https://glossary.sil.org/term/definiteness](https://glossary.sil.org/term/definiteness))


#### <a name="Table_Values" id="Table_Values" href="#Table_Values">Table: Values</a>

| Value       | Definition | References |
| ----------- | ---------- | ---------- |
| definite    | Value referring to the capacity of identification of an entity. | [purl.org/olia/olia.owl#Definite](https://purl.org/olia/olia.owl#Definite) |
| indefinite  | An entity is specified as indefinite when it refers to a non-particularized individual of the species denoted by the noun. | [purl.org/olia/olia.owl#Indefinite](https://purl.org/olia/olia.owl#Indefinite) |
| construct   | The state of the first noun in a genitive phrase of a possessed noun followed by a possessor noun. | Not directly linked, but explained under: [purl.org/olia/olia-top.owl#DefinitenessFeature](https://purl.org/olia/olia-top.owl#DefinitenessFeature) |
| unspecified | Noun without any definiteness marking in some specific construction (specific to Danish). |   |


