## <a name="namePattern_Syntax" id="namePattern_Syntax" href="#namePattern_Syntax">namePattern Syntax</a>

* **A _namePattern_ is**: A _namePattern_  is composed of a sequence of field IDs, each enclosed in curly braces, and separated by zero or more literal characters (eg, space or comma + space). An Extended Backus Normal Form (EBNF) is used to describe the namePattern format for a specific set of attributes. It has the following structure. This is the `( #PCDATA )` reference in the element specification above.


|              | EBNF                          | Comments |
| ------------ | ----------------------------- | -------- |
| namePattern  | = literal?<br/><span style="white-space:nowrap">( modField  literal? )+;</span> | Two literals cannot be adjacent |
| modField     | <span style="white-space:nowrap">= '{' field modifierList? '}';</span> | A name field, optionally modified |
| field        | = 'title'<br/>\| 'given'<br/>\| 'given2'<br/>\| 'surname'<br/>\| 'surname2'<br/>\|  'generation'<br/>\| 'credentials' ; | See [Fields](#fields) |
| modifierList | = '-informal'?<br/><span style="white-space:nowrap">( '-allCaps' \| ‘-initialCap' )?;</span><br/><span style="white-space:nowrap">( '-initial'  \| '-monogram' )?</span><br/><span style="white-space:nowrap">( '-prefix' \| '-core' )?</span> | Optional modifiers that can be applied to name parts, see [Modifiers](#modifiers). Note that some modifiers are exclusive: only `prefix` or `core`, only `initial` or `monogram`, only `allCaps` or `initialCap`. |
| literal      | = codepoint+ ; | One or more Unicode codepoints. |

### <a name="Fields" id="Fields" href="#Fields">Fields</a>

* The Person Name formatting data assumes that the name data to be formatted consists of the fields in the table below. All of the fields may contain multiple words. Field IDs are lowercase ASCII alphanumeric, and start with an alphabetic character.


* When determining how a full name is to be placed into name fields, the data to be formatted should be organized functionally. That is, if a name part is on the dividing line between `given2` and `given`, the key feature is whether it would always occur with the rest of the given name. For example, in _“Mary Jean Smith”_, if _“Mary”_ never occurs without the _“Jean”_, then the given name should be _“Mary Jean”_. If _“Smith”_ never occurs without the _“Jean”_, the `surname` should be _“Jean Smith”_. Otherwise, _“Jean”_ would be the `given2` field.


For example, a patronymic would be treated as a `given2` name in most slavic languages.

* In some cultures, two surnames are used to indicate the paternal and maternal family names or generational names indicating father, grandfather. The `surname2` field is used to indicate this. The CLDR PersonName formatting data assumes that if a PersonName object to be formatted does not have two surnames, then the `surname2` field is not populated. (That is, no pattern should have a `surname2` field without a surname field.) Order of fields in a pattern can vary arbitrarily by locale.


* In most cultures, there is a concept of nickname or preferred name, which is used in informal settings or sometimes to represent a “public” or “stage name”. The nickname or preferred name may be submitted as a separate PersonName object to be formatted, or included with a modifier such as `given-informal`.


| Field      | Description<br/>Note: The values for each are as supplied by the PersonName object, via the PersonName data interface. |
| ---------- | ----------- |
| `title`   | A title or honorific qualifier.<br/>Example: ‘Ms.’, ‘Mr.’, ’Dr’, ‘President’<br/><br/>Note that CLDR PersonName formats data does not define regional or locale-specific lists of titles or honorifics such as “Mr”, “Ms”, “Mx”, “Prof”, etc. |
| `given`    | The “given” name. Can be multiple words such as “Mary Ann”.<br/>Examples:  “Janus”, “Mary Jean”, or “Jean-Louis”|
| `given2`   | Additional given name or names or middle name, usually names(s) written between the given and surname. Can be multiple words. In some references, also known as a “second” or “additional” given name or patronymic. This field is separate from the “given” field because it is often optional in various presentation forms.<br/>Examples:  “Horatio Wallace” as in<br/>`{ given: "Janus", `**`given2: "Horatio Wallace"`**`, surname: "Young" }`<br/><br/>“S.” as in “Harry S. Truman”. Yes, his full middle name was legally just “S.”.|
| `surname`  | The “family name”. Can be more than one word.<br/><br/>Example: “van Gogh” as in<br/>`{ given: "Vincent", given2: "Willem", `**`surname: "van Gogh"`**` }`<br/><br/>Other examples: “Heathcote-Drummond-Willoughby” as in “William Emanuel Heathcote-Drummond-Willoughby III”|
| `surname2` | Secondary surname (used in some cultures), such as second or maternal surname in Mexico and Spain. This field is separate from the “surname” field because it is often optional in various presentation forms, and is considered a separate distinct name in some cultures.<br/><br/>Example: “Barrientos” in “Diego Rivera Barrientos”;<br/>`{ given: "Diego", surname: "Rivera", `**`surname2: "Barrientos"`**` }`<br/><br/>Example: if "Mary Jane Smith" moves to Spain the new name may be<br/>`{ given: "Mary", given2: "Jane", surname: "Smith", `**`surname2: "Jones"`**` }`|
| `credentials`   | A credential or accreditation qualifier.<br/>Example: “PhD”, “MBA”<br/><br/>Example: “Salvatore Jarvis MBA”<br/>`{ given: "Salvatore", given2: "Blinken", surname: "Jarvis", `**`credentials: "MBA"`**` }`<br/><br/>An alternate PersonName object may be presented for formatting using the “stage” name from the application’s data:<br/>`{ given: "Salvatore", given-informal: "Salvatore", given2: "", surname: "Jarvis", `**`credentials: "MBA"`**` }` |
| `generation`   | A generation qualifier.<br/>Example: “III”, “Jr.”<br/><br/>Example: “Sonny Jarvis Jr.”<br/>`{ given: "Salvatore", given2: "Blinken", surname: "Jarvis", `**`generation: "Jr."`**` }` |

Some other examples:

* British name: _John Ronald Reuel Tolkien_: `given` name is "John", `given2` name would be  "Ronald Reuel", and the `surame` is "Tolkien".
* Dutch name: _Anneliese Louise van der Pol_: `given` name: "Anneliese", `given2` name: "Louise", `surname`: "van der Pol"
    * Also surname-prefix: “van der”, surname-core: “Pol” — see below.
* French name: “Jean-Louis Trintignant” would _not_ be Jean (`given`) Louis (`given2`) Trintignant (`surname`), since “Louis” wouldn’t be discarded when formatting. Instead it would be Jean-Louis (`given`) Trintignant (`surname`)

* Note: If the legal name, stage name, etc. are substantially different, then that information can be logically in a separate PersonName object. That is, it is up to the implementation to maintain any distinctions that are important to it: CLDR PersonName formats is focusing on formatting a PersonName object that is given to it.


* **surname2` would only**: `surname2` would only be asked for in certain locales, and where it is considered a separate, divisible name, such as in Mexico or Spain. For instance, in Mexico, the first and second surname are used for the legal name and in formal settings, and sometimes only the first surname is used in familiar or informal contexts.


* Heathcote-Drummond is a single surname and would not be `{surname}-{surname2}` because we would never discard part of the name when formatting.
* Spanish name: "Jose Luis Garcia Barrientos":   The `given` name is “Jose”, the `given2` name is “Luis”, the `surname` is "Garcia”, and the `surname2` is “Barrientos"

How names get placed into fields to be formatted is beyond the scope of CLDR PersonName formats; this document just lays out the assumptions the formatting code makes when formatting the names.

### <a name="Modifiers" id="Modifiers" href="#Modifiers">Modifiers</a>

* Each field in a pattern can have one or more modifiers. The modifiers can be appended to any field name, such as `{given-initial}` for the first grapheme of the given name. If more than one modifier is applied, they must be structured as in the EBNF.


The modifiers transform the input data as described in the following table:

| Modifier   | Description |
| ---------- | ----------- |
| informal   | Requests an informal version of the name if available. For example, {given} might be “Thomas”, and {given-informal} might be “Tom”. If there is no informal version, then the normal one is returned. An informal version should not be generated, because they vary too much: Beth, Betty, Betsy, Bette, Liz, … |
| prefix     | Return the “prefix” name, or the “tussenvoegsel'' if present. For example, “van der Poel” becomes “van der”, “bint Fadi” becomes “bint”, “di Santis” becomes “di”. Note that what constitutes the prefix is language- and locale-sensitive. It may be passed in as part of the PersonName object, similar to the _“-informal”_ modifier, e.g. as _“surname-prefix”_.<br/><br/>The implementation of this modifier depends on the PersonName object. CLDR does not currently provide support for automatic identification of tussenvoegsels, but may in the future.<br/><br/>If the resulting _“-prefix”_ value is empty, it defaults to an empty string.<br/><br/>An example sorting pattern for “Johannes van den Berg” may be<br/>{surname-core}, {given} {given2} {surname-prefix}<br/><br/>Only the _“-prefix”_ or the _“-core”_ modifier may be used, but not both. They are mutually exclusive. |
| core       | Return the “core” name, removing any tussenvoegsel. For example, “van der Poel” becomes “Poel”, “bint Fadi” becomes “Fadi”, “di Santis” becomes “Santis”. Note that what constitutes the core is language- and locale-sensitive.<br/><br/>The implementation of this modifier depends on the PersonName object. CLDR does not currently provide support for identification of tussenvoegsel, but may in the future.<br/><br/>If the resulting _“-core”_ value is empty, it defaults to the field it modifies. E.g., if _“surname-core”_ is empty in the PersonName object to be formatted, it will default to the _“surname”_ field.<br/><br/>Vice-versa, if the _surname_ field is empty, the formatter will attempt to use _surname-prefix_ and _surname-core_, if present, to format the name.<br/><br/>Only the _“-prefix”_ or the _“-core”_ modifier may be used, but not both. They are mutually exclusive. |
| allCaps    | Requests the element in all caps, which is desired In some contexts. For example, a new guideline in Japan is that for the Latin representation of Japanese names, the family name comes first and is presented in all capitals. This would be represented as<br/>“{surname-allCaps} {given}”<br/><br/>Hayao Miyazaki (宮崎 駿) would be represented in Latin characters in Japan (ja-Latn-JP) as _“MIYAZAKI Hayao”_<br/><br/>_The default implementation uses the default Unicode uppercase algorithm; if the PersonName object being formatted has a locale, and CLDR supports a locale-specific algorithm for that locale, then that algorithm is used. The PersonName object can override this, as detailed below._<br/><br/>Only the _“-allCaps”_ or the _“-initalCap”_ modifier may be used, but not both. They are mutually exclusive. |
| initialCap | Request the element with the first grapheme capitalized, and remaining characters unchanged. This is used in cases where an element is usually in lower case but may need to be modified. For example in Dutch, the name<br/>{ title: “dhr.”, given: ”Johannes”, surname: “van den Berg” },<br/>when addressed formally, would need to be “dhr. Van den Berg”. This would be represented as<br/>“{title} {surname-initialCap}”<br/><br/>Only the _“-allCaps”_ or the _“-initalCap”_ modifier may be used, but not both. They are mutually exclusive. |
| initial    | Requests the initial grapheme cluster of each word in a field. The `initialPattern` patterns for the locale are used to create the format and layout for lists of initials. For example, if the initialPattern types are<br/>``<initialPattern type="initial">`{0}.`</initialPattern>`<br/>`<initialPattern type="initialSequence">`{0} {1}`</initialPattern>``<br/>then a name such as<br/>{ given: “John”, given2: “Ronald Reuel”, surname: “Tolkien” }<br/>could be represented as<br/>“{given-initial-allCaps} {given2-initial-allCaps} {surname}”<br/>and will format to “**J. R. R. Tolkien**”<br/><br/>_The default implementation uses the first grapheme cluster of each word for the value for the field; if the PersonName object has a locale, and CLDR supports a locale-specific grapheme cluster algorithm for that locale, then that algorithm is used. The PersonName object can override this, as detailed below._<br/><br/>Only the _“-initial”_ or the _“-monogram”_ modifier may be used, but not both. They are mutually exclusive. |
| monogram   | Requests initial grapheme. Example: A name such as<br/>{ given: “Landon”, given2: “Bainard Crawford”, surname: “Johnson” }<br/>could be represented as<br/>“{given-monogram-allCaps}{given2-monogram-allCaps}{surname-monogram-allCaps}”<br/>or “**LBJ**”<br/><br/>_The default implementation uses the first grapheme cluster of the value for the field; if the PersonName object has a locale, and CLDR supports a locale-specific grapheme cluster algorithm for that locale, then that algorithm is used. The PersonName object can override this, as detailed below. The difference between monogram an initial is that monogram only returns one element, not one element per word._<br/><br/>Only the _“-initial”_ or the _“-monogram”_ modifier may be used, but not both. They are mutually exclusive. |
| retain | This is needed in languages that preserve punctuation when forming initials. For example, normally the name {given=Anne-Marie} is converted into initials with {given-initialCaps} as “A. M.”. However, where a language preserves the hyphen, the pattern should use {given-initialCaps**-retain**} instead. In that case, the result is “A.-M.”. (The periods are added by the pattern-initialSequence.) |
| genitive, vocative | Patterns can use these modifiers so that better results can be obtained for inflected languages. However, see the details below. |

#### <a name="Grammatical_Modifiers_for_Names" id="Grammatical_Modifiers_for_Names" href="#Grammatical_Modifiers_for_Names">Grammatical Modifiers for Names</a>

The CLDR person name formatting does not itself support grammatical inflection.
However, name sources (NameObject) can support inflections, either by having additional fields or by using an inflection engine that can handle personal name parts.

In the current release, the focus is on supporting `referring` and `addressing` forms.
Typically the `referring` forms will be in the most neutral (*nominative*) case, and the `addressing` forms will be in the *vocative* case.
Some modifiers have been added to facilitate this, so that there can be patterns like: {given-vocative} {surname-vocative}.

Notice that some **parts** of the formatted name may be in different grammatical cases, so the cases may not be consistent across the whole name.
For example:

| English Pattern | Examples | Latvian Pattern | Examples |
| ---- | ---- | ---- | ---- |
| {given} {surname} | John Smith | {given} {surname} | Kārlis Ozoliņš |
| {title} {surname} | Mr Smith | {surname} {title} | Ozoliņa kungs |

Notice that the `surname` in Latvian needs to change to the genitive case with that pattern:

Ozoliņš ➡︎ **Ozoliņa**

That is accomplished by changing the pattern to be {surname<b>-genitive</b>} {title}. In this case the {surname} should only be genitive if followed by the {title}.

#### <a name="Future_Modifiers" id="Future_Modifiers" href="#Future_Modifiers">Future Modifiers</a>

Additional modifiers may be added in future versions of CLDR.

Examples:

1. For the initial of the surname **_“de Souza”_**, in a language that treats the “de” as a tussenvoegsel, the PersonName object can automatically recast `{surname-initial}` to:<br/>`{surname-prefix-initial}{surname-core-initial-allCaps} `to get “dS” instead of “d”.
2. If the locale expects a surname prefix to to be sorted after a surname, then both `{surname-core} `then `{surname-prefix}` would be used as in<br/>`{surname-core}, {given} {given2} {surname-prefix}`
3. Only the grammatical modifiers requested by translators for `referring` or `addressing` have been added as yet, but additional grammatical modifiers may be added in the future.

