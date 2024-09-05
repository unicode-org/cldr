---
title: Language/Locale Name Patterns
---

# Language/Locale Name Patterns

Locale display patterns are used to format a compound language (locale) name such as 'en\_AU' or 'uz\_Arab'. The following patterns are used.

| Type | Example | Usage Example |  Meaning  |
|---|---|---|---|
| locale pattern | { BASE_LANGUAGE **} (** { MODIFIERS } **)** | English **(** South Africa **)** | Pattern used to combine the base language (eg, "English") with variants (eg, "South Africa") |
| locale separator | **,**    | Uzbek (Cyrillic **,** Uzbekistan) | Text used to separate different variants ( script , country/region , etc.) |
| locale option pattern | {0} **:** {1}   | German (Currency **:** USD)  | When the variant is a locale option (see  Locale Option Names ), and there is not a single name for the Option+Value, this pattern is used. |

For example, take "en\_AU". First the language code 'en' is translated, such as to "anglais", then the country is translated, such as "Australie". The patterns is used to put those together, into something like "anglais (Australie)". This works the same way if there is a script; for example, "uz-Arab" => "ouzbek (arabe)".

If there is both a script and a region, then a list is formed using the *separator*, then {MODIFIERS} is replaced by that list, such as "uz-Arab-AF" => "ouzbek (arabe, Afghanistan)"

For certain compound language (locale) names, you can also supply specific translations. Thus for the whole locale 'en\_AU', you can provide a translation like "Australian English".

Code patterns are used to format a language, script or locale for display. For example, the language code pattern would be translated from "Language: {0}" in English to "langue : {0}" in French, and would be used to format the language "ouzbek" into "langue : ouzbek".

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)