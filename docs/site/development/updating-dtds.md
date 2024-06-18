---
title: Updating DTDs
---

# Updating DTDs

## Introduction

CLDR makes special use of XML because of the way it is structured. In particular, the XML is designed so that you can read in a CLDR XML file and interpret it as an unordered list of \<path,value> pairs, called a CLDRFile internally. These path/value pairs can be added to or deleted, and then the CLDRFile can be written back out to disk, resulting in a valid XML file. That is a very powerful mechanism, and also allows for the CLDR inheritance model.

Sounds simple, right? But it isn't quite that easy.

## Summary

In summary, when you add an element, attribute, or new kind of attribute value, there are some important steps you must also take. Note that running our unit tests and ConsoleCheck will catch most of these, but you should understand what is going on. Make sure that you don't break any of the invariants below (read through once to make sure you get them)! There is more detailed information further down on the page.

### New Alt Values

If you are only adding new alt values, it is much easier. You still need to change related information, otherwise your strings won't show up properly in the Survey Tool, or the right default values won't be set. So go to [Root Aliases](https://cldr.unicode.org/development/updating-dtds).

## Changing DTDs

We augment the DTD structure in various ways.

1. Annotations, included below the !ELEMENT or !ATTLIST line    
    - \<!--@VALUE--> to indicate that an attribute is not distinguishing, and is treated like an element value.        
    - \<!--@METADATA--> to indicate that an attribute is a "comment" on the data, like the draft status.        
    - \<!--@ORDERED--> to indicate that an element's children are ordered.       
    - \<!--@DEPRECATED--> to indicate that an attribute or element is deprecated.        
    - \<!--@DEPRECATED:attribute-value--> to indicate that an attribute value is deprecated.        
2. attributeValueValidity.xml    
    - For additional validity checks        
3. Check\* tests and unit tests    
    - There are many consistency tests that are performed on the data that can't be expressed with the above.
        
### Removing Structure

1. We never explicitly remove structure except in very unusual cases, so be sure that the committee is in full agreement before doing that.    
2. Normally, we just deprecate it, by adding attributes in the DTD file    
    1. \<!--@DEPRECATED --> below an !ELEMENT or !ATTLIST item        
    2. \<!--@DEPRECATED: comma-separated-attribute-value-list --> for specific attribute values
        

### Adding structure (elements, attributes, attribute-values)

1. For each element
    1. add @ORDERED if it is must be ordered.      
    2. read more details below.
2. For each attribute
    1. add @VALUE or @METADATA to an !ATTLIST if the attribute is non-distinguishing. (See the spec for what this means)  
        1. **@VALUE should never occur except on leaf nodes!** (There are some cases before we realized this was a mistake.)     
    2. If the attribute values are a closed set, you can add them explicitly, like:
        - \<!ATTLIST version draft (approved | contributed | provisional | unconfirmed) #IMPLIED>
    3. Otherwise
        1. Make it NMTOKEN where only single values are allowed, or NMTOKENS otherwise (CDATA in rare cases, but clear with the committee first)
        2. Add validity information to attributeValueValidity.xml    
        3. **Never introduce any default DTD attribute values.** (There are some cases before we realized this was a mistake.)
    4.  For each attribute
        1. add @VALUE or @METADATA to an !ATTLIST if the attribute is non-distinguishing. (See the spec for what this means) 
        2. add @ORDERED to an !ELEMENT.
            
Add the annotations.

### ldml.dtd

1. **Attribute Value.**
    - Certain values have special sorting behavior. These are listed in **CLDRFile.getAttributeValueComparator**. They look like::
        - attribute.equals("day")            
        - || attribute.equals("type") &&            
            - element.endsWith("FormatLength")                
            - || element.endsWith("Width")                
            - ...                
    - Those need to be updated, or an exception will be thrown when the items are processed. *Note that this is different than the sort order used in PathHeader for the survey tool.*
    - To fix them, look at the code and find the right comparator, then modify. Example:       
        - widthOrder = (MapComparator) new MapComparator().add(new String\[\] {"abbreviated", "narrow", "short", "wide"}).freeze();            
2. **Survey Tool Data.** Add information so that the Survey Tool can display these properly to translators
    1. PathHeader.txt (tools/java/org/unicode/cldr/util/data/) - provides the information for what section of the Survey Tool this item shows up in, and how it sorts.  
        1. Edit as described in [PathHeader](https://cldr.unicode.org/development/updating-dtds).     
    2.  PathDescription.txt (tools/java/org/unicode/cldr/util/data/) - provides a description of what the field is, for translators.
        1. If it needs more explanation, add a section (or perhaps a whole page) to the translation guide, eg http://cldr.org/translation/plurals.
        2. For an example, see [8479](https://cldr.unicode.org/index/bug-reports#TOC-Filing-a-Ticket)
    3. Placeholders.txt - provides information about the placeholders, if there can be any.
         1. If the value has placeholders ({0}, {1},...) then edit this file as described in [Placeholders](https://cldr.unicode.org/development/updating-dtds).
    4. The coverageLevels.xml (common/supplemental/coverageLevels) - sets the coverage level for the path.
        1. **\[TBD - John\]**
    5. *Making sure paths are visible.*   
        1. There are 3 ways for paths to show up in ST even though there are no values in root. See Visible Paths below
        2. **Examples:** For any value that has placeholders, or is used in other values that have placeholders, add handling code to the **test/ExampleGenerator** so that survey tool users see examples of your structure in place.   
        3. **Cleaning up input.** If there are things you can do to fix the user data on entry, add to **test/DisplayAndInputProcessor**
3. **Survey Tool Tests.** Add those needed to CheckCLDR
    1. In particular, add to CheckNew so that people see it **\[TBD, fix this advice\]**
        1. If the user's input could be bad, add a survey test to one or more of the tests subclassed from CheckCLDR, to check for bad user input.
            1. Look at test/**CheckDates** to see how this is done.    
            2. Run test/**ConsoleCheckCLDR** with various types of invalid input to make sure that they fail.   
    2. To update the casing files used by CheckConsistentCasing , run org.unicode.cldr.test.CasingInfo -l \<locale\_regex> which will update the casing files in common/casing. When you check this in, sanity check the values, because in some cases we have have had different rules than just what the heuristics generate.
    3. TEST out the **SurveyTool** to verify that you can see/edit the new items. If users should be able to input data and are not able to, the item has not been properly added to CLDR. See [Running the Survey Tool in Eclipse](https://cldr.unicode.org/development/running-survey-tool).
4.  **Data.**
    1. Add necessary data to root and English. 
    2. (Optional) add additional data for locales (if part of main). If the data is just seed data (that you aren't sure of), make sure that you have draft="unconfirmed" on the leaf nodes.
        
### supplementalData.dtd

1. Add code to util/SupplementalDataInfo to fetch the data.
2. You should develop a chart program that shows your data in http://www.unicode.org/cldr/data/charts/supplemental/index.html
    

### Structure Requirements

The following are required for elements, attributes, and attribute values.

#### Elements

We never have "mixed" content. That is, no element values can occur in anything but leaf nodes. You can never have \<x>abcd\<y>def\</y>\</x>. You must instead introduce another element, such as: \<x>\<z>abcd\</z>\<y>def\</y>\</x>

There is a strong distinction between *rule elements and structure elements*. Example: in collations you have \<p>x\</p>\<p>y\</p> representing x < y. Clearly changing the order would cause problems! There are restrictions on this, however:

1. Rule elements must be written in the same order they are read.  
2. They can't inherit.  
3. You can't (easily) add to them programmatically. 
4. You can't mix rule and structure elements under the same parent element. That is, if you can have \<x>\<y>...\</y>\<z>...\</z>\</x>, then either y and z must *both* be rule or *both* be structure elements.
5. In our code, rule elements have their ordering preserved by adding a fake attribute added when reading, \_q="nnn".
6. The CLDRFile code has a list of these, in the right order, as **orderedElements**. If you ever add an rule element to a DTD, you MUST add it there. Be careful to preserve the above invariants.
    - Note: we should change the name *orderedElements* for clarity.

In order to write out an XML file correctly, we also have to know the valid ordering of paths for elements that are not ordered. This ordering is generated automatically from the DTD, constructed by merging. ***If there are any cycles in the ordering, then the CLDR tools will throw an exception, and you have to fix it.*** That also means that we cannot have complicated DTDs; each non-leaf node **MUST** be of the form:
- \<!ELEMENT foo (alias (*first?*, *second*\*, *third*?, ... special\*))>.

The subelements of an element will vary between \* and ?. Note however that all leaf nodes MUST allow for the attributes alt=... draft=... and references=.... So that the alt can work, the leaf nodes MUST occur in their parent as \*, not ?, even if logically there can be only one. For example, even though logically there is only a single quotationStart, we see:
- \<!ELEMENT delimiters (alias | (quotationStart\*, ...
    
#### Attributes

The attribute order is much more flexible, since it doesn't affect the validity of the file. That is, in XML the following are equal:
- \<info iso4217="ADP" digits="0" rounding="0"/>
- \<info digits="0" rounding="0" iso4217="ADP"/>

However, when this is turned into a path, the order does matter. That is, as *strings* the following are *not* equal

- //supplementalData/currencyData/fractions/info\[@iso4217="ADP"\]\[@digits="0"\]\[@rounding="0"\]
- //supplementalData/currencyData/fractions/info\[@digits="0"\]\[@rounding="0"\]\[@iso4217="ADP"\]
    
The ordering of attributes in the string path and in the output file is controlled by the ordering in the DTD. Certain attributes always come first (like \_q and type), and certain others always come last (like draft and references). Normally you add new attributes to the middle somewhere.

When computing the file ordering, we compare paths using CLDRFile.ldmlComparator. Here is the basic ordering algorithm:

Walk through the elements in the path. For each element and its attributes:

1. compare the corresponding elements at that level in the respective paths; if unequal, return their ordering
    - If they are orderedElements, treat them as equal (the \_q attributes will distinguish them). 
    - Otherwise the "less than" ordering is given by elementOrdering.       
2. otherwise compare the respective attributes and attribute values, one by one:
    1. if the attributes are unequal, return their ordering (according to attributeOrdering) 
    2. if the attribute values are unequal, return their ordering
        
While attribute value orderings are mostly alphabetic, we do have a number of tweaks in getAttributeValueComparator so that values come in a reasonable order, such as "sun" < "mon" < "tues" < ...

There is an important distinction for attributes. The **distinguishing** attributes are relevant to the identity of the path and for inheritance. For example, in <language type="en"...> the type is a distinguishing attribute. The **non-distinguishing** attributes instead carry information, and aren't relevant to the identity of the path, nor are they used in the ordering above. ***Non-distinguishing elements in the ldml DTD cause problems: try to design all future DTD structure to avoid them; put data in element values, not attribute values.*** It is ok to have data in attributes in the other DTDs. The distinction between the distinguishing and non-distinguishing elements is captured in the distinguishingData in CLDRFile. So by default, always put new ldml attributes in this array.

- *(Note: we should change this to be exclusive instead of inclusive, to reduce the possibility for error.)*
    
#### Attribute Values

We use some default attribute values in our DTD, such as

- \<!ATTLIST decimalFormat type NMTOKEN **"standard"** >
    
This was a mistake, since it makes the interpretation of the file depend on the DTD; we might fix it some day, maybe if we go to Relax, but for now just don't introduce any more of these. It also means that we have a table in CLDRFile with these values: defaultSuppressionMap.

When you make a draft attribute on a new element, don't copy the old ones like this:

\<!ATTLIST xxx draft ( approved | contributed | provisional | unconfirmed | true | false ) #IMPLIED >\<!-- true and false are deprecated. -->

That is, we *don't* want the deprecated values on new elements. Just make it:

\<!ATTLIST xxx draft ( approved | contributed | provisional | unconfirmed ) #IMPLIED >

The DTD cannot do anything like the level of testing for legitimate values that we need, so supplemental data also has a set of attributeValueValidity.xml data for checking attribute values. For example, we see:

- \<attributeValues dtds='supplementalData' elements='calendarPreference' attributes='ordering' type='list'>$\_bcp47\_calendar\</attributeValues>
        

This means that whenever you see any matching dtd/element/attribute combination, it can be tested for a list of values that are contained in the variable \$\_bcp47\_calendar. Some of these variables are lists, and some are regex, and some (those with $\_) are generated internally from other information. When you add a new attribute to ldml, you must add a \<validity> element unless it is a closed set.

#### No default attribute values

The ones we have in CLDR were (in hindsight) a mistake, since it makes the interpretation of the file depend on the DTD; we might fix it some day, maybe if we go to Relax, but for now just don't introduce any more of these. It also means that for writing out the files we have a table in CLDRFile with these values: defaultSuppressionMap and in supplementalMetadata *\<suppress>*.

#### Don't Reuse

For many many reasons, you never reuse an element name or attribute name unless you mean precisely the same thing, and the item is used in the same way. So to="2009-05-21" is always an attribute that means an end date. Be very careful about new elements with the same name as old ones. You can't have \<territory> be an orderedElement in one place, and a non-orderedElement in another. The attribute type=... is always used as an id. For historial reasons, sometimes it is distinguishing and sometimes note (this is very painful, don't add to it!). It is also not used as the id in numberingSystems.

## Root Aliases

If your new structure should have aliases, such as when the "narrow" values should default to the "short" values, which should default to the regular values, then you need to add aliases in root.xml. Look at examples there for how to do this.

## PathHeader

PathHeader.txt determines the placement and ordering in SurveyTool. It consists of a sequence of regex lines of the following form:

\<regex> ; \<section> ; \<page> ; \<header> ; \<code>

Here's an example:

//ldml/dates/timeZoneNames/metazone\[@type="%A"\]/%E/%E ; Timezones ; &metazone($1) ; $1 ; $3-$2

### Key Features

These are also in the header of PathHeader.txt:

- \# Be careful, order matters. It is used to determine the order on the page and in menus. Also, be sure to put longer matches first, unless terminated with $.
    - \# The quoting of \\\[ is handled automatically, as is alt=X
    - \# If you add new paths, change @type="..." => @type="%A"   
    - \# The syntax &function(data) means that a function generates both the string and the ordering. The functions MUST be supported in PathHeader.java    
    - \# The only function that can be in Page right now are &metazone and &calendar, and NO functions can be in Section       
    - \# A \* at the front (like \*$1) means to not change the sorting group.
        
There are a set of variables at the top of the file. These all are in parens, so the %A, %E, and %E correspond to the $1, $2, and $3 in the \<section> ; \<page> ; \<header> ; \<code>

The order of the section and page is determined by the enums in the PathHeader.java file. So the \<section> and \<page> must correspond to those enum values.

### Uniqueness is Vital

The results from PathHeader must be unique: that is, if the source paths are different, then at least one of \<section> ; \<page> ; \<header> ; \<code> must be different.

### Changing Order

If you need to change the order of the header or code or the appearance programmatically, then you need to create a function (call it xyz), and use it in the PathHeader.txt file (eg &xyz($1)). In PathHeader.java, search for *functionMap* to see examples of these.

The order of the header and then of the code within the same header is normally determined by the ordering in the file. To override this, set the order field in your function. For example, the following gets integer values and changes them into real ints for comparison.

**int** m = Integer.*parseInt*(source);

*order* = m;

There is also a "suborder" used in a few cases for the code. You probably don't need to worry about this, but here is an example. Ask for help on the cldr-dev list if you need this.

*suborder* = **new** SubstringOrder(source, 1);

The return value is the appearance to the user. For example, the following changes integer months into strings for display:

**static** String\[\] *months* = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Und" };

...

**return** *months*\[m - 1\];

## Placeholders

If a value has placeholders, edit Placeholders.txt:

1. Add 1 item per placeholder, with the form
    - \<regex> ; {0}=\<message\_name> \<example> ; {1}=\<message\_name> \<example> ...   
    - ^//ldml/units/unit\\\[@type="day%A"\]/unitPattern ; {0}=NUMBER\_OF\_DAYS 3 
2. There is a variable %A that will match attribute value syntax (or substrings).
3. \<example> may contain spaces, but \<message\_name> must not.
4. For an example, see [8484](https://cldr.unicode.org/index/bug-reports#TOC-Filing-a-Ticket)
5. Check that the ConsoleCheckCLDR **CheckForExamplars** fails if there are no placeholders in the value
6. Note: we should switch methods so that we don't need to quote \\\[, etc, but we haven't yet.
    
## PathDescription

This file provides a description of each kind of path, and a link to a section of https://cldr.unicode.org/translation. Easiest is to take an existing description and modify.

## Coverage

Coverage determines the minimum coverage level at which a given item will appear in the survey tool. If a given field is not in coverage, then the item will not appear in the survey tool at all. This data is required for the elements in /main/.

The file **common/supplemental/coverageLevels.xml** is a series of regular expressions describing the paths and the coverage levels associated with each. The file also gives you the ability to define a "coverage variable", which can then be used as a placeholder in the regular expressions used for matching. Always try to be as exact as possible and avoid using wildcards in the regular expressions, as they can impact lookup performance.

Coverage values are currently numeric, although we may change them to be words in the near future in order to make them easier to understand. The coverage level values are:

10 = Core data, 20 = POSIX, 30 = Minimal, 40 = Basic, 60 = Moderate, 80 = Modern, 100 = Comprehensive

Example: The following two lines define the coverage for the exemplar characters items. Note that "//ldml" is automatically prepended to the path names, in order to make the paths in this file smaller.

\<coverageVariable key="%exemplarTypes" value="(auxiliary|index|punctuation)"/>

\<coverageLevel value="10" match="characters/exemplarCharacters\[@type='%exemplarTypes'\]"/>

## LDML2ICU

Modify the following files as described in [ldml2icu\_readme.txt](https://home.unicode.org/basic-info/projects/#!/repos/cldr/trunk/tools/java/org/unicode/cldr/icu/ldml2icu_readme.txt). This will allow NewLdml2IcuConverter.java to work properly so that the data can be read into ICU and tested there.

1. ldml2icu\_locale.txt and/or
2. ldml2icu\_supplemental.txt
    
Unfortunately, you have to change input parameters to get the different kinds of generated files. Here's an example:

\-s {workspace-cldr}/common/supplemental

\-d {workspace-temp}/cldr/icu/

\-t supplementalData

\-k

Use -k to build into a single file, which is helpful for checking the supplemental data. There are a few other useful parameters if you look at the top of NewLdml2IcuConverter.

### Warning

If you add a new kind of file or directory, you may have to adjust the tool to make sure it is seen and built. For example, if you add a new kind of supplemental file, you also have to modify SupplementalMapper.fillFromCldr(...).

## Visible Paths

There are three ways for paths to show up in the Survey Tool (and in other tooling!) even if the value is null for a given locale. These are important, since they determine what users will be able to enter.

1. **root.** This is the simplest, and should always be used whenever there is a 'real' fallback value for the path, and the path is not part of an algorithmically computed set. It also has the aliases for paths that get special inheritance.
2. **code\_fallback.** This is used for all algorithmically computed paths *that **don't** depend on the locale*. For example, the paths for language codes, currency codes, region codes, etc. are here.
    - To modify, go to XMLSource.java (tools/java/org/unicode/cldr/util/) and update constructedItems to add special paths for items that should appear in locales even though there is no corresponding item in root (e.g. for localeDisplayNames including standard language codes and regional variants, and for all alt="short" or alt="variant" forms).
    - Check to make sure that all of the special alt values in en.xml are there.
1. **extraPaths.** This is used for algorithmically computed paths *that **do** depend on the locale*. For example, we generate count values based on the plural rules. The 'other' form must be in root, but all other forms are calculated here. This should not be overused, since it is recalculated dynamically, whereas root and code\_fallback are constant over the life of the ST.
    - To modify, look at CLDRFile.getRawExtraPaths().
        

### Gotchas

- Even if root, code\_fallback, or extraPaths are set up right, the data may not be visible in ST. If it should show up but isn't, look at:
    - **PathHeader:** Special items are suppressed (they all have HIDE on them). This is used for all paths that don't vary by locale. Paths can also be marked as having unmodifiable values.
    - **Coverage:** If a path has too high a coverage level, then it will be hidden.
    - **Other stuff?** \[Steven to fill out\].
            

### OK if Missing

Certain paths don't have to be present in locales. They are not counted as Missing in the Dashboard and shouldn't have an effect on coverage. To handle these, modify the file [missingOk.txt](https://cldr.unicode.org/index/bug-reports#TOC-Filing-a-Ticket) to provide a regex that captures those paths. Be careful, however, to not be overly inclusive: you want all and only those paths that are ok to skip. Typically those are paths for which root values are perfectly fine.

## Examples of DTD modifications

The following is an example of the different files that may need to be modified. It has both count= and a placeholder, so it hits most of the kinds of changes.
- https://cldr.unicode.org/index/bug-reports#TOC-Filing-a-Ticket
    

## Modifying English/Root

Whenever you modify values in English or Root, be sure to run GenerateBirth as described on [Updating English/Root](https://cldr.unicode.org/development/cldr-development-site/updating-englishroot) and check in the results. That ensures that CheckNew works properly. This must be done before the Survey Tool starts or is in the Submission Phase.

## Validation

- **Do the steps on** [**Running Tests**](https://cldr.unicode.org/development/running-tests)
    

## Debugging Regexes

- Moved to [**Running Tests**](https://cldr.unicode.org/development/running-tests)

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)