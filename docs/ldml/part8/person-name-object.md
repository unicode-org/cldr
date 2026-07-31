## <a name="Person_Name_Object" id="Person_Name_Object" href="#Person_Name_Object">Person Name Object</a>

* The information that is to be formatted logically consists of a data object containing a number of fields. This data object is a construct for the purpose of formatting, and doesn’t represent the source of the name data. That is, the original source may contain more information. The PersonName object is merely a logical ‘transport’ of information to formatting; it may in actuality consist of, for example, an API that fetches fields from a database.


* Note that an application might have more than one set of name data for a given person, such as data for both a legal name and a nickname or preferred name. Or the source data may contain two whole sets of name data for a person from an Eastern Slavic region, one in Cyrillic characters and one in Latin characters. Or it might contain phonetic data for a name (commonly used in Japan). The additional application-specific information in person’s names is out of scope for the CLDR Person Name formatting data. Thus a calling application may produce more than one PersonName object to format depending on the purpose.


For illustration, the following is a sample PersonName object.

| Field            | Value        | Comment                         |
| ---------------- | ------------ | ------------------------------- |
| `title`          | “Dr.”        |                                 |
| `given`          | “William”    |                                 |
| `given-informal` | “Bill”       | example inclusion of "nickname" |
| `given2`         | “Torval”     |                                 |
| `surname`        | “Brown”      |                                 |
| `nameLocale`     | “und-US”     | this is just for illustration   |
| `preferredOrder` | “givenFirst” | values are givenFirst and surnameFirst                        |

* A PersonName object is logically composed of the fields above plus other possible variations. See [[Fields](#fields)]. There must be at least one field present: either a `given` or `surname` field. Other fields are optional, and some of them can be constructed from other fields if necessary.


* A modifier is supplied, _-informal_, which can be used to indicate which data element to choose when formatting informal cases which might include nicknames or preferred names. For more details, see section on [_[Modifiers](#modifiers)_] in [namePattern Syntax](#namepattern-syntax) below.


