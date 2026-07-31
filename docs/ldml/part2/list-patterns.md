## <a name="ListPatterns" id="ListPatterns" href="#ListPatterns">List Patterns</a>

```dtd
<!ELEMENT listPatterns (alias | (listPattern*, special*)) >

<!ELEMENT listPattern (alias | (listPatternPart*, special*)) >
<!ATTLIST listPattern type (NMTOKEN) #IMPLIED >

<!ELEMENT listPatternPart ( #PCDATA ) >
<!ATTLIST listPatternPart type (start | middle | end | 2 | 3) #REQUIRED >
```

* List patterns can be used to format variable-length lists of things in a locale-sensitive manner, such as "Monday, Tuesday, Friday, and Saturday" (in English) versus "lundi, mardi, vendredi et samedi" (in French). For example, consider the following example:


```xml
<listPatterns>
 <listPattern>
  <listPatternPart type="2">{0} and {1}</listPatternPart>
  <listPatternPart type="start">{0}, {1}</listPatternPart>
  <listPatternPart type="middle">{0}, {1}</listPatternPart>
  <listPatternPart type="end">{0}, and {1}</listPatternPart>
 </listPattern>
</listPatterns>
```

Each pattern satisifies the following conditions:
```xml
<ul>
    <li>it contains the placeholders <code>{0}</code>, <code>{1}</code>, and <code>{2}</code> ("3"-pattern only) in order</li>
    <li>"start" and "middle" patterns end with the <code>{1}</code> placeholder</li>
    <li>"middle" and "end" patterns begin with the <code>{0}</code> placeholder</li>
</ul>
```

That is,
```xml
<ul>
    <li>all patterns can have text between the placeholders</li>
    <li>only the "start", "2", and "3" patterns can have text before the first placeholder, and</li>
    <li>only the "end", "2", and "3" patterns can have text after the last placeholder.</li>
</ul>
```

The data is used as follows: If there is a type that matches exactly the number of elements in the desired list (such as "2" in the above list), then use that pattern. Otherwise,

1.  Format the last two elements with the "end" pattern.
2.  Then use the "middle" pattern to add on subsequent elements working towards the front, all but the very first element. That is, `{1}` is what you've already done, and `{0}` is the previous element.
3.  Then use "start" to add the front element, again with `{1}` as what you've done so far, and `{0}` is the first element.

Thus a list (a,b,c,...m, n) is formatted as: `start(a,middle(b,middle(c,middle(...end(m, n))...)))`. Alternatively, the list can also be processed front-to-back:

1. Format the first two elements with the "start" pattern.
2. Then use the "middle" pattern to add on subsequent elements working towards the back, all but the very last element. That is, `{0}` is what you've already done, and `{1}` is the next element.
3. Then use "end" to add the last element, again with `{0}` as what you've done so far, and `{1}` is the last element.

Here, the list (a,b,c,...m, n) is formatted as:  `end(middle(..., middle(start(a, b), c) ...) m) n) `. While this prefix-expression looks less suitable, it actually only requires appends,
so this algorithm can be used to write into append-only sinks. Both the back-to-front and the front-to back algorithm produce this expression:

```text
start_before + a + start_between + b + middle_between + c + ... + middle_between + m + end_between + n + end_after
```

where the patters are "start": `start_before{0}start_between{1}`, "middle": `{0}middle_between{1}`, and "end": `{0}end_between{1}end_after`.

More sophisticated implementations can customize the process to improve the results for languages where context is important. For example:

<!-- HTML: rowspan, block elements in cells -->

<table>
<tbody>
<tr><td rowspan="3">Spanish</td><td>AND</td>
    <td>Use ‘e’ instead of ‘y’ in the listPatternPart for "end" and "2" in either of the following cases:
        <ol><li>The value substituted for {1} starts with ‘i’
                <ol><li><i>fuerte <b>e</b> indomable, </i>not <i>fuerte <b>y</b> indomable</i></li></ol>
            </li>
            <li>The value substituted for {1} starts with ‘hi’, but not with ‘hie’ or ‘hia’
                <ol><li><i>tos <b>e</b> hipo,</i> not <i>tos <b>y</b> hipo</i></li>
                    <li><i>agua <b>y</b> hielo,</i> not <i>agua <b>e</b> hielo</i></li></ol>
            </li></ol></td></tr>

<tr><td>OR</td>
    <td>Use ‘u’ instead of ‘o’ in the listPatternPart for "end" and "2" in any of the following cases:
        <ol><li>The value substituted for {1} starts with ‘o’ or ‘ho’
                <ol><li><i>delfines <b>u</b> orcas,</i> not <i>delfines <b>o</b> orcas</i></li>
                    <li><i>mañana <b>u</b> hoy,</i> not <i>mañana <b>o</b> hoy</i></li></ol>
            </li>
            <li>The value substituted for {1} starts with ‘8’
                <ol><li><i>6 <b>u</b> 8,</i> not <i>6 <b>o</b> 8</i></li></ol>
            </li>
            <li>The value substituted for {1} starts with ‘11’ where the numeric value is 11 x 10<sup>3×y</sup> (eg 11 thousand, 11.23 million, ...)
                <ol><li><i>10 <b>u</b> 11,</i> not <i>10 <b>o</b> 11</i></li>
                    <li><i>10 <b>u</b> 11.000,</i> not <i>10 <b>o</b> 11.000</i></li>
                    <li><i>10 <b>o</b> 111,</i> not <i>10 <b>u</b> 111</i></li></ol>
            </li></ol></td></tr>
<tr><td colspan="2">See <a href="http://web.archive.org/web/20240525091135/https://www.rae.es/espanol-al-dia/cambio-de-la-y-copulativa-en-e-0" title="Archived from https://www.rae.es/espanol-al-dia/cambio-de-la-y-copulativa-en-e-0">Cambio de la y copulativa en e</a><br><b>Note: </b>more advanced implementations may also consider the pronunciation, such as foreign words where the ‘h’ is not mute.</td></tr>

<tr><td rowspan="2">Hebrew</td><td>AND</td>
    <td>Use ‘-ו’ instead of ‘ו’ in the listPatternPart for "end" and "2" in the following case:
        <ol><li>if the value substituted for {1} starts with something other than a Hebrew letter, such as a digit (0-9) or a Latin-script letter
            <ol><li><i>one hour and two minutes =‎ ‏"שעה ושתי דקות"‏</i></li>
                <li><i>one hour and 9 minutes =‎ ‏"שעה ו-9 דקות"‏</i></li></ol>
            </li></ol></td></tr>
<tr><td colspan="2">See <a href="https://hebrew-academy.org.il/topic/hahlatot/punctuation/#target-3475">https://hebrew-academy.org.il/topic/hahlatot/punctuation/#target-3475</a></td></tr>xml
</tbody>
</table>

The following `type` attributes are in use:

| type attribute value      | Description                                                  | Examples                         |
| ------------------------- | ------------------------------------------------------------ | -------------------------------- |
| `standard` (or no `type`) | A typical 'and' list for arbitrary placeholders              | _January, February, and March_   |
| `standard-short`          | A short version of an 'and' list, suitable for use with short or abbreviated placeholder values | _Jan., Feb., and Mar._ |
| `standard-narrow`         | A yet shorter version of a short 'and' list (where possible) | _Jan., Feb., Mar._               |
| `or`                      | A typical 'or' list for arbitrary placeholders               | _January, February, or March_    |
| `or-short`                | A short version of an 'or' list                              | _Jan., Feb., or Mar._            |
| `or-narrow`               | A yet shorter version of a short 'or' list (where possible)  | _Jan., Feb., or Mar._            |
| `unit`                    | A list suitable for wide units                               | _3 feet, 7 inches_               |
| `unit-short`              | A list suitable for short units                              | _3 ft, 7 in_                     |
| `unit-narrow`             | A list suitable for narrow units, where space on the screen is very limited. | _3′ 7″_          |

In many languages there may not be a difference among many of these lists. In others, the spacing, the length or presence or a conjunction, and the separators may change.

Currently there are no locale keywords that affect list patterns; they are selected using the base locale ID, ignoring anu -u- extension keywords.

### <a name="List_Gender" id="List_Gender" href="#List_Gender">Gender of Lists</a>

```dtd
<!-- Gender List support -->
<!ELEMENT gender ( personList+ ) >
<!ELEMENT personList EMPTY >
<!ATTLIST personList type ( neutral | mixedNeutral | maleTaints ) #REQUIRED >
<!ATTLIST personList locales NMTOKENS #REQUIRED >
```

This can be used to determine the gender of a list of 2 or more persons, such as "Tom and Mary", for use with gender-selection messages. For example,

```xml
<supplementalData>
    <gender>
        <!-- neutral: gender(list) = other -->
        <personList type="neutral" locales="af da en..."/>

        <!-- mixedNeutral: gender(all male) = male, gender(all female) = female, otherwise gender(list) = other -->
        <personList type="mixedNeutral" locales="el"/>

        <!-- maleTaints: gender(all female) = female, otherwise gender(list) = male -->
        <personList type="maleTaints" locales="ar ca..."/>
    </gender>
</supplementalData>
```

There are three ways the gender of a list can be formatted:

1. **neutral:** A gender-independent "other" form will be used for the list.
2. **mixedNeutral:** If the elements of the list are all male, "male" form is used for the list. If all the elements of the lists are female, "female" form is used. If the list has a mix of male, female and neutral names, the "other" form is used.
3. **maleTaints:** If all the elements of the lists are female, "female" form is used, otherwise the "male" form is used.

