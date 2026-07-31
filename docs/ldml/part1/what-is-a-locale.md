## <a name="Locale" id="Locale" href="#Locale">What is a Locale?</a>

* Before diving into the XML structure, it is helpful to describe the model behind the structure. People do not have to subscribe to this model to use data in LDML, but they do need to understand it so that the data can be correctly translated into whatever model their implementation uses.


* The first issue is basic: _what is a locale?_ In this model, a locale is an identifier (id) that refers to a set of user preferences that tend to be shared across significant swaths of the world. Traditionally, the data associated with this id provides support for formatting and parsing of dates, times, numbers, and currencies; for measurement units, for sort-order (collation), plus translated names for time zones, languages, countries (regions), and scripts. The data can also include support for text boundaries (character, word, line, and sentence), text transformations (including transliterations), and other services.


* Locale data is not cast in stone: the data used on someone's machine generally may reflect the US format, for example, but preferences can typically set to override particular items, such as setting the date format for 2002.03.15, or using metric or Imperial measurement units. In the abstract, locales are simply one of many sets of preferences that, say, a website may want to remember for a particular user. Depending on the application, it may want to also remember the user's time zone, preferred currency, preferred character set, smoker/non-smoker preference, meal preference (vegetarian, kosher, and so on), music preference, religion, party affiliation, favorite charity, and so on.


* Locale data in a system may also change over time: country boundaries change; governments (and currencies) come and go: committees impose new standards; bugs are found and fixed in the source data; and so on. Thus the data needs to be versioned for stability over time.


* In general terms, the locale id is a parameter that is supplied to a particular service (date formatting, sorting, spell-checking, and so on). The format in this document does not attempt to represent all the data that could conceivably be used by all possible services. Instead, it collects together data that is in common use in systems and internationalization libraries for basic services. The main difference among locales is in terms of language; there may also be some differences according to different countries or regions. However, the line between _locales_ and _languages_, as commonly used in the industry, are rather fuzzy. Note also that the vast majority of the locale data in CLDR is in fact language data; all non-linguistic data is separated out into a separate tree. For more information, see _[Language and Locale IDs](#Language_and_Locale_IDs)_.


* We will speak of data as being "in locale X". That does not imply that a locale _is_ a collection of data; it is simply shorthand for "the set of data associated with the locale id X". Each individual piece of data is called a _resource_ or _field_, and a tag indicating the key of the resource is called a _resource tag._



<a name="Identifiers"></a>
