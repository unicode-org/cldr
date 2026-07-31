## <a name="Parsing_Numbers" id="Parsing_Numbers" href="#Parsing_Numbers">Parsing Numbers</a>

The following elements are relevant to determining the value of a parsed number:

* A possible prefix or suffix, indicating sign
* A possible currency symbol or code
* Decimal digits
* A possible decimal separator
* A possible exponent
* A possible percent or per mille character

* Other characters should either be ignored, or indicate the end of input, depending on the application. The key point is to disambiguate the sets of characters that might serve in more than one position, based on context. For example, a period might be either the decimal separator, or part of a currency symbol (for example, "NA f."). Similarly, an "E" could be an exponent indicator, or a currency symbol (the Swaziland Lilangeni uses "E" in the "en" locale). An apostrophe might be the decimal separator, or might be the grouping separator.


Here is a set of heuristic rules that may be helpful:

* Any character with the decimal digit property is unambiguous and should be accepted.

  **Note:** In some environments, applications may independently wish to restrict the decimal digit set to prevent security problems. See [[UTR36](https://www.unicode.org/reports/tr41/#UTR36)].

* The exponent character can only be interpreted as such if it occurs after at least one digit, and if it is followed by at least one digit, with only an optional sign in between. A regular expression may be helpful here.
* For the sign, decimal separator, percent, and per mille, use a set of all possible characters that can serve those functions. For example, the decimal separator set could include all of [.,']. (The actual set of characters can be derived from the number symbols in the By-Type charts [[ByType](tr35.md#ByType)], which list all of the values in CLDR.) To disambiguate, the decimal separator for the locale must be removed from the "ignore" set, and the grouping separator for the locale must be removed from the decimal separator set. The same principle applies to all sets and symbols: any symbol must appear in at most one set.
* Since there are a wide variety of currency symbols and codes, this should be tried before the less ambiguous elements. It may be helpful to develop a set of characters that can appear in a symbol or code, based on the currency symbols in the locale.
* Otherwise, a character should be ignored unless it is in the "stop" set. This includes even characters that are meaningful for formatting, for example, the grouping separator.
* If more than one sign, currency symbol, exponent, or percent/per mille occurs in the input, the first found should be used.
* A currency symbol in the input should be interpreted as the longest match found in the set of possible currency symbols.
* Especially in cases of ambiguity, the user's input should be echoed back, properly formatted according to the locale, before it is actually used for anything.

