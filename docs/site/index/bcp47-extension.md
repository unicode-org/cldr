---
title: Unicode Extensions for BCP 47
---

# Unicode Extensions for BCP 47

[IETF BCP 47 *Tags for Identifying Languages*](https://www.rfc-editor.org/info/bcp47) defines the language identifiers (tags) used on the Internet and in many standards. It has an extension mechanism that allows additional information to be included. The Unicode Consortium is the maintainer of the extension ‘u’ for Locale Extensions, as described in [rfc6067](https://datatracker.ietf.org/doc/html/rfc6067), and the extension 't' for Transformed Content, as described in [rfc6497](https://datatracker.ietf.org/doc/html/rfc6497).

- The subtags available for use in the 'u' extension provide language tag extensions that provide for additional information needed for identifying locales. The 'u' subtags consist of a set of keys and associated values (types). For example, a locale identifier for British English with numeric collation has the following form: en-GB-**u-kn-true**    
- The subtags available for use in the 't' extension provide language tag extensions that provide for additional information needed for identifying transformed content, or a request to transform content in a certain way. For example, the language tag "ja-Kana-t-it" can be used as a content tag indicates Japanese Katakana transformed from Italian. It can also be used as a request for a given transformation.
    

For more details on the valid subtags for these extensions, their syntax, and their meanings, see LDML Section 3.7 [*Unicode BCP 47 Extension Data*](https://www.unicode.org/reports/tr35/#Locale_Extension_Key_and_Type_Data).

## Machine-Readable Files for Validity Testing

Beginning with CLDR version 1.7.2, machine-readable files are available listing the valid attributes, keys, and types for each successive version of [LDML](https://unicode.org/reports/tr35/). The most recently released version is always available at http://unicode.org/Public/cldr/latest/ in a file of the form cldr-common\*.zip (in older versions the file was of the form cldr-core\*.zip). Inside that file, the directory "common/bcp47/" contains the data files defining the valid attributes, keys, and types.

The BCP47 data is also currently maintained in a source code repository, with each release tagged, for viewing directly without unzipping. For example, see https://github.com/unicode-org/cldr/tree/release-38/common/bcp47. The current development snapshot is found at https://github.com/unicode-org/cldr/tree/master/common/bcp47.

All releases including the latest are listed on http://cldr.unicode.org/index/downloads, with a link to each respective data directory under the column heading **Data**, and direct access to the repository under the **GitHub Tag.**

For example, the timezone.xml file looks like the following:

\<keyword\>

\<key name="tz" alias="timezone"\>

\<type name="adalv" alias="Europe/Andorra"\/>

\<type name="aedxb" alias="Asia/Dubai"\/>

Using this data, an implementation would determine that "fr-u-tz-adalv" and fr-u-tz-aedxb" are both valid. Some data in the CLDR data files also requires reference to [LDML](https://unicode.org/reports/tr35/) for validation according to [Appendix Q](https://unicode.org/reports/tr35/#Locale_Extension_Key_and_Type_Data) of [LDML](https://unicode.org/reports/tr35/). For example, LDML defines the type 'codepoints' to define specific code point ranges in Unicode for specific purposes.

## Version Information

The following is not necessary for correct validation of the -u- extension, but may be useful for some readers.

Each release has an associated data directory of the form "http://unicode.org/Public/cldr/\<version\>", where "\<version\>" is replaced by the release number. The version number for any file is given by the directory where it was downloaded from. If that information is no longer available, the version can still be accessed by looking at the common/dtd/ldml.dtd file in the cldr-common\*.zip file (for older versions, the core.zip file), at the element cldrVersion, such as the following. This information is also accessible with a validating XML parser.

\<!ATTLIST version cldrVersion CDATA #FIXED "1.8" \>

For each release after CLDR 1.8, types introduced in that release are also marked in the data files by the XML attribute "since", such as in the following example: \<type name="adp" since="1.9"/\>

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)