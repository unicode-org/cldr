---
title: CLDR JSON Bindings
---


> **Note**: This page is out of date. See <https://github.com/unicode-org/cldr-json> for current CLDR JSON data.

<!-- TODO: Update this page from https://sites.google.com/unicode.org/cldr/index/cldr-spec/cldr-json-bindings -->

-----

## History
- from: Shanjian Li (Google)
- original draft approved by CLDR Committee: May 9, 2011
- original date: March 29, 2011
- last update: S eptember 9, 2013 by John C. Emmons (IBM)
- latest draft approved by CLDR Committee: July 31, 2013**

## Summary

This document describes transformation between the XML format of structured locale data and JavaScript Object Notation (JSON) for the purpose of making CLDR data available in a lightweight form to interested clients.

## Sample

The sample is based on the preliminary specification and CLDR version 22.1.

## Introduction

Unicode Technical Standard \#35 describes an XML format for the exchange of structured locale data named Unicode Locale Data Markup Language (LDML). Data gathered and vetted through the Common Locale Data Repository (CLDR) project is stored in LDML format.

This data is used for many purposes. However, distribution of it tends to be unwieldy for various reasons. For many potential users, the only alternative is to use an internationalization library, such as ICU. Such library might not exist in users’ platform, or too much a burden due to its size and performance requirement, or just too much overhead for a seemingly simple task. The rapidly growing area of web applications needs a way to access i18n data in a simple way.

The goal is for a new CLDR representation format is to make the data directly usable by applications across network.

## JSON Format

JSON has become the default format choice in web application world. It is compact, human readable, and supported by almost any scripts/languages with very little or no overhead. Especially, it is supported by Javascript (and Python) natively.

This doc proposes a JSON binding for CLDR data. It does **not** replace XML as the default and official format for maintaining the data. It was designed to improve usability for clients. Two major decisions are:

1. The JSON representation is based on the fully resolved form. For each locale, all the information should have been fully resolved and can be accessed right on that spot. There is no need to resort back to root and other complicated fallback mechanism as used in official CLDR XML specification.
2. The JSON binding will contains all the information found in original XML format. But there is no goal to convert JSON data back to XML format. All the changes should still happen in LDML.

Compared to LDML, information is organized cleanly in a  tree structure. Such a structure allows a client application to have an easy way to locate the information it needs, and receive the data in the desired granularity.

### Data Access

Three data types are used in JSON binding, “object”, “string” and “array”. Array can be treated as a special object that has two kinds of properties, “length” being the number of the elements in this array, and a numerical index map to the corresponding element. That leads to a simple tree structure, with all leaves being string, and all branches are “objects”/”array”.  CLDR files are organized in tree structure, thus all CLDR data can be treated as a single tree. The path of the parent elements is sufficient to locate a subtree or a leaf.

With a clean tree structure, to access data in CLDR JSON form is very simple, the elements path from the root to the leaf to subtree is enough. We name this path as cldr\_path, and it always start from root element “cldr”.

\<cldr\_path\> ::= cldr(/\<element\>)\*

\<element\> ::= (letter|digit|-|\_)(letter|digit|-|\_)\*

The directory name (like “main”, “supplemental”) will be the 2nd level element, and each file understand those subdirectories is also corresponding to a level of element.

In actual use, some additional information might be necessary to pin down the version of data etc. That’s up to the actual deployment. In our implementation, we added 3 name/value pairs and pass them as URL’s query parameters.

* tag=\<tag\_name\>
* Specify the name of an instance of the tree structure. Tag is usually used to incorporate CLDR version and approval status.
* depth=\<number\>
* Specify the depth of subtree that should be returned. If not specified, maximum depth will be used, which mean the complete subtree without truncation.
* fallback=\<locale name\>\[,\<locale name\>\]\*
* If certain value is not explicitly specified in certain locale, before fallback to “root” locale, try the locale in fallback first.
* (Clean tree structure could not support this feature. We will have to have a way to mark if certain value is resolved from root or not. We might need a separate “no root” tree to support this feature.)

##  Conversion

This section describes how the conversion should happen.

### Rough guidelines

* Hierarchy is retained as much as it makes sense
* Key names in JSON representation must be unique and URL safe
* All the aliases should be resolved
* Better readability in the transformed JSON representation is always a priority, even if it add complexity to the transformation process.

### Element Handling

The mapping from an XML element to JSON element is mostly direct one-to-one mapping.

The root element (ldml) in CLDR XML was omitted. The cldr\_path will be like `cldr/main/en/localeDisplaynames`  instead of `cldr/main/en/ldml/localeDisplayNames`.
Time zone ids have the form of `America/Los_angeles`. In order to make the name safe for URL, time zone elements are split into multiple levels and grouped in each level.
Instead of transforming them directly like,

```json
{
 “Europe/London”: {...},
 “Europe/Dublin”: {...},
 …
}
```

They will be transformed as,

```json
{
 “Europe”: {
       London”: {...},
       Dublin”: {...},
        …
  },
 …
}
```

This set of elements include: “zone”, “timezone”, “zoneItem”, “typeMap”
Element "alias" is resolved.

"usesMetazone" is used as a bridge to connect time zone and their content. The content of metazone is assigned to those time zone that uses the metazone. That improves the usability of JSON representation.

For elements that have many child elements with the same name, their child elements will be translated as an array. Order of its children is significant for some elements. In such case,  array is also used.

The processing of attribute of the element might change the name of elements, creating additional JSON objects. Detail will be covered in next section.

There are certain groups of elements that can be combined into a single JSON element in order to make the resulting JSON format more compact. These currently include the following:

`(dateFormat|timeFormat|dateTimeFormat)\[@type="(full|long|medium|short)"\]/(dateFormat|timeFormat|dateTimeFormat)/pattern`

In this case the "dateFormat" and "pattern" elements are superfluous and should be omitted.
Example: The following fragment of LDML:

```xml
<dateFormats>
    <dateFormatLength type="full">
        <dateFormat>
            <pattern>EEEE, MMMM d, y</pattern>
        </dateFormat>
    </dateFormatLength>
    <dateFormatLength type="long">
        <dateFormat>
            <pattern>MMMM d, y</pattern>
        </dateFormat>
    </dateFormatLength>
    <dateFormatLength type="medium">
        <dateFormat>
            <pattern>MMM d, y</pattern>
        </dateFormat>
    </dateFormatLength>
    <dateFormatLength type="short">
        <dateFormat>
            <pattern>M/d/yy</pattern>
        </dateFormat>
    </dateFormatLength>
</dateFormats>
```

will be converted as

```json
"dateFormats": {
   "full": "EEEE, MMMM d, y",
   "long": "MMMM d, y",
   "medium": "MMM d, y",
   "short": "M/d/yy"
}
```

instead of

```json
"dateFormats": {
   "full": {
       "dateFormat": {
           "pattern": "EEEE, MMMM d, y"
       }
   },
   "long": {
       "dateFormat": {
           "pattern": "MMMM d, y"
       }
   },
   "medium": {
       "dateFormat": {
           "pattern": "MMM d, y"
       }
   },
   "short": {
       "dateFormat": {
           "pattern": "M/d/yy"
       }
   }
}
```

### Attributes Handling

The attributes of XML elements is handled by a set of rules. They could change the way how element is named and may lead to creation of extra JSON objects. In CLDR, XML attributes are classified as “distinguishing attributes” and “non-distinguishing attributes”. “distinguishing attributes” usually make into JSON key name. “non-distinguishing attributes” usually end up with values.

#### Distinguishing attributes becomes the key name.

The default processing of distinguishing attribute is to make it to be the key name. The original element name is dropped as it can be inferred from its parent.

Example

```xml
<dayPeriod type=”am”>AM</dayPeriod>
<dayPeriod type=”am” alt=”variant”>a.m.</dayPeriod>
```

will be transformed as,

```json
“wide” : {
   “sun”: “Sunday”,
   …
}
```

#### Distinguishing attribute  becomes part of the key name

This set of distinguishing attributes decorate an additional element item, “alt” is the typical one. There is an item without “alt” attribute, and there is one with “alt”. In such case, attribute name and value will be appended to element name to form a new name in form of `<element_name>-<attribute>-<value>`

Example:

```xml
<dayPeriod type=”am”>AM</dayPeriod>
<dayPeriod type=”am” alt=”variant”>a.m.</dayPeriod>
```

This will be translated to following using rule1 \+ rule2.

```json
"am": "AM",
"am-alt-variant": "a.m.",
```

##### Enumeration

In following list, each  item appears in the form of \<parent\>:\<element\>:\<attribute\>, and “\*” is the wildcard that matches everything.

```java
"monthWidth:month:yeartype",
"currencyFormats:unitPattern:count",
"currency:displayName:count",
"numbers:symbols:numberSystem",
"*:*:alt",
```

<!-- srl note: TODO Enumeration seems out of place here. -->

#### Distinguishing attribute create an additional level of object

Sometimes multiple elements of the same name differentiated by distinguishing attribute appear in the same level together with elements of different name(s). Distinguishing attribute can no longer be extracted as key because the existence of other type of elements. In such case, an object keyed by element name is created first to enclose all element of with the same name. Inside this object, a set of objects keyed by distinguishing attribute is created. There could be one element without distinguishing attribute. This is handled by adding this attribute with value of “standard”.

Example

\<exemplarCharacters\> ... \</exemplarCharacter\>

\<exemplarCharacters type=”auxiliary”\> ... \</exemplarCharacter\>

will be mapped to:

```json
“exemplarChacter”: {
   “standard”: ...,
   “auxilliary”: ...
}
```

##### Enumeration

Some element names is ambiguous by itself. Their parent element is added and separated by ‘/’.

```java
“exemplarCharacter”,
“ellipsis”,
“metazone”,
“identity/language”,
“languagePopulation”,
“paperSize”,
“decimalFormats/decimalFormatLength”,
“alias”,
“firstDay”,
“minDays”,
“weekendStart”,
“weekendEnd”
“currencyData/region”
```

#### Distinguishing attribute combinations that create multiple levels.

In this case, the logical grouping is for descriptions by each type of key.  These cannot be handled as in section \#5 below as originally specified, because doing so would create multiple objects with the same key value ( See [http://unicode.org/cldr/trac/ticket/5477](http://unicode.org/cldr/trac/ticket/5477) )

**Example**

\<types\>
\<type type=”arab” key=”numbers”\>Arabic-Indic Digits\</type\>
\<type type="chinese" key="calendar"\>Chinese Calendar\</type\>
\<type type="deva" key="numbers"\>Devanagari Digits\</type\>
\<type type="gregorian" key="calendar"\>Gregorian Calendar\</type\>
\</types\>

would be transformed to the following,

```json
"types": {
  "calendar": {
    "chinese": "Chinese Calendar",
    "gregorian": "Gregorian Calendar"
  },
  "numbers": {
    "arab": "Arabic-Indic Digits",
    "deva": "Devanagari Digits"
  }
}
```

**Distinguishing attributes that should be treated as values**

There is a set of distinguishing attributes that should really be leaf with a string value. The attribute value does not fit into key name.

**Example: ( from windowsZones.xml in supplemental )....**

\<mapZone other="Greenwich Standard Time" territory="CI" type="Africa/Abidjan"/\>

**would convert to:**

```json
"mapZone": {
"@other": "Greenwich Standard Time",
"@territory": "CI",
"@type": "Africa/Abidjan"
}
```

**Enumeration**

In following list, each  item appears in the form of \<parent\>:\<element\>:\<attribute\>.

  // in common/supplemental/dayPeriods.xml
  "dayPeriodRules:dayPeriodRule:from",

  // in common/supplemental/likelySubtags.xml
  "likelySubtags:likelySubtag:to",

  // in common/supplemental/metaZones.xml
  "timezone:usesMetazone:mzone",
  // Only the current usesMetazone will be kept, it is not necessary to keep
  // "to" and "from" attributes to make key unique. This is needed as their
  // value is not good if used as key.
  "timezone:usesMetazone:to",
  "timezone:usesMetazone:from",
  "mapTimezones:mapZone:other",
  "mapTimezones:mapZone:type",
  "mapTimezones:mapZone:territory",
  // in common/supplemental/numberingSystems.xml
  "numberingSystems:numberingSystem:type",
  // in common/supplemental/supplementalData.xml
  "region:currency:from",
  "region:currency:to",
  "calendar:calendarSystem:type",

  // in common/supplemental/windowsZones.xml
  "mapTimezones:mapZone:other",

  // in common/bcp47/\*.xml
  "keyword:key:alias",
  "key:type:alias",
  "key:type:name",

  // common/segments
  "identity:territory:type",
  "identity:variant:type",
  // common/rbnf
  "identity:script:type",

#### Default processing of non-distinguishing attributes.

Non-distinguishing attributes are treated as values unless other rules instruct it differently.  “**\_**” is added to the name to differentiate attributes from child elements. Previous versions of this specification used "@" instead of "\_", but this is problematic because "@" can't be used as the first character in a JavaScript identifier, thus making the resulting JSON almost unusable for parsing in JavaScript.

Example:

\<info iso4217=”ADP” digits=”0” rounding=”0”/\>

will be transformed to:

```json
“ADP”: {
  “rounding”: “0”,
  “digits”: “0”
}
```

“iso4217” is a distinguishing attribute, it is transformed into a key by Rule-1. “digits” and “rounding” are non-distinguishing attributes and transformed to values. Attributes that are treated as value with attribute name suppressed.

Among the attributes that are being treated as values, some of them will be the only item in the converted object, ie.
 key: { attribute: value }

it would be desirable to reduce it to form of**:**
key: value

In fact, many of those item is exactly representing such a mapping. In those cases, the attribute name will be dropped.

Example

 \<likelySubtag from=”zh” to=”zh\_Hans\_CN” /\>

will be mapped to:

```json
  “zh”: “zh_Hans_CN”,
```

**Enumeration:**

// common/main
"calendars:default:choice",
"dateFormats:default:choice",
"months:default:choice",
"monthContext:default:choice",
"days:default:choice",
"dayContext:default:choice",
"timeFormats:default:choice",
"dateTimeFormats:default:choice",

// common/supplemental
"likelySubtags:likelySubtag:to",
"territoryContainment:group:contains",
"calendar:calendarSystem:type",
"calendarPreferenceData:calendarPreference:ordering",
"weekData:firstDay:day",
"weekData:weekendStart:day",
"weekData:weekendEnd:day",
"measurementData:measurementSystem:type",
"codesByterritory:telephoneCountryCode:code",

// common/collation
"collations:default:choice",
// common/segments
"identity:territory:type",
"identity:variant:type",

**“references” are dropped in the transformation.**

This attributes is for documentation in nature.

**“\_q” attribute is dropped, but order is kept.**

“\_q” attribute is used to mark the order and distinguishing child element. If there could be only one child element, “\_q” will be dropped without further processing. Otherwise those child elements will be converted to array and order will be kept there. In either case “\_q” is not needed after the conversion.

**Attributes set that can be suppressed.**

There are a set of (distinguishing) attributes which always have the default value.  Using the element name instead of the value of that distinguishing attribute will make the JSON result more readable. We choose to convert those \<element, attribute, value\> triples using element name. So instead of transforming to key name “element-attribute-value” or “value”, the value can be suppressed and thus transformed to “element”.

**Example:**

\<timeFormat type=”standard”\>...\</timeFormat\>

It will be transformed to:

“timeFormat”: …

**Enumeration**:

This list include both distinguishing and non-distinguishing attributes. It comes from cldr/common/supplemental/supplementalMetadata. Each group is a triple of (element, attribute, value). If the specified attribute appears in specified element with specified value, attribute and value should be suppressed with only element name left.

     "currencyFormat", "type",  "standard",
     "dateFormat",  "type", "standard",
     "dateTimeFormat", "type", "standard",
     "decimalFormat", "type", "standard",
     "pattern", "type",  "standard",
     "percentFormat", "type",  "standard",
     "scientificFormat", "type",  "standard",
     "timeFormat", "type", "standard",

**Attributes with values in form of multiple items will be split into multiple items.**

If multiple items are enumerated as value for an attribute, such element can be split into multiple elements, with each element only contains one item in its value part.

**Example:**

\<weekendStart day="thu"  territories="DZ KW OM SA SD YE AF IR"/\>

will be first split into a group of elements:

 \<weekendStart day="thu"  territories="DZ"/\>
 \<weekendStart day="thu"  territories="KW"/\>
 \<weekendStart day="thu"  territories="OM"/\>
 \<weekendStart day="thu"  territories="SA"/\>
 \<weekendStart day="thu"  territories="SD"/\>
 \<weekendStart day="thu"  territories="YE"/\>
 \<weekendStart day="thu"  territories="AF"/\>
 \<weekendStart day="thu"  territories="IR"/\>

And such items are later translated to a more easily usable form in JSON, like

```json
“weekendStart”: {
 “DZ”: “thu”,
 “KW”: “thu”,
 “OM”: “thu”,
 “SA”: “thu”,
 …
}
```

**Enumeration**:

"/measurementSystem", "territories",
  "/calendarPreference", "territories",
  "/pluralRules", "locales",
  "/weekendStart", "territories",
  "/weekendEnd", "territories",
  "/firstDay", "territories",
  "/dayPeriodRules", "locales",

**Special case: handle “transforms” rules**

Following conversion is a little bit special. None of the above rules can handle it in satisfaction. “transform” element have several attributes that should be converted to value according Rule-5. The child element of “transform” has multiple name but need to be kept in order. It is not desirable to mingle converted attribute with ordered rules. In this case, an additional object “tRules” is added to enclose the rules.

\<transforms\>
 \<transform source=”Simplified” target=”Traditional” direction=”both”\>
   \<comment\>...\</comment\>
   \<tRule\>...\</tRule\>

will be mapped to,

“transforms”: {
 “transform”: {
   “@source”: “Simplified”,
   “@target”: “Traditional”,
   “@direction”: “both”,
   “tRules”: \[
     {“comment”: …},
     {“tRule”: …}
   \]
 }
}

Here “tRules” is brought in so that those attributes of transform can be separated from those rules.
