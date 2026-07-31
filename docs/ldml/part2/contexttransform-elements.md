## <a name="Context_Transform_Elements" id="Context_Transform_Elements" href="#Context_Transform_Elements">ContextTransform Elements</a>

```dtd
<!ELEMENT contextTransforms ( alias | (contextTransformUsage*, special*)) >
<!ELEMENT contextTransformUsage ( alias | (contextTransform*, special*)) >
<!ATTLIST contextTransformUsage type CDATA #REQUIRED >
<!ELEMENT contextTransform ( #PCDATA ) >
<!ATTLIST contextTransform type ( uiListOrMenu | stand-alone ) #REQUIRED >
```

* CLDR locale elements provide data for display names or symbols in many categories. The default capitalization for these elements is intended to be the form used in the middle of running text. In many languages, other capitalization may be required in other contexts, depending on the type of name or symbol.


* **Each `` element’s**: Each `<contextTransformUsage>` element’s `type` attribute specifies a category of data from the table below; the element includes one or more `<contextTransform>` elements that specify how to perform capitalization of this category of data in different contexts. The `<contextTransform>` elements are needed primarily for cases in which the capitalization is other than the default form used in the middle of running text. However, it is also useful to mark cases in which it is _known_ that no transformation from this default form is needed; this may be necessary, for example, to override the transformation specified by a parent locale. The following values are currently defined for the `<contextTransform>` element:


* "titlecase-firstword" designates the case in which raw CLDR text that is in middle-of-sentence form, typically lowercase, needs to have its first word titlecased.
* "no-change" designates the case in which it is known that no change from the raw CLDR text (middle-of-sentence form) is needed.

Four contexts for capitalization behavior are currently identified. Two need no data, and hence have no corresponding `<contextTransform>` elements:

* In the middle of running text: This is the default form, so no additional data is required.
* At the beginning of a complete sentence: The initial word is titlecased, no additional data is required to indicate this.

Two other contexts require `<contextTransform>` elements if their capitalization behavior is other than the default for running text. The context is identified by the `type` attribute, as follows:

* uiListOrMenu: Capitalization appropriate to a user-interface list or menu.
* stand-alone: Capitalization appropriate to an isolated user-interface element (e.g. an isolated name on a calendar page)

Example:

```xml
<contextTransforms>
    <contextTransformUsage type="languages">
        <contextTransform type="uiListOrMenu">titlecase-firstword</contextTransform>
        <contextTransform type="stand-alone">titlecase-firstword</contextTransform>
    </contextTransformUsage>
    <contextTransformUsage type="month-format-except-narrow">
        <contextTransform type="uiListOrMenu">titlecase-firstword</contextTransform>
    </contextTransformUsage>
    <contextTransformUsage type="month-standalone-except-narrow">
        <contextTransform type="uiListOrMenu">titlecase-firstword</contextTransform>
    </contextTransformUsage>
</contextTransforms>
```

###### <a name="contextTransformUsage_type_attribute_values" id="contextTransformUsage_type_attribute_values" href="#contextTransformUsage_type_attribute_values">Table: Element contextTransformUsage type attribute values</a>

| type attribute value             | Description |
| -------------------------------- | ----------- |
| `all`                            | Special value, indicates that the specified transformation applies to all of the categories below |
| `language`                       | `localeDisplayNames` language names |
| `script`                         | `localeDisplayNames` script names |
| `territory`                      | `localeDisplayNames` territory names |
| `variant`                        | `localeDisplayNames` variant names |
| `key`                            | `localeDisplayNames` key names |
| `keyValue`                       | `localeDisplayNames` key value type names |
| `month-format-except-narrow`     | `dates/calendars/calendar[type=*]/months` format wide and abbreviated month names |
| `month-standalone-except-narrow` | `dates/calendars/calendar[type=*]/months` stand-alone wide and abbreviated month names |
| `month-narrow`                   | `dates/calendars/calendar[type=*]/months` format and stand-alone narrow month names |
| `day-format-except-narrow`       | `dates/calendars/calendar[type=*]/days` format wide and abbreviated day names |
| `day-standalone-except-narrow`   | `dates/calendars/calendar[type=*]/days` stand-alone wide and abbreviated day names |
| `day-narrow`                     | `dates/calendars/calendar[type=*]/days` format and stand-alone narrow day names |
| `era-name`                       | `dates/calendars/calendar[type=*]/eras` (wide) era names |
| `era-abbr`                       | `dates/calendars/calendar[type=*]/eras` abbreviated era names |
| `era-narrow`                     | `dates/calendars/calendar[type=*]/eras` narrow era names |
| `quarter-format-wide`            | `dates/calendars/calendar[type=*]/quarters` format wide quarter names |
| `quarter-standalone-wide`        | `dates/calendars/calendar[type=*]/quarters` stand-alone wide quarter names |
| `quarter-abbreviated`            | `dates/calendars/calendar[type=*]/quarters` format and stand-alone abbreviated quarter names |
| `quarter-narrow`                 | `dates/calendars/calendar[type=*]/quarters` format and stand-alone narrow quarter names |
| `calendar-field`                 | `dates/fields/field[type=*]/displayName` field names<br/>(for relative forms see type "tense" below) |
| `zone-exemplarCity`              | `dates/timeZoneNames/zone[type=*]/exemplarCity` city names |
| `zone-long`                      | `dates/timeZoneNames/zone[type=*]/long` zone names |
| `zone-short`                     | `dates/timeZoneNames/zone[type=*]/short` zone names |
| `metazone-long`                  | `dates/timeZoneNames/metazone[type=*]/long` metazone names |
| `metazone-short`                 | `dates/timeZoneNames/metazone[type=*]/short` metazone names |
| `symbol`                         | `numbers/currencies/currency[type=*]/symbol` symbol names |
| `currencyName`                   | `numbers/currencies/currency[type=*]/displayName` currency names |
| `currencyName-count`             | `numbers/currencies/currency[type=*]/displayName[count=*]` currency names for use with count |
| `relative`                       | `dates/fields/field[type=*]/relative and dates/fields/field[type=*]/relativeTime` relative field names |
| `unit-pattern`                   | `units/unitLength[type=*]/unit[type=*]/unitPattern[count=*]` unit names |
| `number-spellout`                | `rbnf/rulesetGrouping[type=*]/ruleset[type=*]/rbnfrule` number spellout rules |

