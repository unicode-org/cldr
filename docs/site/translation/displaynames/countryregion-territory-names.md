---
title: Country/Region (Territory) Names
---

# Country/Region (Territory) Names

Country and region names (referred to as Territories in the Survey Tool) may be used as part of [Language/Locale Names](https://cldr.unicode.org/translation/displaynames/languagelocale-names), or may be used in UI menus and lists to select countries or regions. 

## General Guidelines

Please follow these guidelines:

1. Use the most neutral grammatical form for the country/region that is natural for these two usages above. If there is no single form that can accomplish that, favor the usage within UI menus.
2. Use the capitalization that would be appropriate in the middle of a sentence; the \<contextTransforms> data can specify the capitalization for other contexts. For more information, see [Capitalization](https://cldr.unicode.org/translation/translation-guide-general/capitalization).
3. Each of the names **must** be unique (see below).
4. Don't use commas and don't invert the name (eg use "South Korea", not "Korea, South").

## Customary Names

*The ISO names and the "official" names are often not necessarily the best ones.* The goal is the most customary name used in your language, even if it is not the official name. For example, for the territory name in English you would use "Switzerland" instead of "Swiss Confederation", and use "United Kingdom" instead of "The United Kingdom of Great Britain and Northern Ireland". One of the best sources for customary usage is to look at what common major publications such as newspapers and magazines do, the equivalents of *The Economist, NY Times, BBC, WSJ*, etc. in your language. You can look at style guides if available or at a sampling of pages, but favor publications’ rather than academic style guidelines. For example, to see how "Congo" is used in French, one might search [*for Congo on Le Monde*](http://www.google.com/search?q=Congo+site%3Alemonde.fr) and on other publications.

Also look at frequency data: for example, at the time of this writing, "Côte d’Ivoire" has [117M](https://www.google.com/search?hl=en&q=%22C%C3%B4te%20d%27Ivoire%22) hits on Google in English, while "Ivory Coast" has [99M](https://www.google.com/search?hl=en&q=%22Ivory%20Coast%22). That makes them roughly equal, and other factors come into play. Favor shorter names, all other things being (roughly) equal, and consider carefully politically sensitive names (see below). The most customary name may change over time, but this tends to happen slowly; we do not want changes between versions without good cause. 

## Geopolitically Sensitive Names

Some country/region names need special treatment to avoid geopolitical sensitivity or ambiguity. 

- In some cases, parentheses are used purely to disambiguate. For example:
	- Cocos (Keeling) Islands
	- Congo (DRC)
	- Congo (Republic)
- In other cases, special default or variant forms may be required due to geopolitical considerations. For example:
	- Falkland Islands (Islas Malvinas)
	- Myanmar (Burma)
- Some of the politically sensitive cases have a "short" or a "variant" marker indicating an alternative form. (See table below for examples)
	- Forms marked as "short" are typically more informal, and shorter than the default (non-alt) form, such as "US instead of "United States".
	- Forms marked as "variant" represent alternate names that may be appropriate in certain context.
- In general strive for names that will be acceptable to the largest audience, and that are least likely to be sensitive or to cause offense.

The following is a summary of these issues for some key regions. Some of these may require parentheses in your language for disambiguation.

| Code | Alt | English Name | Instructions/Comments |
|---|---|---|---|
| CD | *none* | Congo - Kinshasa | Use the customary name in your language . The capital may or may not be included, but the name must be different than for CG to distinguish between these two countries. You may also use the variant term for CD here if it is preferred as the default .  |
|  | variant | Congo (DRC) | Include an abbreviation for the full name ( Democratic Republic of the Congo) in  parentheses  (the name must be different than for CG). If you use this form for the default (non-Alt) name, then select the code value (CD) here rather than repeating the same form twice. See Unused Variants. |
| CG | *none* | Congo - Brazzaville | Use the customary name in your language . The capital may or may not be included, but the name must be different than for CD to distinguish between these two countries. You may also use the same variant term for CG here if it is preferred as the default. |
|  | variant | Congo  ( Republic) | Include an abbreviation for the full name (Republic of the Congo) in  parentheses  (the name must be different than for CD). If you use this form for the default (non-Alt) name, then select the code value (CG) here rather than repeating the same form twice. See Unused Variants. |
| FK | *none* | Falkland Islands | Use the customary form of the name in your language. It may correspond to either "Falkland" or "Malvinas" naming depending on your locale. |
|  | variant | Falkland Islands (Islas Malvinas) | Include  both the names corresponding to  “Falkland Islands” and “Islas Malvinas”. The most customary one for readers of your language should be first, with the second one following in  parentheses . For example, the Spanish translation might be “Islas Malvinas  ( Falkland Islands)”. In your language it may not be necessary to translate the word for islands/islas twice, so the name may correspond to “Falkland Islands  ( Malvinas)” or “Islas Malvinas  ( Falkland)”.  See  Duplicate Names , however. |
| MK | *none* | North Macedonia | Use the new name as of 2019, the equivalent of North Macedonia. |
|  | variant | no longer used | This pre-2019 variant formerly included just “Macedonia” plus an abbreviation for  “Former Yugoslav Republic of Macedoni a” in parentheses. With the 2019 name change to North Macedonia, it is no longer used. |
| PS | *none* | Palestinian Territories | Include a term like "territories" |
|  | short | Palestine | Use what is most customary in your language; normally that would be a simple name, without any of the terms “Occupied”, “Territories”, “West Bank”, or “Gaza”. |
| HK | none | Hong Kong SAR China | Follow "Hong Kong" with an abbreviation for "Special Administrative Region of the People’s Republic of China" . It can be the equivalent of SARC, or SAR China, in your language. |
|  | short | Hong Kong | Don’t use the abbreviation for “SAR China”, just the common name by itself. |
| MO | *none* | Macau SAR China | Follow "Macau" with an abbreviation for "Special Administrative Region of the People’s Republic of China" . It can be the equivalent of SARC, or SAR China, in your language. |
|  | short | Macau | Don’t use the abbreviation for “SAR China”, just the common name by itself. |
| CI | *none* | Côte d’Ivoire | For English, we use the French name. In your language, use what is most common: that may be a translation of “Ivory Coast”, a transliteration of “Côte d’Ivoire”, etc. |
|  | variant | Ivory Coast | If more than one form of this name is used in your locale, include the secondary form here. If no variant form is used, then select the code value (CI) here rather than repeating the same form twice. See Unused Variants. |
| TL | *none* | Timor-Leste | For English, we use the Portuguese name. In your language use what is most common: that may be translation of "East Timor", a transliteration of "Timor-Leste", etc. |
|  | variant | East Timor   | If more than one form of this name is used in your locale, include the secondary form here. If no variant form is used, then select the code value (TL) he re rather than repeating the same form twice. See Unused Variants. |
| MM | *none* | Myanmar (Burma) | Use the names corresponding to “Myanmar”  and “Burma”, with the one corresponding to “Burma” in  parentheses . See  Duplicate Names , however. |
| MM | short | Myanmar | Use the name corresponding to just “Myanmar” |
| SZ | *none* | Eswatini | The new name introduced in 2018 |
| SZ  | variant | Swaziland | The pre-2018 name |

| Code | Possible Pairs of Translations |  |  |  |  | Comments |
|---|---|---|---|---|---|---|
| CD | Democratic Republic of the Congo | **or** | Congo - Kinshasa | **or** | Congo - Kinshasa | *See sensitive names above* |
| CG | Congo |  | Congo - Brazzaville |  | Congo |   |
| 003 | North America | **or** | North American Continent | **or** | North America |   |
| 021 | Northern America |  | Northern America |  | Americas north of Mexico |   |
| 018 | Southern Africa | **or** | Southern Region of Africa | **or** | … |   |
| ZA | South Africa |  | South Africa |  | … |  |
| 057 | Micronesian Region | **or** | Micronesian Region | **or** | Micronesian Region |   |
| FM | Micronesia |  | Micronesia (FS) |  | Micronesian States | *FS = “Federated States”* |

## Unique Names

**All names must be unique within a given category:** Names include countries, some parts of countries (such as Hong Kong) with special status, and so-called *macroregions*: continents and subcontinents, as defined by a UN standard. 

Therefore, you cannot use the same translated names for different codes. For example: 

- For the codes CD and CG, *only one can be called "Congo".*
- For the codes 018 and ZA, you can't give the same name to *South Africa* (the country) and to *Southern Africa* (the southern region of the continent of Africa), even though there may be no distinction in your language between the terms for "*South*" and "*Southern*".
- For the codes 003 and 021, *you need to distinguish North America* (the continent that extends down to Panama) and *Northern America* (the region of the Americas north of Mexico).

***When there is a conflict between country name and macroregion name, the country name should be the most natural:*** generally you'll adjust instead the name of the macroregion. So you might say the equivalent of "South Region of Africa", or add clarifying language like "*Amérique du Nord continentale*" vs "*Amérique du Nord*". If you have any question as to the extent of any region, see Territory Containment.

## Duplicate Names

If in your language, one of the disambiguating names (such as in “Myanmar (Burma)”) either is not customarily used or is the same as the ‘main’ name, you don't need to include it in parentheses.

## Unused Variants

There are times in a given language where there is no difference between the default (no-Alt)“ and Alt (variant or short) forms or where there could be an Alt form, but it would not be customarily used. In this case, the code value should be used in place of an Alt form, rather than repeating the default form. For example, in French locales there is no difference between the name of "Côte d’Ivoire" and "Ivory Coast", so should vote for "CI" as the value for the Alt-variant form.

Similarly, where there is no special name for a language+region combination, the code should be used. For example, for English we have the forms:

|  |  |  |  |
|---|---|---|---|
| 1 | American English | en_US |  |
| 2 | US English | en_US | alt=short |

These override the normal constructions, which would be:

|  |  |  |  |
|---|---|---|---|
| 1 | English (United States) | en_US |  |
| 2 | English (US) | en_US | alt=short |

If a particular language would just use the normal constructions, such as in the following, then the code "en\_US" should be the contents. 

|  |  |  |  |
|---|---|---|---|
| 1 | Englisch (Vereinigte Staaten) | en_US |  |
| 2 | Englisch (USA) | en_US | alt=short |

## EU Names

The EU names on [Annex A5 List of countries, territories and currencies](http://publications.europa.eu/code/en/en-5000500.htm) are generally a good guide, ***however,*** they cannot be used as-is.

1. They are incomplete, not including continental regions, the EU itself, ZZ (Unknown region), and some dozen others.
2. They have incorrect country codes (UK and EL for GB and GR).
3. They do not have the variant names needed for CLDR.
4. They use the phrasing "X, The" which is not used in CLDR, as in the EU’s "Gambia, The".
5. They are sometimes unnecessarily longer than the CLDR names (SH, TF, UM, VA).
6. They differ in important ways for some other codes, such as for ones listed above.

## Subdivision Names

CLDR also supports [subdivision names](http://www.unicode.org/cldr/charts/latest/supplemental/territory_subdivisions.html), such as for Scotland:

|   |   |
|---|---|
| **Écosse** | ·fr· |
| **Escocia** | ·es· ·gl· |
| **Escócia** | ·pt· |
| **Escòcia** | ·ca· |
| **Eskozia** | ·eu· |

The names follow the same basic considerations as for Country/Region names. There are some additional considerations.

1. The names only need to be unique within the surrounding Country/Region. That is, it is not a problem for a subdivision of Argentina to have the same name as a subdivision of Chile.
2. Sometimes a name may include a category, such as New York State or Canton Zurich. These category words should be omitted where the context makes them clear.
3. Some countries have two subdivisions with the same names, typically of different categories: arb = "Buenos Aires" — the Province while arc = "Buenos Aires" — the City. Add a category where necessary to distinguish them.
	1. In general, favor making better-known entity be the shorter one. In some cases, it may be necessary to add a category to both of the names.
	2. The category may be added in parentheses after the main name; just make sure it would look ok in the form in a list.

**Note:** There are three subdivisions in **Locale Display Names / Territories (Europe):** England, Scotland, and Wales. 

Tip on translating these, for example, see [French](http://st.unicode.org/cldr-apps/v#/fr/T_Europe/). Distinguish the name for “England” from the name for “United Kingdom”, which includes England, Scotland, Wales, and Northern Ireland

## PseudoLocale Names

There are two special region names used for Pseudo Locales. These are special locales that are used in developer testing, and visible to developers in a locale selection list. So that these work properly in such lists, these pseudo-locales also need translated names. However, the names can be simply transliterations or "piecemeal" translations: please don't spend time researching the best name!

| English Name | Description |
|---|---|
| [**Pseudo-Accents**](https://st.unicode.org/cldr-apps/v#/USER/Territories/4ef00bbec7020af2) | Used to specify artificial locale data that is English with superfluous accents (and lengthened). |
| [**Pseudo-Bidi**](https://st.unicode.org/cldr-apps/v#/USER/Territories/33ce17d5a876bf18)| Used to specify artificial locale data with special controls that produce English text but with the characters from right to left. That is used for testing whether displays will work for Arabic, Hebrew, and other RTL languages. |

If there is no good term for "Pseudo" in your language, some options are the equivalent of "Fake" or "Artificial" in your language.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)