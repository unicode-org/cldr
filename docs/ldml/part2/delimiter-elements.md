## <a name="Delimiter_Elements" id="Delimiter_Elements" href="#Delimiter_Elements">Delimiter Elements</a>

```dtd
<!ELEMENT delimiters (alias | (quotationStart*, quotationEnd*, alternateQuotationStart*, alternateQuotationEnd*, special*)) >
```

The delimiters supply common delimiters for bracketing quotations. The quotation marks are used with simple quoted text, such as:

> He said, “Don’t be absurd!”

When quotations are nested, the quotation marks and alternate marks are used in an alternating fashion:

> He said, “Remember what the Mad Hatter said: ‘Not the same thing a bit! Why you might just as well say that “I see what I eat” is the same thing as “I eat what I see”!’”

```xml
<quotationStart>“</quotationStart>
<quotationEnd>”</quotationEnd>
<alternateQuotationStart>‘</alternateQuotationStart>
<alternateQuotationEnd>’</alternateQuotationEnd>
```

### <a name="Tailor_Linebreak_With_Delimiters" id="Tailor_Linebreak_With_Delimiters" href="#Tailor_Linebreak_With_Delimiters">Tailoring Linebreak Using Delimiters</a>

The delimiter data can be used for language-specific tailoring of linebreak behavior, as suggested
in the [description of linebreak class QU: Quotation](https://www.unicode.org/reports/tr14/#QU)
in [[UAX14](https://www.unicode.org/reports/tr41/#UAX14)]. This is an example of
[tailoring type](https://www.unicode.org/reports/tr14/#Tailoring) 1 (from that same document),
changing the line breaking class assignment for some characters.

If the values of `<quotationStart>` and `<quotationEnd>` are different, then:
* if the value of `<quotationStart>` is a single character with linebreak class QU: Quotation, change its class to OP: Open Punctuation.
* if the value of `<quotationEnd>` is a single character with linebreak class QU: Quotation, change its class to CL: Close Punctuation.
Similarly for `<alternateQuotationStart>` and `<alternateQuotationEnd>`.

Some characters with multiple uses should generally be excluded from this linebreak class remapping, such as:
* U+2019 RIGHT SINGLE QUOTATION MARK, often used as apostrophe, should not be changed from QU; otherwise it will introduce breaks after apostrophe.
* Several locales (mostly for central and eastern Europe) have U+201C LEFT DOUBLE QUOTATION MARK as `<quotationEnd>` or `<alternateQuotationEnd>`. However users in these locales may also encounter English text in which U+201C is used as `<quotationStart>`. In order to prevent improper breaks for English text, in these locales U+201C should not be changed from QU.

