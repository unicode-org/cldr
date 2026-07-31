## <a name="Parsing_Dates_Times" id="Parsing_Dates_Times" href="#Parsing_Dates_Times">Parsing Dates and Times</a>

* For general information on lenient parsing, see [Lenient Parsing](tr35.md#Lenient_Parsing) in the core specification. This section provides additional information specific to parsing of dates and times.


* Lenient parsing of date and time strings is especially complicated, due to the large number of possible fields and formats. The fields fall into two categories: numeric fields (hour, day of month, year, numeric month, and so on) and symbolic fields (era, quarter, month, weekday, AM/PM, time zone). In addition, the user may type in a date or time in a form that is significantly different from the normal format for the locale, and the application must use the locale information to figure out what the user meant. Input may well consist of nothing but a string of numbers with separators, for example, "09/05/02 09:57:33".


* The input can be separated into tokens: numbers, symbols, and literal strings. Some care must be taken due to ambiguity, for example, in the Japanese locale the symbol for March is "3 月", which looks like a number followed by a literal. To avoid these problems, symbols should be checked first, and spaces should be ignored (except to delimit the tokens of the input string).


* The meaning of symbol fields should be easy to determine; the problem is determining the meaning of the numeric fields. Disambiguation will likely be most successful if it is based on heuristics. Here are some rules that can help:


* Always try the format string expected for the input text first. This is the only way to disambiguate 03/07 (March 2007, a credit card expiration date) from 03/07 (March 7, a birthday).
* Attempt to match fields and literals against those in the format string, using loose matching of the tokens. In particular, Unicode normalization and case variants should be accepted. Alternate symbols can also be accepted where unambiguous: for example, “11.30 am”.
* When matching symbols, try the narrow, abbreviated, and full-width forms, including standalone forms if they are unique. You may want to allow prefix matches too, or diacritic-insensitive, again, as long as they are unique. For example, for a month, accept 9, 09, S, Se, Sep, Sept, Sept., and so on. For abbreviated symbols (e.g. names of eras, months, weekdays), allow matches both with and without an abbreviation marker such as period (or whatever else may be customary in the locale); abbreviated forms in the CLDR data may or may not have such a marker.
* *** Note: While**: * Note: While parsing of narrow date values (e.g. month or day names) may be required in order to obtain minimum information from a formatted date (for instance, the only month information may be in a narrow form), the results are not guaranteed; parsing of an ambiguous formatted date string may produce a result that differs from the date originally used to create the formatted string.

  * For day periods, ASCII variants for AM/PM such as “am”, “a.m.”, “am.” (and their case variants) should be accepted, since they are widely used as alternates to native formats.
* When a field or literal is encountered that is not compatible with the pattern:
  * Synchronization is not necessary for symbolic fields, since they are self-identifying. Wait until a numeric field or literal is encountered before attempting to resynchronize.
  * Ignore whether the input token is symbolic or numeric, if it is compatible with the current field in the pattern.
* *** Look forward**: * Look forward or backward in the current format string for a literal that matches the one most recently encountered. See if you can resynchronize from that point. Use the value of the numeric field to resynchronize as well, if possible (for example, a number larger than the largest month cannot be a month).

  * If that fails, use other format strings from the locale (including those in `<availableFormats>`) to try to match the previous or next symbol or literal (again, using a loose match).

