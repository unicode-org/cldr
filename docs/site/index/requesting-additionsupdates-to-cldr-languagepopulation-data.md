---
title: Requesting Additions/Updates to CLDR Language/Population Data
---

# Requesting Additions/Updates to CLDR Language/Population Data

The main purpose of having language/population supplemental data in CLDR is for generating "likely subtags": determining which languages are likely to be useful in different locations.

It is not a goal for this data to cover all possible languages, or even all the languages that might be used in a given country.

Foremost, the data are intended to cover official languages of each country or region. Official languages are not necessarily the most widely used in a given region, but their status makes them necessary. For example, they may be constitutionally mandated, regionally official, official for use in particular application areas, and so forth. The data also cover languages in widespread use for large populations within countries, attempting to cover somewhere near 100% of the country's population when possible. Other languages with smaller user populations may be included based on special status or other perceived cultural importance within the country.

Before adding data for a language and population, the committee needs to know the importance of the addition. It is not enough that a language be "in use". For example, within a country such as the United States, hundreds of languages are used, sometimes by fairly sizable populations, but they are not all useful additions to the CLDR supplemental data.

For CLDR purposes, the language data focus on the usefulness with computer interfaces, rather than general utility as spoken languages. Data for primarily spoken languages are usually included only where the languages have official status.

Requests to add or change language/population data must provide the following basic information:

- language name    
- 2 or 3-letter language code    
- applicable country/region name    
- applicable country/region code  
- official status (and justification)    
- language population in the region    
- literacy in the language, where possible    
- links to reliable sources for population/literacy data
    

Reliable sources for population data and official status are required for population updates and additions. While [Ethnologue](https://www.google.com/url?q=https%3A%2F%2Fwww.ethnologue.com%2F&sa=D&sntz=1&usg=AOvVaw02Rajsyksb8nOu8MESVtKi) may be a good source for "mother tongue" or native speaker data for more common languages, it is not a sufficient source on its own for population data on most languages. Recent government or NGO-sponsored census data are typically better sources.

For language names and codes, some resources are: [Unicode CLDR charts](http://www.google.com/url?q=http%3A%2F%2Fwww.unicode.org%2Fcldr%2Fcharts%2Flatest%2F&sa=D&sntz=1&usg=AOvVaw0IfeZJzXzVdSLQDbkyoE4x), [IANA Language Subtag Registry](https://www.google.com/url?q=https%3A%2F%2Fwww.iana.org%2Fassignments%2Flanguage-subtag-registry%2Flanguage-subtag-registry&sa=D&sntz=1&usg=AOvVaw0KaE5Pb3Bfy7xyMSIiNrbi), and [Wikipedia](https://www.google.com/url?q=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FMain_Page&sa=D&sntz=1&usg=AOvVaw1NobdoSkQ4MRb3AWb_nNp3) articles on individual languages.

Also for new additions, the request must include a rationale for inclusion and discuss the importance of the addition.

Some examples of CLDR Trac tickets that include sufficient information include these:

http://unicode.org/cldr/trac/ticket/9767

http://unicode.org/cldr/trac/ticket/9680#comment:1

http://unicode.org/cldr/trac/ticket/9609

http://unicode.org/cldr/trac/ticket/9601#comment:2

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)