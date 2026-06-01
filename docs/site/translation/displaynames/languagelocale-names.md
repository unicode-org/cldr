---
title: Language/Locale Names
---

# Language/Locale Names

Some language names are simple, like "English". However, it is often important to distinguish a variant of the language, even when it is only written. For example, British English and American English are often written differently. In some cases, the difference can be quite substantial, such as when the same language is written with different [scripts](/translation/displaynames/script-names) (aka writing systems, like Latin letters vs Greek letters).

Thus more complex language names may be composed from simple languages plus variants. A pattern is used to control how the translations for language, script, and region codes are composed into a name when the compound code doesn't have a specific translation. An example is "αγγλικά (Αυστραλία)", which has the native name for "English", followed by the native word for "Australia" in parentheses. See [Patterns for Locale/Language Names](/translation/displaynames/languagelocale-name-patterns).

For the simple language names, please follow these guidelines:

- Each of the simple language names **must** be unique.
- Don't use commas and don't invert the name (eg use "French Creole", not "French, Creole").
- Don't use the characters "(" and ")", since they will be confusing in combination with countries or scripts in more complex language names. If you have to use brackets, use square ones: [ and ].
- The most neutral grammatical form for the language name should be chosen.
- Use the capitalization that would be appropriate for a language name in the middle of a sentence; the \<contextTransforms> data can specify the capitalization for other contexts. For more information, see [Capitalization](/translation/translation-guide-general/capitalization).

## Unique Names

There are a few special cases:

- Local variants of a language sometimes need to be translated, such as *Australian English* (internal code: en\_AU) or Simplified Chinese (zh\_Hans).
- "Iberian Portuguese" or "European Portuguese" is the style of Portuguese used in Portugal (as opposed to Brazil)
- Similarly "Iberian Spanish" or "European Spanish" is the style of Spanish specifically used in Spain (as opposed to Latin America).
- "Swiss High German" (*Schweizer Hochdeutsch*), also called "Swiss Standard German", has the code de\_CH.
- "Swiss German" (*Schwyzerdütsch*) has the code gsw.

## Menu variants

For languages that are part of a larger family, the Survey Tool may request translations of a “menu variant” of the language name that puts the family name first so it will be grouped in a menu together with other languages of the same family. For example:

![image](../../images/displaynames/menuVariants1.png)
![image](../../images/displaynames/menuVariants2.png)

If your standard translation of the language name already puts the family name first (as in “Kurdish, Central”) then you can supply the same name as the menu variant.

### Core and Extensions

Sometimes languages and other fields need further qualitification in the context of a menu. For example, when there is only one variant of French shown in a menu, it is fine to just list the language as follows.
* English
* French
* German

However, when there are multiple variants, it is clearer to show the variant information, such as:
* English
* French (Canada)
* French (France)
* German

When the variants differ not by region or script, but by another qualifier, then you may see codes with "-core" or "-extension", as in the following.

<img src="kurdish-example.png" />

There are two separate cases.
1. All of the variants have a qualifier (eg, Northern Sotho, Southern Sotho)
2. One of the variants does not have a qualifier (eg, Kurdish, Sorani Kurdish)

In these cases:

Code | Your Action
--|--
xx-core | Supply the name of the core (eg, Kurdish or Sotho) according to your language. In Case 2 above, this is the same as the “regular” (xx) value.
xx-extension | Supply the name of a qualifier (eg, Kurmanji, Southern, or Northern). 

Examples:

Code | English
-- | --
ckb | Sorani Kurdish
ckb-core | Kurdish
ckb-extension | Sorani
ku | Kurdish
ku-core | Kurdish
ku-extension | Kurmanji

When users see this in a menu, they'll see the core names, with the extensions treated like script or region variants, as in the following:

* English
* French (Canada)
* French (France)
* German
* Kurdish (Kurmanji)
* Kurdish (Sorani)

The separation of the core and extension can also be used in other contexts than messages.
