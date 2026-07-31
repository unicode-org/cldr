## <a name="Overview_Dates_Element_Supplemental" id="Overview_Dates_Element_Supplemental" href="#Overview_Dates_Element_Supplemental">Overview: Dates Element, Supplemental Date and Calendar Information</a>

```dtd
<!ELEMENT dates (alias | (calendars?, fields?, timeZoneNames?, special*)) >
```

The LDML top-level `<dates>` element contains information regarding the format and parsing of dates and times, the formatting of date/time intervals, and the naming of various calendar elements.

*   The `<calendars>` element is described in [Calendar Elements](#Calendar_Elements).
*   The `<fields>` element is described in [Calendar Fields](#Calendar_Fields).
*   The `<timeZoneNames>` element is described in [Time Zone Names](#Time_Zone_Names).
*   The formats use pattern characters described in [Date Format Patterns](#Date_Format_Patterns).

```dtd
<!ELEMENT supplementalData ( …, calendarData?, calendarPreferenceData?, weekData?, timeData?, …, timezoneData?, …, metazoneInfo?, …, dayPeriodRuleSet*, metaZones?, primaryZones?, windowsZones?, …) >
```

The relevant top-level supplemental elements are listed above.

*   The `<calendarData>`, `<calendarPreferenceData>`, `<weekData>`, `<timeData>`, and `<dayPeriodRuleSet>` elements are described in [Supplemental Calendar Data](#Supplemental_Calendar_Data).
*   The `<timezoneData>` element is deprecated and no longer used; the `<metazoneInfo>` element is deprecated at this level, and is now only used as a sub-element of `<metaZones>`.
*   The `<metaZones>`, `<primaryZones>`, and `<windowsZones>` elements are described in [Supplemental Time Zone Data](#Supplemental_Time_Zone_Data).

