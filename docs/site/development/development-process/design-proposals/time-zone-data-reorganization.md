---
title: Time Zone Data Reorganization
---

# Time Zone Data Reorganization

### Existing Data by Type

**a) alias mapping**

supplementalData.xml

\<zoneFormatting>

...

\<zoneItem type="Africa/Asmera" territory="ER" aliases="Africa/Asmara"/>

...

\</zoneFormatting>

**b) metazone -> golden zone**

supplementalData.xml

\<mapTimezones type="metazone">

...

\<mapZone other="Africa\_Central" territory="001" type="Africa/Maputo"/>

\<mapZone other="Africa\_Central" territory="BI" type="Africa/Bujumbura"/>

\<mapZone other="Africa\_Central" territory="BW" type="Africa/Gaborone"/>

\<mapZone other="Africa\_Central" territory="CD" type="Africa/Lubumbashi"/>

\<mapZone other="Africa\_Central" territory="MW" type="Africa/Blantyre"/>

\<mapZone other="Africa\_Central" territory="RW" type="Africa/Kigali"/>

\<mapZone other="Africa\_Central" territory="ZM" type="Africa/Lusaka"/>

\<mapZone other="Africa\_Central" territory="ZW" type="Africa/Harare"/>

...

\</mapTimezones>

**c) historic metazone mapping**

metazoneInfo.xml

\<metazoneInfo>

...

\<timezone type="Asia/Yerevan">

\<usesMetazone to="1991-09-22 20:00" mzone="Yerevan"/>

\<usesMetazone from="1991-09-22 20:00" mzone="Armenia"/>

\</timezone>

...

\</metazoneInfo>

**d) zone -> territory**

supplementalData.xml

\<zoneFormatting>

...

\<zoneItem type="Africa/Asmera" territory="ER" aliases="Africa/Asmara"/>

...

\</zoneFormatting>

**e) territories where multiple time zones are available**

supplementalData.xml

\<zoneFormatting multizone="001 AQ AR AU BR CA CD CL CN EC ES FM GL ID KI KZ MH MN MX MY NZ PF PT RU UA UM US UZ" tzidVersion="2009f">

**f) Mapping between Olson ID and Unicode time zone short ID (bcp47 ids)**

(1) bcp47/timezone.xml

Short ID -> Olson ID

\<type name="adalv" alias="Europe/Andorra"/>

(2) supplementalData.xml

Olson ID -> Short ID

\<bcp47KeywordMappings>

...

\<mapTypes type="timezone">

...

\<typeMap type="Europe/Andorra" bcp47="adalv"/>

...

\</mapTypes>

...

\</bcp47KeywordMappings>

**g) Windows tzid mapping**

supplementalData.xml

\<mapTimezones type="windows">

\<mapZone other="AUS Central Standard Time" type="Australia/Darwin"/> \<!-- S (GMT+09:30) Darwin -->

...

\</mapTimezones>

### Hige Level Proposal

- bcp47/timezone.xml must be there, because all keyword keys/types must reside in bcp47/\*.xml. Therefore, f) (2) \<bcpKeywordMappings> should be deprecated (this is an invert mapping table)
- The set of canonical Unicode time zone IDs is defined by bcp47/timezone.xml. Because we do not want to maintain the set in multiple places, long ID aliases could be embedded in bcp47/timezone.xml.
- Metazone tables (b and c) should be in a single file
- Territory mapping is almost equivalent to zone.tab in tzdata (minor exception - zone.tab does not include deprecated zones). I think it is not worth maintaining the data in CLDR. Therefore, d) and e) should be deprecated / no corresponding data in 1.8
- Windows tz mapping is independently maintained - it should be moved into a new file. Side note: A single Windows tz could be mapped to multiple zones in future.

### New Data Organization

1. bcp47/timezone.xml

Add long aliases - for example

\<type name="erasm" alias="Africa/Asmera Africa/Asmara"/>

2. metazoneInfo.xml

Add b) into this file

3. wintz.xml (new)

Store only g).

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)