## <a name="Layout_Elements" id="Layout_Elements" href="#Layout_Elements">Layout Elements</a>

```dtd
<!ELEMENT layout ( alias | (orientation*, inList*, inText*, special*) ) >
```

This top-level element specifies general layout features. It currently only has one possible element (other than `<special>`, which is always permitted).

```dtd
<!ELEMENT orientation ( characterOrder*, lineOrder*, special* ) >
<!ELEMENT characterOrder ( #PCDATA ) >
<!ELEMENT lineOrder ( #PCDATA ) >
```

The `lineOrder` and `characterOrder` elements specify the default general ordering of lines within a page, and characters within a line. The possible values are:

<!-- HTML: rowspan -->
<table>
<tbody>
<tr><th>Direction</th><th>Value</th></tr>
<tr><td rowspan="2">Vertical</td><td>top-to-bottom</td></tr>
<tr>                             <td>bottom-to-top</td></tr>
<tr><td rowspan="2">Horizontal</td><td>left-to-right</td></tr>
<tr>                               <td>right-to-left</td></tr>
</tbody>
</table>

* If the value of lineOrder is one of the vertical values, then the value of characterOrder must be one of the horizontal values, and vice versa. For example, for English the lines are top-to-bottom, and the characters are left-to-right. For Mongolian (in the Mongolian Script) the lines are right-to-left, and the characters are top to bottom. This does not override the ordering behavior of bidirectional text; it does, however, supply the paragraph direction for that text (for more information, see _UAX #9: The Bidirectional Algorithm_ [[UAX9](https://www.unicode.org/reports/tr41/#UAX9)]).


For dates, times, and other data to appear in the right order, the display for them should be set to the orientation of the locale.

* * *

```xml
<inList> (deprecated)
```

The `<inList>` element is deprecated and has been superseded by the `<contextTransforms>` element; see _[ContextTransform Elements](#Context_Transform_Elements)_.

* This element controls whether display names (language, territory, etc) are title cased in GUI menu lists and the like. It is only used in languages where the normal display is lower case, but title case is used in lists. There are two options:


```xml
<inList casing="titlecase-words">

<inList casing="titlecase-firstword">
```

* In both cases, the title case operation is the default title case function defined by Chapter 3 of _[[Unicode](tr35.md#Unicode)]_. In the second case, only the first word (using the word boundaries for that locale) will be title cased. The results can be fine-tuned by using alt="list" on any element where titlecasing as defined by the Unicode Standard will produce the wrong value. For example, suppose that "turc de Crimée" is a value, and the title case should be "Turc de Crimée". Then that can be expressed using the alt="list" value.


* * *

```xml
<inText> (deprecated)
```

The `<inList>` element is deprecated and has been superseded by the `<contextTransforms>` element; see _[ContextTransform Elements](#Context_Transform_Elements)_.

This element indicates the casing of the data in the category identified by the `inText` `type` attribute, when that data is written in text or how it would appear in a dictionary. For example:

```xml
<inText type="languages">lowercase-words</inText>
```

indicates that language names embedded in text are normally written in lower case. The possible values and their meanings are :

*   titlecase-words : all words in the phrase should be title case
*   titlecase-firstword : the first word should be title case
*   lowercase-words : all words in the phrase should be lower case
*   mixed : a mixture of upper and lower case is permitted, generally used when the correct value is unknown

