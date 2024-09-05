---
title: Direct Modifications to CLDR Data
---

# Direct Modifications to CLDR Data

*See also: Bulk Import of XML Data.*

### 1\. Verifying changes

Please check that your changes don't cause problems. A minimal test is to run ConsoleCheckCLDR with the following parameter:

\-f(en)

This will run the checks on en: you can substitute other locales to check them also (It is a regular expression, so -f(en.\*|fr.\*) will do all English and French locales).

I recommend also using the following options, to show opened files, and increase memory (some tests require that).

\-Dfile.encoding=UTF-8 -DSHOW\_FILES -Xmx512M

An example of where a DTD broke, the invalid XML:

\<!ELEMENT commonlyUsed (true | false ) #IMPLIED \>.

I changed to \<!ELEMENT commonlyUsed EMPTY \> to get it to function; other changes might be necessary.

### 2\. Explicit defaults

Don't use them, since they cause the XML to be fluffed up, and may interfere with the inheritance unless you make other modifications.

\<!ATTLIST fields casing ( titlecase-words | titlecase-firstword |

lowercase-words | mixed ) "mixed" \>

\=>

\<!ATTLIST fields casing ( titlecase-words | titlecase-firstword |

lowercase-words | mixed ) #IMPLIED \>

Instead, the default should be documented in the spec

### 3\. Mixing meanings.

Attribute and element names should be unique, unless they have the same meaning across containing elements, and same substructure. This is a hard-and-fast rule for elements. For attributes, it is better to have unique names (as we've found by bitter experience) where possible. It is \*required\* when the attribute is distinguishing for one element and not for another.

So the following is ok, but would be better if one of the attribute values were changed.

\<!ATTLIST standard casing (verbatim) #IMPLIED \>

\<!ATTLIST fields casing ( titlecase-words | titlecase-firstword | lowercase-words | mixed ) #IMPLIED \>

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)