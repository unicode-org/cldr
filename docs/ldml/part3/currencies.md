## <a name="Currencies" id="Currencies" href="#Currencies">Currencies</a>

```dtd
<!ELEMENT currencies (alias | (default?, currency*, special*)) >
<!ELEMENT currency (alias | (((pattern+, displayName*, symbol*) | (displayName+, symbol*, pattern*) | (symbol+, pattern*))?, decimal*, group*, special*)) >
<!ELEMENT symbol ( #PCDATA ) >
<!ATTLIST symbol choice ( true | false ) #IMPLIED > <!-- deprecated -->
```

> **Note:** The term "pattern" appears twice in the above. The first is for consistency with all other cases of pattern + displayName; the second is for backwards compatibility.

```xml
<currencies>
    <currency type="USD">
        <displayName>Dollar</displayName>
        <symbol>$</symbol>
    </currency>
    <currency type ="JPY">
        <displayName>Yen</displayName>
        <symbol>¥</symbol>
    </currency>
    <currency type="PTE">
        <displayName>Escudo</displayName>
        <symbol>$</symbol>
    </currency>
</currencies>
```

* In formatting currencies, the currency number format is used with the appropriate symbol from `<currencies>`, according to the currency code. The `<currencies>` list can contain codes that are no longer in current use, such as PTE. The `choice` attribute has been deprecated.


The `count` attribute distinguishes the different plural forms, such as in the following:

```xml
<currencyFormats>
    <unitPattern count="other">{0} {1}</unitPattern>
    …
</currencyFormats>
```

```xml
<currency type="ZWD">
    <displayName>Zimbabwe Dollar</displayName>
    <displayName count="one">Zimbabwe dollar</displayName>
    <displayName count="other">Zimbabwe dollars</displayName>
    <symbol>Z$</symbol>
</currency>
```

Note on displayNames:
* In general the region portion of the displayName should match the territory name, see **Part 2** _[Locale Display Name Fields](tr35-general.md#locale_display_name_fields)_.
* As a result, the English currency displayName in CLDR may not match the name in ISO 4217.

To format a particular currency value "ZWD" for a particular numeric value _n_ using the (long) display name:

1. If the numeric value is exactly 0 or 1, first see if there is a count with a matching explicit number (0 or 1). If so, use that string (see [Explicit 0 and 1 rules](#Explicit_0_1_rules)).
2. Otherwise, determine the `count` value that corresponds to _n_ using the rules in _[- Language Plural Rules](#Language_Plural_Rules)_
3. Next, get the currency unitPattern.
   1. Look for a `unitPattern` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root.
   2. If no matching `unitPattern` element was found in the previous step, then look for a `unitPattern` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to root (which has a `unitPattern` element with `count="other"` for every unit type).
   3. The resulting unitPattern element indicates the appropriate positioning of the numeric value and the currency display name.
4. Next, get the `displayName` element for the currency.
   1. Look for a `displayName` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root.
   2. If no matching `displayName` element was found in the previous step, then look for a `displayName` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to, but not including root.
   3. If no matching `displayName` element was found in the previous step, then look for a `displayName` element with no count, starting in the current locale and then following the locale fallback chain up to root.
   4. If there is no `displayName` element, use the currency code itself (for example, "ZWD").
5. Format the numeric value according to the locale. Use the locale’s `<decimalFormats …>` pattern, not the `<currencyFormats>` pattern that is used with the symbol (eg, Z$). As when formatting symbol currency values, reset the number of decimals according to the supplemental `<currencyData>` and use the currencyDecimal symbol if different from the decimal symbol.
   1. The number of decimals should be overridable in an API, so that clients can choose between “2 US dollars” and “2.00 US dollars”.
6. Substitute the formatted numeric value for the {0} in the `unitPattern`, and the currency display name for the {1}.

* While for English this may seem overly complex, for some other languages different plural forms are used for different unit types; the plural forms for certain unit types may not use all of the plural-form tags defined for the language.


* For example, if the currency is ZWD and the number is 1234, then the latter maps to `count="other"` for English. The unit pattern for that is "{0} {1}", and the display name is "Zimbabwe dollars". The final formatted number is then "1,234 Zimbabwe dollars".


---

* When a currency symbol is substitited into a pattern, some spacing adjustments or other adjustments may be necessary depending on the nature of the symbol. In CLDR 42 and later, the preferred way to handle this is via the `alt="alphaNextToNumber"` variant of the `currencyFormat` `pattern`, as described in _[Section 2.4.2: Currency Formats](#Currency_Formats)_. In earlier versions of CLDR this was handled via the `currencySpacing` element as described below. This element is still present in CLDR 42 and its use is described below for implementations that may not yet support the `alt="alphaNextToNumber"` variant of the `currencyFormat` `pattern`.


```xml
<currencySpacing>
  <beforeCurrency>
    <currencyMatch>[:^S:]</currencyMatch>
    <surroundingMatch>[:digit:]</surroundingMatch>
    <insertBetween> </insertBetween>
  </beforeCurrency>
  <afterCurrency>
    <currencyMatch>[:^S:]</currencyMatch>
    <surroundingMatch>[:digit:]</surroundingMatch>
    <insertBetween> </insertBetween>
  </afterCurrency>
</currencySpacing>
```

* This element controls whether additional characters are inserted on the boundary between the symbol and the pattern. For example, with the above `currencySpacing`, inserting the symbol "US\$" into the pattern "#,##0.00¤" would result in an extra _no-break space_ inserted before the symbol, for example, "#,##0.00 US\$". The `beforeCurrency` element governs this case, since we are looking _before_ the "¤" symbol. The `currencyMatch` is positive, since the "U" in "US\$" is at the start of the currency symbol being substituted. The `surroundingMatch` is positive, since the character just before the "¤" will be a digit. Because these two conditions are true, the insertion is made.


* Conversely, look at the pattern "¤#,##0.00" with the symbol "US$". In this case, there is no insertion; the result is simply "US$#,##0.00". The `afterCurrency` element governs this case, since we are looking _after_ the "¤" symbol. The `surroundingMatch` is positive, since the character just after the "¤" will be a digit. However, the `currencyMatch` is **not** positive, since the "\$" in "US\$" is at the end of the currency symbol being substituted. So the insertion is not made.


For more information on the matching used in the `currencyMatch` and `surroundingMatch` elements, see the main document _[Appendix E: Unicode Sets](tr35.md#Unicode_Sets)_.

---

* Currencies can also contain optional grouping, decimal data, and pattern elements. This data is inherited from the `<symbols>` in the same locale data (if not present in the chain up to root), so only the _differing_ data will be present. See the main document _[Multiple Inheritance](tr35.md#Multiple_Inheritance)_.


> **Note:** _Currency values should **never** be interchanged without a known currency code. You never want the number 3.5 interpreted as $3.50 by one user and €3.50 by another._ Locale data contains localization information for currencies, not a currency value for a country. A currency amount logically consists of a numeric value, plus an accompanying currency code (or equivalent). The currency code may be implicit in a protocol, such as where USD is implicit. But if the raw numeric value is transmitted without any context, then it has no definitive interpretation.

* Notice that the currency code is completely independent of the end-user's language or locale. For example, BGN is the code for Bulgarian Lev. A currency amount of <BGN, 1.23456×10³> would be localized for a Bulgarian user into "1 234,56 лв." (using Cyrillic letters). For an English user it would be localized into the string "BGN 1,234.56". The end-user's language is needed for doing this last localization step; but that language is completely orthogonal to the currency code needed in the data. After all, the same English user could be working with dozens of currencies. Notice also that the currency code is also independent of whether currency values are inter-converted, which requires more interesting financial processing: the rate of conversion may depend on a variety of factors.


* Thus logically speaking, once a currency amount is entered into a system, it should be logically accompanied by a currency code in all processing. This currency code is independent of whatever the user's original locale was. Only in badly-designed software is the currency code (or equivalent) not present, so that the software has to "guess" at the currency code based on the user's locale.


> **Note:** The number of decimal places **and** the rounding for each currency is not locale-specific data, and is not contained in the Locale Data Markup Language format. Those values override whatever is given in the currency `numberFormat`. For more information, see _[Supplemental Currency Data](#Supplemental_Currency_Data)_.

For background information on currency names, see [[CurrencyInfo](tr35.md#CurrencyInfo)].

### <a name="Supplemental_Currency_Data" id="Supplemental_Currency_Data" href="#Supplemental_Currency_Data">Supplemental Currency Data</a>

```dtd
<!ELEMENT currencyData ( fractions*, region+ ) >
<!ELEMENT fractions ( info+ ) >

<!ELEMENT info EMPTY >
<!ATTLIST info iso4217 NMTOKEN #REQUIRED >
<!ATTLIST info digits NMTOKEN #IMPLIED >
<!ATTLIST info rounding NMTOKEN #IMPLIED >
<!ATTLIST info cashDigits NMTOKEN #IMPLIED >
<!ATTLIST info cashRounding NMTOKEN #IMPLIED >

<!ELEMENT region ( currency* ) >
<!ATTLIST region iso3166 NMTOKEN #REQUIRED >

<!ELEMENT currency ( alternate* ) >
<!ATTLIST currency iso4217 NMTOKEN #REQUIRED >
<!ATTLIST currency from NMTOKEN #IMPLIED >
<!ATTLIST currency to NMTOKEN #IMPLIED >
<!ATTLIST currency tz CDATA #IMPLIED >
<!ATTLIST currency to-tz CDATA #IMPLIED >
<!ATTLIST currency tender ( true | false ) #IMPLIED >
```

Each `currencyData` element contains one `fractions` element followed by one or more `region` elements. Here is an example for illustration.

```xml
<supplementalData>
    <currencyData>
        <fractions>
        …
        <info iso4217="CHF" digits="2" rounding="5"/>
        …
        <info iso4217="ITL" digits="0"/>
        …
        </fractions>
        …
        <region iso3166="IT">
            <currency iso4217="EUR" from="1999-01-01"/>
            <currency iso4217="ITL" from="1862-8-24" to="2002-02-28"/>
        </region>
        …
        <region iso3166="CS">
            <currency iso4217="EUR" from="2003-02-04"/>
            <currency iso4217="CSD" from="2002-05-15"/>
            <currency iso4217="YUM" from="1994-01-24" to="2002-05-15"/>
        </region>
        …
    </currencyData>
    …
</supplementalData>
```

The `fractions` element contains any number of `info` elements, with the following attributes:

* **iso4217:** the ISO 4217 code for the currency in question. If a particular currency does not occur in the fractions list, then it is given the defaults listed for the next two attributes.
* **digits:** the minimum and maximum number of decimal digits normally formatted.
The default is 2.
For example, in the en_US locale with the default value of 2 digits, the value 1 USD would format as "$1.00", and the value 1.123 USD would format as → "$1.12".
This value of this field is based on the "minor unit" value from ISO 4217, but may deviate from ISO 4217 where there is compelling evidence for different customary practice.
* **rounding:** the rounding increment, in units of 10<sup>-digits</sup>. The default is 0, which means no rounding is to be done. Therefore, rounding=0 and rounding=1 have identical behavior. Thus with fraction digits of 2 and rounding increment of 5, numeric values are rounded to the nearest 0.05 units in formatting. With fraction digits of 0 and rounding increment of 50, numeric values are rounded to the nearest 50.
* **cashDigits:** the number of decimal digits to be used when formatting quantities used in cash transactions (as opposed to a quantity that would appear in a more formal setting, such as on a bank statement). If absent, the value of "digits" should be used as a default.
* **cashRounding:** the cash rounding increment, in units of 10-cashDigits. The default is 0, which means no rounding is to be done; and as with rounding, this has the same effect as cashRounding="1". This is the rounding increment to be used when formatting quantities used in cash transactions (as opposed to a quantity that would appear in a more formal setting, such as on a bank statement). If absent, the value of "rounding" should be used as a default.

For example, the following line

```xml
<info iso4217="CZK" digits="2" rounding="0"/>
```

should cause the value 2.006 to be displayed as “2.01”, not “2.00”.

Each `region` element contains one attribute:

* **iso3166:** the ISO 3166 code for the region in question. The special value _XXX_ can be used to indicate that the region has no valid currency or that the circumstances are unknown (usually used in conjunction with _before_, as described below).

And can have any number of `currency` elements, with the `ordered` subelements.

```xml
<region iso3166="HR"> <!-- Croatia -->
    <currency iso4217="EUR" from="2023-01-01" tz="Europe/Zagreb"/>
    <currency iso4217="HRK" from="1994-05-30" to="2023-01-14" to-tz="Europe/Zagreb"/>
    <currency iso4217="HRD" from="1991-12-23" to="1995-01-01"/>
    <currency iso4217="YUN" from="1990-01-01" to="1991-12-23"/>
    <currency iso4217="YUD" from="1966-01-01" to="1990-01-01"/>
</region>
```

* **iso4217:** the ISO 4217 code for the currency in question. Note that some additional codes that were in widespread usage are included, others such as GHP are not included because they were never used.
* **from:** the currency was valid from the datetime indicated by the value. See the main document _[Dates and Date Ranges](tr35.md#Date_Ranges)_.
* **to:** the currency was valid up to the datetime indicated by the value. See the main document _[Dates and Date Ranges](tr35.md#Date_Ranges)_.
* **tz:** the timezone associated with the `from` transition datetime. If no `to-tz` attribute is specified, it also applies to the `to` transition datetime.
* *** Timezones for**: * Timezones for the `tz` and `to-tz` attribute are specified using the CLDR canonical “long” time zone ID as described under **Stability of Time Zone Identifiers** in [Time Zone Identifiers](tr35.md#Time_Zone_Identifiers).

* **to-tz:** the timezone associated with the `to` transition datetime. This timezone applies to the `to` value, and need only be specified if it is different from any timezone specified by a `tz` attribute.
* **tender:** indicates whether the ISO currency code represents a currency that was or is legal tender in some country. The default is "true". Certain ISO codes represent things like financial instruments or precious metals, and do not represent normally interchanged currencies.

> **Note on converting transition datetimes for implementation use:** The fact that CLDR stores the timezone separately from the transition datetime is for ease of CLDR maintenance. Implementations that use CLDR data may want to convert the combination into something like a single UTC timestamp for internal use.

* That is, each `currency` element will list an interval in which it was valid. The _ordering_ of the elements in the list tells us which was the primary currency during any period in time. Here is an example of such an overlap:


```xml
<currency iso4217="CSD" to="2002-05-15"/>
<currency iso4217="YUD" from="1994-01-24" to="2002-05-15"/>
<currency iso4217="YUN" from="1994-01-01" to="1994-07-22"/>
```

All `currency` elements with `tender="false"` should be at the end of the list for a given `region`.

The `from` element is limited by the fact that ISO 4217 does not go very far back in time, so there may be no ISO code for the previous currency.

Currencies change relatively frequently. There are different types of changes:

1. YU=>CS (name change)
2. CS=>RS+ME (split, different names)
3. SD=>SD+SS (split, same name for one // South Sudan splits from Sudan)
4. DE+DD=>DE (Union, reuses one name // East Germany unifies with Germany)

The [UN Information](https://unstats.un.org/unsd/methodology/m49/) is used to determine dates due to country changes.

When a code is no longer in use, it is terminated (see #1, #2, #4, #5)

> Example:
>
> * ```<currency iso4217="EUR" from="2003-02-04" to="2006-06-03"/>```text
When codes split, each of the new codes inherits (see #2, #3) the previous data. However, some modifications can be made if it is clear that currencies were only in use in one of the parts.

When codes merge, the data is copied from the most populous part.

> Example. When CS split into RS and ME:
>
> * RS & ME copy the former CS, except that the line for EUR is dropped from RS
> * CS now terminates on Jun 3, 2006 (following the UN info)

