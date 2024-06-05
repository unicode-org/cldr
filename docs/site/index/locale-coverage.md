---
title: Locale Coverage Special Data
---

# Locale Coverage Special Data

## Missing Features

The following may be listed as Missing features in a Locale Coverage chart, such as [v43 Locale Coverage](https://unicode-org.github.io/cldr-staging/charts/43/supplemental/locale_coverage.html).

### Core

These are supplied or generated from data supplied in [Core Data for New Locales](https://cldr.unicode.org/index/cldr-spec/core-data-for-new-locales).

1.  ***default\_content** — required in supplied core data*
2.  ***country\_data** — required in supplied core data* 
3.  ***time\_cycle** — required in supplied core data* 
4.  ***likely\_subtags*** — Based on the language population data, a likely subtag mapping is generated. For example, from "de" the likely subtags are "de\_Latn\_DE". 
5.  ***orientation*** — generated from exemplar data

### Moderate

The following are needed at the Moderate level. *The first three should be present before submitting other moderate data*

1.  ***casing** — for bicameral scripts, what is the normal casing for different kinds of fields (country names, language names, etc). Used internally in the Survey Tool.*
2.  ***plurals** — the number of different plural forms, and the rules for deriving them. See [Plural Rules](https://cldr.unicode.org/index/cldr-spec/plural-rules)* and [Plurals & Units](https://cldr.unicode.org/translation/getting-started/plurals)  
3.  ***ordinals** — the number of different plural forms, and the rules for deriving them. See [Plural Rules](https://cldr.unicode.org/index/cldr-spec/plural-rules)* and [Plurals & Units](https://cldr.unicode.org/translation/getting-started/plurals)  
4.  ***collation** — rules for the sorting order for a language.*
    
### Modern

The following are needed at the Modern level. *The **grammar** should be present before adding grammatical forms (eg for units)*

1. ***grammar** — what are the grammatical forms used in a language, in particular usages.*
    
2. ***romanization** — what are rules for romanizing the language's script.*

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)