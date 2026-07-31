## <a name="Person_Name_Attributes" id="Person_Name_Attributes" href="#Person_Name_Attributes">Person Name Attributes</a>

* A person name pattern may have any of four attributes: order, length, usage, and formality. LDML specifies that all the values for these attributes are unique. For example, because length=long is valid, usage=long cannot also be valid. That allows the pattern labels to be simple, because the attribute names can be skipped. That is,


> `{order=givenFirst, length=long, usage=referring, formality=formal}`

can be abbreviated without loss of information as:

> _givenFirst-long-referring-formal._

Each of these attributes are described below using sample PersonName objects as examples.

### <a name="order" id="order" href="#order">order</a>

* The order attribute is used for patterns with different orders of fields. The order=sorting patterns are chosen based on input parameters, while the choice between givenFirst and surnameFirst is based on features of the PersonName object to be formatted and the nameOrder element values.


| Parameter      | Description                                  |
| -------------- | -------------------------------------------- |
| `givenFirst`   | The given name precedes the surname.         |
| `surnameFirst` | The surname precedes the given name.         |
| `sorting`      | Used to format names for a sorted list.<br/>example: “Brown, William”  [medium, informal] |

* For example, when the display language is Japanese, it is customary to use _surnameFirst_ for names of people from Japan and Hungary, but use _givenFirst_ for names of people from the United States and France. Although the English pattern for sorting is distinct from the other patterns (except for unusual names), that is not necessarily the case in other languages.


### <a name="length" id="length" href="#length">length</a>

* The `length` attribute specifies the relative length of a formatted name depending on context. For example, a `long` formal name in English might include title, given, given2, surname plus generation and credentials; whereas a `short` informal name may only be the given name.


* Note that the formats may be the same for different lengths depending on the formality, usage, and cultural conventions for the locale. For example, medium and short may be the same for a particular context.


| Parameter | Description |
| --------- | ----------- |
| `long`    | A `long` length would usually include all parts needed for a legal name or identification.<br/>Example: `usage="referring", formality="formal"`<br/>_“Mr. Robert John Smith, PhD”_ |
| `medium`  | A `medium` length is between long and short.<br/>Example: `usage="referring", formality="formal"`<br/>_“Robert Smith”_ |
| `short`   | A `short` length uses a minimum set of names.<br/>Example: `usage="referring", formality="formal"`<br/>_“Mr. Smith”_ |

### <a name="usage" id="usage" href="#usage">usage</a>

The usage indicates if the formatted name is being used to address someone, refer to someone, or present their name in an abbreviated form.

The pattern for `usage="referring"` may be the same as the pattern for `usage="addressing"`.

| Parameter    | Description |
| ------------ | ----------- |
| `addressing` | Used when speaking “to” a person, or “vocative” case. This may also have an effect on the formality.<br/>example: “Welcome, **Robert**” |
| `referring`  | Used when speaking “about” a person, or “nominative” case.<br/>example: “**Robert Smith** joined your group” |
| `monogram`   | The `monogram` usage is for a specific abbreviated form for computer UI.<br/>Example: a monogram for Robert James Smith may be **RS** or **RJS**.|

Slavic languages provide a good  example of `addressing` vs `referring`. An example _uk-Cyrl_ PersonName object:

| Field            | Value        | Comment                         |
| ---------------- | ------------ | ------------------------------- |
| `title`          | “г-н”        | “Mr.”                           |
| `given`          | “Иван”       | “Ivan”                          |
| `given2`         | “Петрович”   | “Petrovich”                     |
| `surname`        | “Васильев”   | “Vasiliev”                      |

In Slavic languages, when _`addressing`_ a person (with `length="long"`), it might be

* г-н Иван Петрович Васильев `// "Mr Ivan Petrovich Vasiliev"`

And when _`referring`_ to a person, it might place the surname first.:

* Васильев Иван Петрович `// "Vasiliev Ivan Petrovich"`

* The `monogram` usage is for very short abbreviated names, such as might be found in online messaging text avatars or other annotations. Ideally, a `monogram` format should result in something that could fit in an em square. Some emoji provide examples of this: 🅰️ 🆎 🆘


* When used with `length`, for many alphabetic locales a `monogram` would resolve to one, two, or three characters for short, medium, and long respectively. But that may vary depending on the usage in a locale.


### <a name="formality" id="formality" href="#formality">formality</a>

The `formality` indicates the formality of usage. A name on a badge for an informal gathering may be much different from an award announcement at the Nobel Prize Ceremonies.

* Note that the formats may be the same for different formality scenarios depending on the length, usage, and cultural conventions for the locale. For example short formal and short informal may both be just the given name.


| Parameter  | Description |
| ---------- | ----------- |
| `formal`   | A more formal name for the individual. The composition depends upon the language. For example, a particular locale might include the title, generation, credentials and a full middle name (given2) in the long form.<br/><br/>`length="medium", formality="formal"`<br/>“Robert J. Smith” |
| `informal` | A less formal name for the individual. The composition depends upon the language. For example, a language might exclude the title, credentials and given2 (middle) name. Depending on the length, it may also exclude the surname. The formatting algorithm should choose any passed in name data that has an _informal_ attribute, if available.<br/><br/>`length="medium", formality="informal"`<br/>“Bob Smith” |

