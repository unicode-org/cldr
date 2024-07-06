---
title: Proposed Collation Additions
---

# Proposed Collation Additions

|   |   |
|---|---|
| Author | Mark Davis, Markus Scherer, Michael Fairley |
| Date | 2009-06-23 |
| Status | Proposal |
| Bugs | *insert linked bug numbers here* |

## Script Reordering

We would like to add script reordering as a new collation setting. This will allow, for example, sorting Greek before Latin, and digits after all letters, without listing all affected characters in the rules. Since this is a parameter, it can also be changed at runtime without changing any rules.

This will be implemented via a permutation table for primary collation weights. See the original (somewhat outdated) ICU collation design doc for reference:

http://source.icu-project.org/repos/icu/icuhtml/trunk/design/collation/ICU\_collation\_design.htm#Script\_Order

### Proposed LDML syntax:

Add the '**kr**' key, with an ordered list of script names as its types, in the order they should be sorted. For example, to specify an ordering of Greek, followed by Latin, followed by everything else (Zzzz = unknown), with digits (Zyyy = Common) last, the following would be used: **el-u-kr-grek-latn-zzzz-zyyy**. That would modify the ordering found on [http://unicode.org/charts/collation/](http://unicode.org/charts/collation/) in the following way:

- OLD
    - [Null](http://unicode.org/charts/collation/chart_Null.html) [Ignorable](http://unicode.org/charts/collation/chart_Ignorable.html) [Variable](http://unicode.org/charts/collation/chart_Variable.html) [Common](http://unicode.org/charts/collation/chart_Common.html) [Latin](http://unicode.org/charts/collation/chart_Latin.html) [Greek](http://unicode.org/charts/collation/chart_Greek.html) [Coptic](http://unicode.org/charts/collation/chart_Coptic.html) ... [CJK](http://unicode.org/charts/collation/chart_CJK.html) [CJK-Extensions](http://unicode.org/charts/collation/chart_CJK-Extensions.html) [Unsupported](http://unicode.org/charts/collation/chart_Unsupported.html)
- NEW
    - [Null](http://unicode.org/charts/collation/chart_Null.html) [Ignorable](http://unicode.org/charts/collation/chart_Ignorable.html) [Variable](http://unicode.org/charts/collation/chart_Variable.html) [Greek](http://unicode.org/charts/collation/chart_Greek.html) [Latin](http://unicode.org/charts/collation/chart_Latin.html) [Coptic](http://unicode.org/charts/collation/chart_Coptic.html) ... [CJK](http://unicode.org/charts/collation/chart_CJK.html) [CJK-Extensions](http://unicode.org/charts/collation/chart_CJK-Extensions.html) [Unsupported](http://unicode.org/charts/collation/chart_Unsupported.html) [Common](http://unicode.org/charts/collation/chart_Common.html)

***Issue:*** *do we still want Unsupported at the very end??*

The 'digitaft' type for the 'co' key is no longer needed, and can be deprecated (with some minor changes to data).

Add an additional attribute, **scriptReorder**, to **\<settings>**. Its value will be the script names separated by spaces, in the order they should be sorted. The script code **Zzzz** stands for "any other script", and the script code **Zyyy** stands for Common.

Example:

\<settings scriptReorder="grek latn zzzz zyyy">

Note: after looking at the data, I'm thinking that we might want to change the above:

- allow codes that are not just script codes; in particular, Sc and Nd.
- note that implicit is always at the end; thus there would be no code to specify it, so that someone can't try to put something after it.
- Add that if the same script is specified twice in the list, the second wins.
- we also need to warn people that depending on the implementation, specifying a script may drag along others. In particular, historic scripts may be grouped together.

See http://site.icu-project.org/design/collation/script-reordering

### Proposed LDML BCP47 subtag syntax changes:

To allow a key to have multiple types (for listing multiple script codes), change:

extension = key "-" type

to

extension = key ("-" type)+

## Collation Import

We want to add the ability for collation to "import" rules from another collator. This provides two useful features:

- Many European languages can import a common collation for the [European Ordering Rules](http://anubis.dkuug.dk/CEN/TC304/EOR/eorhome.html) and then add language-specific rules on top of that.
- For CJK Unihan variant collation orderings, the large common suffix with the Unihan ordering can be shared.

This should reduce the maintenance burden and make total storage of the collation rule strings significantly smaller.

### Proposed LDML syntax:

Add an **\<import>** tag within collation **\<rules>** with two attributes, **source**, to identify the locale to import from (mirroring \<alias>'s source), and **type**, to identify which collator within the locale to include.

Examples:

\<import source="und\_hani">

\<import source="de" type="phonebk">

Add **private** as an additional attribute for \<settings>:

\<settings private="true"> // mirroring \<transform>'s private attribute

This attribute indicates to clients that the collation is intended only for \<import>, and should not be available as a stand-alone collator or listed in available collator APIs.

**Update CLDR 26 (2014)**: A collation type is marked "private" via a type naming convention, rather than an attribute, so that it is easy for an implementation to omit such a type from a list of available types without reading its data. See [CLDR ticket #3949 comment:18](http://unicode.org/cldr/trac/ticket/3949#comment:18).


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)