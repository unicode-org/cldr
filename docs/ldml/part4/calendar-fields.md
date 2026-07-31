## <a name="Calendar_Fields" id="Calendar_Fields" href="#Calendar_Fields">Calendar Fields</a>

```dtd
<!ELEMENT fields ( alias | (field*, special*)) >
<!ELEMENT field ( alias | (displayName*, relative*, relativeTime*, relativePeriod*, special*)) >
<!ATTLIST field type ( era | era-short | era-narrow | year | year-short | year-narrow | quarter | quarter-short | quarter-narrow | month | month-short | month-narrow | week | week-short | week-narrow | weekOfMonth | weekOfMonth-short | weekOfMonth-narrow | day | day-short | day-narrow | dayOfYear | dayOfYear-short | dayOfYear-narrow | weekday | weekday-short | weekday-narrow | weekdayOfMonth | weekdayOfMonth-short | weekdayOfMonth-narrow | sun | sun-short | sun-narrow | mon | mon-short | mon-narrow | tue | tue-short | tue-narrow | wed | wed-short | wed-narrow | thu | thu-short | thu-narrow | fri | fri-short | fri-narrow | sat | sat-short | sat-narrow | dayperiod | dayperiod-short | dayperiod-narrow | hour | hour-short | hour-narrow | minute | minute-short | minute-narrow | second | second-short | second-narrow | zone | zone-short | zone-narrow ) #IMPLIED >

<!ELEMENT relative (#PCDATA) >
<!ATTLIST relative type NMTOKEN #IMPLIED >

<!ELEMENT relativeTime ( alias | (relativeTimePattern*, special*)) >
<!ATTLIST relativeTime type NMTOKEN #REQUIRED >

<!ELEMENT relativeTimePattern ( #PCDATA ) >
<!ATTLIST relativeTimePattern count ( zero | one | two | few | many | other ) #REQUIRED >

<!ELEMENT relativePeriod (#PCDATA) >
```

* Translations may be supplied for names of calendar fields (elements of a calendar, such as Day, Month, Year, Hour, and so on), and for relative values for those fields (for example, the day with relative value -1 is "Yesterday"). There are four types of translations; some are only relevant or useful for certain types of fields:


* `<displayName>` General display name for the field type. This should be relevant for all elements, including those like era and zone that might not have useful forms for the other name types. These are typically presented in titlecase (eg “Day”) since they are intended as labels in a UI.
* `<relative>` Display names for the current instance of the field, and one or two past and future instances. In English, data is provided for year, quarter, month, week, day, specific days of the week (sun, mon, tue, …), and—with offset 0 only—for hour, minute, and second.
* `<relativeTime>` Display names for an instance of the field that is a counted number of units in the past or the future relative to the current instance; this needs plural forms. In English, data is provided for year, quarter, month, week, day, specific days of the week, hour, minute, and second.
* `<relativePeriod>` Pattern for designating an instance of the specified field in relation to some other date reference. This is currently only used for weeks, and provides a pattern such as “the week of {0}” which can be used to generate designations such as “the week of April 11, 2016” or “the week of April 11–15”.

Where there is not a convenient, customary word or phrase in a particular language for a particular type of relative value, it should be omitted.

Examples, first for English:

```xml
<fields>
    …
    <field type="day">
        <displayName>Day</displayName>
        <relative type="-1">yesterday</relative>
        <relative type="0">today</relative>
        <relative type="1">tomorrow</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">in {0} day</relativeTimePattern>
            <relativeTimePattern count="other">in {0} days</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">{0} day ago</relativeTimePattern>
            <relativeTimePattern count="other">{0} days ago</relativeTimePattern>
        </relativeTime>
    </field>
    <field type="weekday">
        <displayName>Day of the Week</displayName>
    </field>
    <field type="sun">
        <relative type="-1">last Sunday</relative>
        <relative type="0">this Sunday</relative>
        <relative type="1">next Sunday</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">in {0} Sunday</relativeTimePattern>
            <relativeTimePattern count="other">in {0} Sundays</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">{0} Sunday ago</relativeTimePattern>
            <relativeTimePattern count="other">{0} Sundays ago</relativeTimePattern>
        </relativeTime>
    </field>
    …
    <field type="hour">
        <displayName>Hour</displayName>
        <relative type="0">now</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">in {0} hour</relativeTimePattern>
            <relativeTimePattern count="other">in {0} hours</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">{0} hour ago</relativeTimePattern>
            <relativeTimePattern count="other">{0} hours ago</relativeTimePattern>
        </relativeTime>
    </field>
    …
</fields>

```

Second, for German; includes relative type="-2"/"2", present in the English example:

```xml
<fields>
    …
    <field type="day">
        <displayName>Tag</displayName>
        <relative type="-2">Vorgestern</relative>
        <relative type="-1">Gestern</relative>
        <relative type="0">Heute</relative>
        <relative type="1">Morgen</relative>
        <relative type="2">Übermorgen</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">In {0} Tag</relativeTimePattern>
            <relativeTimePattern count="other">In {0} Tagen</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">Vor {0} Tag</relativeTimePattern>
            <relativeTimePattern count="other">Vor {0} Tagen</relativeTimePattern>
        </relativeTime>
    </field>
    …
</fields>
```

A special name for “now” is indicated using `<relative type="0">` for the "second" field. For example, in English:

```xml
<field type="second">
    <displayName>Second</displayName>
    <relative type="0">now</relative>
    …
</field>
```

Different widths can be supplied for certain fields, such as:

```xml
<field type="year-short">
    <displayName>yr.</displayName>
    <relative type="-1">last yr.</relative>
    <relative type="0">this yr.</relative>
    <relative type="1">next yr.</relative>
    <relativeTime type="future">
        <relativeTimePattern count="one">in {0} yr.</relativeTimePattern>
        <relativeTimePattern count="other">in {0} yr.</relativeTimePattern>
    </relativeTime>
    <relativeTime type="past">
        <relativeTimePattern count="one">{0} yr. ago</relativeTimePattern>
        <relativeTimePattern count="other">{0} yr. ago</relativeTimePattern>
    </relativeTime>
</field>
```

As in other cases, **narrow** may be ambiguous out of context.

