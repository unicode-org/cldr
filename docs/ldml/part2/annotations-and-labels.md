## <a name="Annotations" id="Annotations" href="#Annotations">Annotations and Labels</a>

* Annotations provide information about characters, typically used in input. For example, on a mobile keyboard they can be used to do completion. They are typically used for symbols, especially emoji characters.


* For more information, see version 5.0 or [UTR #51, Unicode Emoji](https://www.unicode.org/reports/tr51/). (Note that during the period between the publication of CLDR v31 and that of Emoji 5.0, the “Latest Proposed Update” link should be used to get to the draft specification for Emoji 5.0.)


```dtd
<!ELEMENT annotations ( annotation* ) >

<!ELEMENT annotation ( #PCDATA ) >

<!ATTLIST annotation cp CDATA #REQUIRED >

<!ATTLIST annotation type (tts) #IMPLIED >
```

There are two kinds of annotations: **short names**, and **search keywords**.

With an attribute `type="tts"`, the value is a **short name**, such as one that can be used for text-to-speech.
It should be treated as one of the element values for other purposes.

When there is no `type` attribute, the value is a set of **keywords**, delimited by |.
Spaces around each element are to be trimmed.
The **keywords** are words associated with the character(s) that might be used in searching for the character,
or in predictive typing on keyboards. The short name itself can be used as a keyword.

Here is an example from German:

```xml
<annotation cp="👎">schlecht | Hand | Daumen | nach | unten</annotation>
<annotation cp="👎" type="tts">Daumen runter</annotation>
```

These are intended as search keywords, and not for "triggering" (aka suggesting).

- For triggering, the user is typing out a message and concurrently seeing a few emoji
  displayed adjacent to the virtual keyboard. Selecting the emoji adds it to the message.
  For example, you mention your birthday while writing, and an emoji cake pops up.
  That is typically done with an LLM or similar advanced technology.
- For searching, the user is looking for an emoji in a search box,
  and typing in in words that narrow down a displayed set of emoji.
  For example, you type 'heart', but that has too many hits, so you add 'blue' and get the set of blue hearts.

### <a name="Usage_Model" id="Usage_Model" href="#Usage_Model">Usage Model</a>

The usage model for the search keywords is:

- The user types one or more words in an emoji search field.
- Each word successively narrows a number of emoji in a results box.
    - heart → 🥰 😘 😻 💌 💘 💝 💖 💗 💓 💞 💕 💟 ❣️ 💔 ❤️‍🔥 ❤️‍🩹 ❤️ 🩷 🧡 💛 💚 💙 🩵 💜 🤎 🖤 🩶 🤍 💋 🫰 🫶 🫀 💏 💑 🏠 🏡 ♥️ 🩺
    - blue → 🥶 😰 💙 🩵 🫐 👕 👖 📘 🧿 🔵 🟦 🔷 🔹 🏳️‍⚧️
    - heart blue → 💙 🩵
- A word with no hits is ignored
    - [heart | blue | confabulation] is equivalent to [heart | blue]
- As the user types a word, each character added to the word narrows the results.
- Whenever the list is short enough to scan, the user will mouse-click on the right emoji — so it doesn’t have to be narrowed too far.
    - In the following, the user would just click on 🎉 if that works for them.
    - celebrate → 🥳 🥂 🎈 🎉 🎊 🪅
- The order of words doesn’t matter.

Multiword search keywords are typically broken up into separate parts,
because that works better with the usage model. So [hand | mouth | omg | open | over] covers the phrase "hand over mouth".

### <a name="cp_attribute" id="cp_attribute" href="#cp_attribute">cp attribute</a>

The `cp` attribute value has two formats: either a single string, or if contained within \[…\] a UnicodeSet.
* The latter format can contain multiple code points or strings. A code point pr string can occur in multiple annotation element **cp** values, such as the following, which also contains the "thumbs down" character.


```xml
<annotation cp='[☝✊-✍👆-👐👫-👭💁🖐🖕🖖🙅🙆🙋🙌🙏🤘]'>hand</annotation>
```

Both for short names and keywords, values do not have to match between different languages.
They should be the most common values that people using _that_ language would associate with those characters.
For example, a "black heart" might have the association of "wicked" in English, but not in some other languages.

The cp value may contain sequences, but does not contain any Emoji or Text Variant (VS15 & VS16) characters.
All such characters should be removed before looking up any short names and keywords.

### <a name="SynthesizingNames" id="SynthesizingNames" href="#SynthesizingNames">Synthesizing Sequence Names</a>

* Many emoji are represented by sequences of characters. When there are no `annotation` elements for that string, the short name can be synthesized as follows. **Note:** The process details may change after the release of this specification, and may further change in the future if other sequences are added.


1.  If **sequence** is an **emoji flag sequence**, look up the territory name in CLDR for the corresponding ASCII characters and return as the short name. For example, the regional indicator symbols P+F would map to “Französisch-Polynesien” in German.
2.  If **sequence** is an **emoji tag sequence**, look up the subdivision name in CLDR for the corresponding ASCII characters and return as the short name. For example, the TAG characters gbsct would map to “Schottland” in German.
3.  If **sequence** is a keycap sequence or 🔟, use the characterLabel for "keycap" as the **prefixName** and set the **suffix** to be the sequence (or "10" in the case of 🔟), then go to step 8.
4.  If the **sequence** ends with the string ZWJ + ➡️, look up the name of that sequence with that string removed. Embed that name into the "facing-right" characterLabelPattern and return it.
5.  Let **suffix** and **prefixName** be "".
6.  If **sequence** contains any emoji modifiers, move them (in order) into **suffix**, removing them from **sequence**.
7.  If **sequence** is a "KISS", "HEART", "FAMILY", or "HOLDING HANDS" emoji ZWJ sequence, move the characters in **sequence** to the front of **suffix**, and set the **sequence** to be "💏", "💑", or "👪" respectively, and go to step 7.
    1. A KISS sequence contains ZWJ, "💋", and "❤", which are skipped in moving to **suffix**.
    2. A HEART sequence contains ZWJ and "❤", which are skipped in moving to **suffix**.
    3. A HOLDING HANDS sequence contains ZWJ+🤝+ZWJ, which are skipped in moving to **suffix**.
    4. A FAMILY sequence contains only characters from the set {👦, 👧, 👨, 👩, 👴, 👵, 👶}. Nothing is skipped in moving to **suffix**, except ZWJ.
8.  If **sequence** ends with ♂ or ♀, and does not have a name, remove the ♂ or ♀ and move the name for "👨" or "👩" respectively to the start of **prefixName**.
9.  Transform **sequence** and append to **prefixName**, by successively getting names for the longest subsequences, skipping any singleton ZWJ characters. If there is more than one name, use the listPattern for unit-short, type=2 to link them.
10.  Transform **suffix** into **suffixName** in the same manner.
11. If both the **prefixName** and **suffixName** are non-empty, form the name by joining them with the "category-list" characterLabelPattern and return it. Otherwise return whichever of them is non-empty.

The synthesized keywords can follow a similar process.

1.  For an **emoji flag sequence** or **emoji tag sequence** representing a subdivision, use "flag".
2.  For keycap sequences, use "keycap".
3.  For sequences with ZWJ + ➡️, use the keywords for the sequence without the ZWJ + ➡️.
3.  For other sequences, add the keywords for the subsequences used to get the short names for **prefixName**, and the short names used for **suffixName**.

Some examples for English data (v30) are given in the following table.

###### <a name="Table_Synthesized_Emoji_Sequence_Names" id="Table_Synthesized_Emoji_Sequence_Names" href="#Table_Synthesized_Emoji_Sequence_Names">Table: Synthesized Emoji Sequence Names</a>

| Sequence | Short Name | Keywords |
| --------- | ---------- | -------- |
| 🇪🇺        | European Union | flag |
| #️⃣        | keycap: # | keycap |
| 9️⃣        | keycap: 9 | keycap |
| 💏        | kiss | couple |
| 👩‍❤️‍💋‍👩 | kiss: woman, woman | couple, woman |
| 💑        | couple with heart | love, couple |
| 👩‍❤️‍👩    | couple with heart: woman, woman | love, couple, woman |
| 👪        | family | family |
| 👩‍👩‍👧        | family: woman, woman, girl | woman, family, girl |
| 👦🏻        | boy: light skin tone | young, light skin tone, boy |
| 👩🏿        | woman: dark skin tone | woman, dark skin tone |
| 👨‍⚖        | man judge | scales, justice, man |
| 👨🏿‍⚖        | man judge: dark skin tone | scales, justice, dark skin tone, man |
| 👩‍⚖        | woman judge | woman, scales, judge |
| 👩🏼‍⚖        | woman judge: medium-light skin tone | woman, scales, medium-light skin tone, judge |
| 👮        | police officer | police, cop, officer |
| 👮🏿        | police officer: dark skin tone | police, cop, officer, dark skin tone |
| 👮‍♂️       | man police officer | police, cop, officer, man |
| 👮🏼‍♂️       | man police officer: medium-light skin tone | police, cop, officer, medium-light skin tone, man |
| 👮‍♀️       | woman police officer | police, woman, cop, officer |
| 👮🏿‍♀️       | woman police officer: dark skin tone | police, woman, cop, officer, dark skin tone |
| 🚴        | person biking | cyclist, bicycle, biking |
| 🚴🏿        | person biking: dark skin tone | cyclist, bicycle, biking, dark skin tone |
| 🚴‍♂️       | man biking | cyclist, bicycle, biking, man |
| 🚴🏿‍♂️       | man biking: dark skin tone | cyclist, bicycle, biking, dark skin tone, man |
| 🚴‍♀️       | woman biking | cyclist, woman, bicycle, biking |
| 🚴🏿‍♀️       | woman biking: dark skin tone | cyclist, woman, bicycle, biking, dark skin tone |

For more information, see [Unicode Emoji](https://www.unicode.org/reports/tr51/).

### <a name="Character_Labels" id="Character_Labels" href="#Character_Labels">Annotations Character Labels</a>

```dtd
<!ELEMENT characterLabels ( alias | ( characterLabelPattern*, characterLabel*, special* ) ) >

<!ELEMENT characterLabelPattern ( #PCDATA ) >

<!ATTLIST characterLabelPattern type NMTOKEN #REQUIRED >

<!ATTLIST characterLabelPattern count (0 | 1 | zero | one | two | few | many | other) #IMPLIED > <!-- count only used for certain patterns" -->

<!ELEMENT characterLabel ( #PCDATA ) >

<!ATTLIST characterLabel type NMTOKEN #REQUIRED >
```

* The character labels can be used for categories or groups of characters in a character picker or keyboard palette. They have the above structure. Items with special meanings are explained below. Many of the categories are based on terms used in Unicode. Consult the [Unicode Glossary](https://www.unicode.org/glossary/) where the meaning is not clear.


The following are special patterns used in composing labels.

###### <a name="Table_characterLabelPattern" id="Table_characterLabelPattern" href="#Table_characterLabelPattern">Table: characterLabelPattern</a>

| Type          | English             | Description of the group specified |
| ------------- | ------------------- | ----------------------------------- |
| all           | {0} — all           | Used where the title {0} is just a subset. For example, {0} might be "Latin", and contain the most common Latin characters. Then "Latin — all" would be all of them. |
| category-list | {0}: {1}            | Use for a name, where {0} is the main item like "Family", and {1} is a list of one or more components or subcategories. The list is formatted using a list pattern. |
| compatibility | {0} — compatibility | For grouping Unicode compatibility characters separately, such as "Arabic — compatibility". |
| enclosed      | {0} — enclosed      | For indicating enclosed forms, such as "digits — enclosed" |
| extended      | {0} — extended      | For indicating a group of "extended" characters (special use, technical, etc.) |
| historic      | {0} — historic      | For indicating a group of "historic" characters (no longer in common use). |
| miscellaneous | {0} — miscellaneous | For indicating a group of "miscellaneous" characters (typically that don't fall into a broader class). |
| other         | {0} — other         | Used where the title {0} is just a subset. For example, {0} might be "Latin", and contain the most common Latin characters. Then "Latin — other" would be the rest of them. |
| scripts       | scripts — {0}       | For indicating a group of "scripts" characters matching {0}. The value for {0} may be a geographic indicator, like "Africa" (although there are specific combinations listed below), or some other designation, like "other" (from below). |
| strokes       | {0} strokes         | Used as an index title for CJK characters. It takes a "count" value, which allows the right plural form to be specified for the language. |
| subscript     | subscript {0}       | For indicating subscript forms, such as "subscript digits". |
| superscript   | superscript {0}     | For indicating superscript forms, such as "superscript digits". |

The following are character labels. Where the meaning of the label is fairly clear (like "animal") or is in the Unicode glossary, it is omitted.

###### <a name="Table_characterLabel" id="Table_characterLabel" href="#Table_characterLabel">Table: characterLabel</a>

| Type                        | English                 | Description of the group specified |
| --------------------------- | ----------------------- | ----------------------------------- |
| activities                  | activity                | Human activities, such as running. |
| african_scripts             | African script          | Scripts associated with the continent of Africa. |
| american_scripts            | American script         | Scripts associated with the continents of North and South America. |
| animals_nature              | animal or nature        | A broad category. |
| arrows                      | arrow                   | Arrow symbols |
| body                        | body                    | Symbols for body parts, such as an arm. |
| box_drawing                 | box drawing             | Unicode box-drawing characters (geometric shapes) |
| bullets_stars               | bullet or star          | Unicode bullets (such as • or ‣ or ⁍) or stars (★✩✪✵...) |
| consonantal_jamo            | consonantal jamo        | Korean Jamo consonants. |
| currency_symbols            | currency symbol         | Symbols such as $, ¥, £ |
| dash_connector              | dash or connector       | Characters like _ or ⁓ |
| dingbats                    | dingbat                 | Font dingbat characters, such as ❿ or ♜. |
| downwards_upwards_arrows    | downwards upwards arrow | ⇕,... |
| female                      | female                  | Indicates that a character is female or feminine in appearance. |
| format                      | format                  | A Unicode format character. |
| format_whitespace           | format & whitespace     | A Unicode format character or whitespace. |
| full_width_form_variant     | full-width variant      | Full width variant, such as a wide A. |
| half_width_form_variant     | half-width variant      | Narrow width variant, such as a half-width katakana character. |
| han_characters              | Han character           | Han (aka CJK: Chinese, Japanese, or Korean) ideograph |
| han_radicals                | Han radical             | Radical (component) used in Han characters. |
| hanja                       | hanja                   | Korean name for Han character. |
| hanzi_simplified            | Hanzi (simplified)      | Simplified Chinese ideograph |
| hanzi_traditional           | Hanzi (traditional)     | Traditional Chinese ideograph |
| historic_scripts            | historic script         | Script no longer in common modern usage, such as Runes or Hieroglyphs. |
| ideographic_desc_characters | ideographic desc. character | Special Unicode characters (see the glossary). |
| kanji                       | kanji                   | Japanese Han ideograph |
| keycap                      | keycap                  | A key on a computer keyboard or phone. For example, the "3" key on a phone or laptop would be "keycap: 3" |
| limited_use                 | limited-use             | Not in common modern use. |
| male                        | male                    | Indicates that a character is male or masculine in appearance. |
| modifier                    | modifier                | A Unicode modifier letter or symbol. |
| nonspacing                  | nonspacing              | Used for characters that occupy no width by themselves, such as the ¨ over the a in ä. |
| facing-left                 | facing-left             | Characters that face to the left. Also used to construct names for emoji variants. |
| facing-right                | facing-right            | Characters that face to the right. Also used to construct names for emoji variants. |

### <a name="Typographic_Names" id="Typographic_Names" href="#Typographic_Names">Typographic Names</a>

```dtd
<!ELEMENT typographicNames ( alias | ( axisName*, styleName*, featureName*, special* ) ) >

<!ELEMENT axisName ( #PCDATA ) >
<!ATTLIST axisName type (ital | opsz | slnt | wdth | wght) #REQUIRED >
<!ATTLIST axisName alt NMTOKENS #IMPLIED >

<!ELEMENT styleName ( #PCDATA ) >
<!ATTLIST styleName type (ital | opsz | slnt | wdth | wght) #REQUIRED >
<!ATTLIST styleName subtype NMTOKEN #REQUIRED >
<!ATTLIST styleName alt NMTOKENS #IMPLIED >

<!ELEMENT featureName ( #PCDATA ) >
<!ATTLIST featureName type (afrc | cpsp | dlig | frac | lnum | onum | ordn | pnum | smcp | tnum | zero) #REQUIRED >
<!ATTLIST featureName alt NMTOKENS #IMPLIED >
```

* The typographic names provide for names of font features for use in a UI. This is useful for apps that show the name of font styles and design axes according to the user’s languages. It would also be useful for system-level libraries.


* The identifiers (types) use the tags from the [OpenType Feature Tag Registry](https://learn.microsoft.com/en-us/typography/opentype/spec/featuretags). Given their large number, only the names of frequently-used OpenType feature names are available in CLDR. (Many features are not user-visible settings, but instead serve as a data channel for software to pass information to the font.) The example below shows an approach for using the CLDR data. Of course, applications are free to implement their own algorithms depending on their specific needs.


To find a localized subfamily name such as “Extraleicht Schmal” for a font called “Extralight Condensed”, a system or application library might do the following:

1. Determine the set of languages in which the subfamily name can potentially be returned. This is the union of the languages for which the font contains ‘name’ table entries with ID 2 or 17, plus the languages for which CLDR supplies typographic names.

2. Use a language matching algorithm such as in ICU to find the best available language given the user preferences. The resulting subfamily name will be localized to this language.

3. If the font’s ‘name’ table contains a typographic subfamily name (ID17) in this language and all font variation axes are set to their defaults, return this name.

4. If the font’s ‘name’ table contains a font subfamilyname (‘name’ID2) in this language and all font variation axes are set to their defaults, return this name.

5. If the font has a style attributes (STAT) table, look up the design axis tags and their ordering. If the font has no STAT table, assume \[Width, Weight, Slant\] as axis ordering, and infer the font’s style attributes from other available data in the font (eg. the OS/2 table).

6. For each design axis, find a localized style name for its value.
   1. If the font’s style attributes point to a ‘name’ table entry that is available in the result language, use this name.
   2. Otherwise, generate a fallback name from CLDR style Name data.
      1. The type key is the OpenType axis tag (‘wght’). The subtype and alt keys are taken from the entry in English CLDR where the string is equal to the English name in the font. For example, when the font uses a weight whose English style name is “Extralight”, this will lead to subtype = “200” and alt = “variant”. If there is no match, take the axis value (“200”) for subtype and the empty string for alt.
      2. Look up (type, subtype) in a data table derived from CLDR’s style names. If CLDR supplies multiple alternate names for this (type, subtype), use the one whose “alt” key is matching; otherwise, use the default alternate (which has no “alt” attribute in CLDR).
7. Concatenate the strings, with a separator between them.

