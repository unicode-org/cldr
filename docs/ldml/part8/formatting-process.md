## <a name="Formatting_Process" id="Formatting_Process" href="#Formatting_Process">Formatting Process</a>

* The patterns are in **personName** elements, which are themselves in a **personNames** container element. The following describes how the formatter's locale interacts with the personName's locale, how the name patterns are chosen, and how they are processed.


The details of the XML structure behind the data referenced here are in [XML Structure](#xml-structure).

The formatting process may be refined in the future. In particular, additional data may be added to allow further customization.

* **The term **maximal**: The term **maximal likely locale** used below is the result of using the [Likely Subtags](tr35.md#Likely_Subtags) data to map from a locale to a full representation that includes the base language, script, and region.


### <a name="Derive_the_name_locale" id="Derive_the_name_locale" href="#Derive_the_name_locale">Derive the name locale</a>

Construct the **name script** in the following way.
1. Iterate through the characters of the surname, then through the given name.
    1. Find the script of that character using the Script property.
    2. If the script is not Common, Inherited, nor Unknown, return that script as the **name script**
2. If nothing is found during the iteration, return Zzzz (Unknown Script)

Construct the **name base language** in the following way.
1. If the PersonName object can provide a name locale, return its language.
2. Otherwise, find the maximal likely locale for the name script and return its base language (first subtag).

Construct the **name locale** in the following way:
1. If the PersonName object can provide a name locale, return a locale formed from it by replacing its script by the name script.
2. Otherwise, return the locale formed from the name base language plus name script.

Construct the **name ordering locale** in the following way:
1. If the PersonName object can provide a name locale, return it.
2. Otherwise, return the maximal likely locale for “und-” + name script.

### <a name="Derive_the_formatting_locale" id="Derive_the_formatting_locale" href="#Derive_the_formatting_locale">Derive the formatting locale</a>

* **Let the **full**: Let the **full formatting locale** be the maximal likely locale for the formatter's locale. The **formatting base language** is the base language (first subtag) of the full formatting locale, and the **formatting script** is the script code of the full formatting locale.


#### <a name="Switch_the_formatting_locale_if_necessary" id="Switch_the_formatting_locale_if_necessary" href="#Switch_the_formatting_locale_if_necessary">Switch the formatting locale if necessary</a>

* A few script values represent a set of scripts, such as Jpan = {Hani, Kana, Hira}. Two script codes are said to _match_ when they are either identical, or one represents a set which contains the other, or they both represent sets which intersect. For example, Hani and Jpan match, because {Hani, Kana, Hira} contains Hani.


If the **name script** doesn't match the **formatting script**:
1. If the name locale has name formatting data, then set the formatting locale to the name locale.
2. Otherwise, set the formatting locale to the maximal likely locale for the the locale formed from und, plus the name script plus the region of the nameLocale.

For example, when a Hindi (Devanagari) formatter is called upon to format a name object that has the locale Ukrainian (Cyrillic):
* If the name is written with Cyrillic letters, under the covers a Ukrainian (Cyrillic) formatter should be instantiated and used to format that name. 
* If the name is written in Greek letters, then under the covers a Greek (Greek-script) formatter should be instantiated and used to format.

To determine whether there is name formatting data for a locale, get the values for each of the following paths.
If at least one of them doesn’t inherit their value from root, then the locale has name formatting data.
* //ldml/personNames/nameOrderLocales[@order="givenFirst"]
* //ldml/personNames/nameOrderLocales[@order="surnameFirst"]

### <a name="Derive_the_name_order" id="Derive_the_name_order" href="#Derive_the_name_order">Derive the name order</a>

A PersonName object’s fields are used to derive an order, as follows:

1. If the calling API requests sorting order, that is used.
2. Otherwise, if the PersonName object to be formatted has a `preferredOrder` field, then return that field’s value
3. Otherwise, use the nameOrderLocales elements to find the best match for the name locale, as follows.
    1. For each locale L1 in the parent locale lookup chain* for the **name ordering locale**, do the following
        1. Create a locale L2 by replacing the language subtag by 'und'. (Eg, 'de_DE' ⇒ 'und_DE')
        2. For each locale L in {L1, L2}, do the following
             1. If there is a precise match among the givenFirst nameOrderLocales for L, then let the nameOrder be givenFirst, and stop.
             2. Otherwise if there is a precise match among the surnameFirst nameOrderLocales for L, then let the nameOrder be surnameFirst, and stop.
    2. Otherwise, let the nameOrder be givenFirst, and stop.

\* For example, here is a parent locale lookup chain:

    de_Latn_DE ⇒ de_Latn ⇒ de_DE ⇒ de ⇒ und

In other words, with the name locale of `de_Latin_DE` you'll check the givenFirst and surnameFirst resources for the following locales, in this order:

    de_Latin_DE, und_Latn_DE, de_Latn, und_Latn, de_DE, und_DE, de, und

* This process will always terminate, because there is always a und value in one of the two nameOrderLocales elements. Remember that the lookup chain requires use of the parentLocales elements: it is not just truncation.


For example, the data for a particular locale might look like the following:

```xml
<nameOrderLocales order="surnameFirst">zh ja und-CN und-TW und-SG und-HK und-MO und-HU und-JP</nameOrderLocales>
```
* These nameOrderLocales will match any locale with a zh or ja [unicode_language_subtag](tr35.md#unicode_language_subtag) and any locale with a CN, TW, SG, HK MO, HU, or JP [unicode_region_subtag](tr35.md#unicode_region_subtag).


* Here are some more examples. Note that if there is no order field or locale field in the PersonName object to be formatted, and the script of the PersonName data is different from that of the formatting locale, then the default result is givenFirst.


| PersonName Object preferredOrder | PersonName Object Locale | Resulting Order |
| -------------------------------- | ------------------------ | --------------- |
| surnameFirst                     | ?                        | surnameFirst    |
|                                  | zh                       | surnameFirst    |
|                                  | und-JP                   | surnameFirst    |
|                                  | fr                       | givenFirst      |
|                                  |                          | givenFirst      |

### <a name="Choose_a_personName_element" id="Choose_a_personName_element" href="#Choose_a_personName_element">Choose a personName element</a>

* The personName data in CLDR provides representations for how names are to be formatted across the different axes of _order_, _length_, _usage_, and _formality_. More than one `namePattern` can be associated with a single `personName` entry. An algorithm is then used to choose the best `namePattern` to use.


As an example for English, this may look like:

```xml
<personNames>
  <personName order="givenFirst" length="long" usage="referring" formality="formal">
    <namePattern>{title} {given} {given2} {surname}, {credentials}</namePattern>
  </personName>
  <personName order="givenFirst" length="long" usage="referring" formality="informal">
    <namePattern>{given} «{given2}» {surname}</namePattern>
    <namePattern alt="2">«{given2}» {surname}</namePattern>
  </personName>
  <personName order="givenFirst" length="long" usage="sorting" formality="informal">
    <namePattern>{surname}, {given} {given2}</namePattern>
  </personName>
  ...
</personNames>
```

* The task is to find the best personName for a given set of input attributes. Well-formed data will always cover all possible combinations of the input parameters, so the algorithm is simple: traverse the list of person names until the first match is found, then return it.


In more detail:

A set of input parameters { order=O length=L usage=U formality=F } matches a personName element when:

* The order attribute values contain O or there is no order attribute, and
* The length attribute values contain L or there is no length attribute, and
* The usage attribute values contain U or there is no usage attribute, and
* The formality attribute values contain F or there is no formality attribute

Example for input parameters

> `order = `**`givenFirst`**`, length = `**`long`**`, usage = `**`referring`**`, formality = `**`formal`**

To match a personName, all four attributes in the personName must match (a missing attribute matches any value for that attribute):

| Sample personName attributes                                 | Matches? | Comment |
| :----------------------------------------------------------- | :------: | :------ |
| `order=`_`"givenFirst"`_` length=`_`"long"`_` usage=`_`"referring"`_` formality=`_`"formal"`_ | Y | exact match |
| `length=`_`"long"`_` usage=`_`"referring"`_` formality=`_`"informal"`_ | N | mismatch for formality |
| `length=`_`"long"`_` formality=`_`"formal"`_                  | Y | missing usage = all! |

To find the matching personName element, traverse all the personNames in order until the first one is found. This will always terminate since the data is well-formed in CLDR.

### <a name="Choose_a_namePattern" id="Choose_a_namePattern" href="#Choose_a_namePattern">Choose a namePattern</a>

* To format a name, the fields in a namePattern are replaced with fields fetched from the PersonName Data Interface. The personName element can contain multiple namePattern elements. Choose one based on the fields in the input PersonName object that are populated:

1. Find the set of patterns with the most populated fields.
2. If there is just one element in that set, use it.
2. Otherwise, among that set, find the set of patterns with the fewest unpopulated fields.
3. If there is just one element in that set, use it.
4. Otherwise, take the pattern that is alphabetically least. (This step should rarely happen, and is only for producing a determinant result.)

For example:

1. Pattern A has 12 fields total, pattern B has 10 fields total, and pattern C has 8 fields total.
2. Both patterns A and B can be populated with 7 fields from the input PersonName object, pattern C can be populated with only 3 fields from the input PersonName object.
3. Pattern C is discarded, because it has the least number of populated name fields.
4. Out of the remaining patterns A and B, pattern B wins, because it has only 3 unpopulated fields compared to pattern A.

### <a name="Access_PersonName_object" id="Access_PersonName_object" href="#Access_PersonName_object">Access PersonName object</a>

#### <a name="Handle_missing_surname" id="Handle_missing_surname" href="#Handle_missing_surname">Handle missing surname</a>

* All PersonName objects will have a given name (for mononyms the given name is used). However, there may not be a surname. In that case, the following process is followed so that formatted patterns produce reasonable results.


1. If there is no surname from a PersonName P1 _and_ the pattern either doesn't include the given name or only shows an initial for the given name, then:
    1. Construct and use a derived PersonName P2, whereby P2 behaves exactly as P1 except that:
        1. Any request for a surname field (with any modifiers) returns P1's given name (with the same modifiers)
        2. Any request for a given name field (with any modifiers) returns "" (empty string)

* As always, this is a logical description and may be optimized in implementations. For example, an implemenation may use an interface for P2 that just delegates calls to P1, with some redirection for accesses to surname and given name.


#### <a name="Handle_core_and_prefix" id="Handle_core_and_prefix" href="#Handle_core_and_prefix">Handle core and prefix</a>

* A given field may have a core value, a prefix value, and/or a ‘plain’ value (neither core nor prefix). If one or more of them are missing, then the returned values should be adjusted according to the table below. In the three cells on the left, a ✓ indicates that a value is available, an ✖️ if there is none. In three cells on the right, the value of = means the returned value is unchanged, ✖️ means the returned value is “empty”, and anything else is a description of what to change it to.


| prefix | core | plain | | prefix | core  | plain |
| ------ | ---- | ----- |-| ------ | ----  | -----    |
| ✓      | ✓    | ✓     | | =      | =     | =        |
| ✓      | ✖️   | ✓     | | ✖️     | plain | =        |
| ✖️     | ✓    | ✓     | | =      | plain | =        |
| ✖️     | ✖️   | ✓     | | =      | plain | =        |
| ✓      | ✓    | ✖️    | | =      | =     | prefix + " " + core |
| ✖️     | ✓    | ✖️    | | =     | =         | core |
| ✓      | ✖️   | ✖️    | | ✖️    | =         | =        |
| ✖️     | ✖️   | ✖️    | | =     | =         | =        |

* For example, if the surname-prefix is "von und zu" and the surname-core is "Stettbach" and there is no surname (plain), then the derived value for the (plain) surname is "von und zu Stettbach". (The cases where existing prefix values are changed should not be necessary with well-formed PersonName data.)


#### <a name="Derive_initials" id="Derive_initials" href="#Derive_initials">Derive initials</a>

The following process is used to produce initials when they are not supplied by the PersonName object. Assuming the input example is “Mary Beth”:

| Action              | Result |
| ------------------- | ------ |
| 1. Split into words | “Mary” and “Beth” |
| 2. Fetch the first grapheme cluster of each word | “M” and “B” |
| 3. The ***initial*** pattern is applied to each<br/>`  `<initialPattern type="initial">`{0}.`</initialPattern>`` | “M.” and “B.” |
| 4. Finally recombined with ***initialSequence***<br/>`  `<initialPattern type="initialSequence">`{0} {1}`</initialPattern>`` | “M. B.” |

See the “initial” modifier in the [Modifiers](#modifiers) section for more details.

### <a name="Process_a_namePattern" id="Process_a_namePattern" href="#Process_a_namePattern">Process a namePattern</a>

The “winning” namePattern may still have fields that are unpopulated (empty) in the PersonName object. That namePattern is populated with field values with the following steps:

1. If one or more fields at the start of the pattern are empty, all fields and literal text before the **first** populated field are omitted.
2. If one or more fields at the end of the pattern are empty, all fields and literal text after the **last** populated field are omitted.
3. Processing from the start of the remaining pattern:
    1. If there are two or more empty fields separated only by literals, the fields and the literals between them are removed.
    2. If there is a single empty field, it is removed.
4. If the processing from step 3 results in two adjacent literals (call them A and B), they are coalesced into one literal as follows:
    1. If either is empty the result is the other one.
    2. If B matches the end of A, then the result is A. So xyz + yz ⇒ xyz, and xyz + xyz ⇒ xyz.
    3. Otherwise the result is A + B, further modified by replacing any sequence of two or more white space characters by the first whitespace character.
5. All of the fields are replaced by the corresponding values from the PersonName object.

The result is the **formatted value**. However, there is one further step that might further modify that value.

#### <a name="Handling_foreign_names" id="Handling_foreign_names" href="#Handling_foreign_names">Handling foreign names</a>

* There are two main challenges in dealing with foreign name formatting that needs to be considered. One is the ordering, which is dealt with under the section [nameOrderLocales Element](#nameorderlocales-element)]. The other is spacing.


* Some writing systems require spaces (or some other non-letters) to separate words. For example, [Hayao Miyazaki](https://en.wikipedia.org/wiki/Hayao_Miyazaki) is written in English with given name first and with a space between the two name fields, while in Japanese there is no space with surname first: [宮崎駿](https://ja.wikipedia.org/wiki/%E5%AE%AE%E5%B4%8E%E9%A7%BF)


* If a locale requires spaces between words, the normal patterns for the formatting locale are used. On Wikipedia, for example, note the space within the Japanese name on pages from English and Korean (an ideographic space is used here for emphasis).


* “​​[Hayao Miyazaki (宮崎<span style="background-color:aqua">　</span>駿, Miyazaki Hayao](https://en.wikipedia.org/wiki/Hayao_Miyazaki)…” or
* “[미야자키<span style="background-color:aqua">　</span>하야오(일본어: 宮﨑<span style="background-color:aqua">　</span>駿 Miyazaki Hayao](https://ko.wikipedia.org/wiki/%EB%AF%B8%EC%95%BC%EC%9E%90%ED%82%A4_%ED%95%98%EC%95%BC%EC%98%A4)…”.

* If a locale **doesn’t** require spaces between words, there are two cases, based on whether the name is foreign or not (based on the PersonName objects explicit or calculated locale's language subtag). For example, the formatting locale might be Japanese, and the locale of the PersonName object might be de_CH, German (Switzerland), such as Albert Einstein. When the locale is foreign, the **foreignSpaceReplacement** is substituted for each space in the formatted name. When the name locale is native, a **nativeSpaceReplacement** is substituted for each space in the formatted name. The precise algorithm is given below.


Here are examples for Albert Einstein in Japanese and Chinese:
* [アルベルト<span style="background-color:aqua">・</span>アインシュタイン](https://ja.wikipedia.org/wiki/%E3%82%A2%E3%83%AB%E3%83%99%E3%83%AB%E3%83%88%E3%83%BB%E3%82%A2%E3%82%A4%E3%83%B3%E3%82%B7%E3%83%A5%E3%82%BF%E3%82%A4%E3%83%B3)
* [阿尔伯特<span style="background-color:aqua">·</span>爱因斯坦](https://zh.wikipedia.org/wiki/%E9%98%BF%E5%B0%94%E4%BC%AF%E7%89%B9%C2%B7%E7%88%B1%E5%9B%A0%E6%96%AF%E5%9D%A6)

#### <a name="Setting_the_spaceReplacement" id="Setting_the_spaceReplacement" href="#Setting_the_spaceReplacement">Setting the spaceReplacement</a>

1. The foreignSpaceReplacement is provided by the value for the `foreignSpaceReplacement` element; the default value is a SPACE (" ").
2. The nativeSpaceReplacement is provided by the value for the `nativeSpaceReplacement` element; the default value is SPACE (" ").
3. If the formatter base language matches the name base language, then let spaceReplacement = nativeSpaceReplacement, otherwise let spaceReplacement = foreignSpaceReplacement.
4. Replace all sequences of space in the formatted value string by the spaceReplacement.

For the purposes of this algorithm, two base languages are said to __match__ when they are identical, or if both are in {ja, zh, yue}.

**Note:** in the future the plan is to make the specific languages and scripts used in this algorithm be data-driven.

Remember that **a name in a different script** will use a different locale for formatting, as per [Switch the formatting locale if necessary](#switch-the-formatting-locale-if-necessary).
* For example, when formatting a name for Japanese, if the name is in the Latin script, a Latin based locale will be used to format it, such as when “Albert Einstein” appears in Latin characters as in the Wikipedia page [Albert Einstein](https://ja.wikipedia.org/wiki/Albert_Einstein).


#### <a name="Examples_of_space_replacement" id="Examples_of_space_replacement" href="#Examples_of_space_replacement">Examples of space replacement</a>

* To illustrate how foreign space replacement works, consider the following name data. For illustration, the name locale is given in the maximized form: in practice, `ja` would be used instead of `ja_Jpan_JP`, and so on.: For more information, see [Likely Subtags](tr35.md#Likely_Subtags).


| name locale   | given    | surname       |
| ------------- | -------- | ------------- |
| `de_Latn_CH`  | Albert   | Einstein      |
| `de_Kata_CH`  | アルベルト | アインシュタイン |
| `ja_Kata_CH`  | アルベルト | アインシュタイン |
| `ja_Latn_JP`  | Hayao    | Miyazaki      |
| `ja_Jpan_JP`  | 駿       | 宮崎           |

Suppose the PersonNames formatting patterns for `ja_JP` and `de_CH` contained the following:

**`ja_JP` formatting patterns**

<pre>
&lt;personNames&gt;
   &lt;nameOrderLocales order="givenFirst"&gt;und&lt;/nameOrderLocales&gt;
   &lt;<strong>nameOrderLocales</strong> order="<strong>surnameFirst</strong>"&gt;hu <strong>ja</strong> ko vi yue zh <strong>und_JP</strong>&lt;/nameOrderLocales&gt;
   &lt;<strong>nativeSpaceReplacement</strong> xml:space="preserve"&gt;<span style="background-color:aqua"></span>&lt;/nativeSpaceReplacement&gt;
   &lt;<strong>foreignSpaceReplacement</strong> xml:space="preserve"&gt;<span style="background-color:aqua">・</span>&lt;/foreignSpaceReplacement&gt;
   . . .
   &lt;personName order="<strong>givenFirst</strong>" length="medium" usage="referring" formality="formal"&gt;
      &lt;namePattern&gt;{given}<span style="background-color:aqua"> </span>{given2}<span style="background-color:aqua"> </span>{surname}{generation}&lt;/namePattern&gt;
   &lt;/personName&gt;
   . . .
   &lt;personName order="<strong>surnameFirst</strong>" length="medium" usage="referring" formality="formal"&gt;
      &lt;namePattern&gt;{surname}{given2}{given}{generation}&lt;/namePattern&gt;
   &lt;/personName&gt;
   . . .
&lt;/personNames&gt;
```xml
</pre>
```

* Note in the `de_CH` locale, _ja_ is not listed in nameOrderLocales, and would therefore fall under _und_, and be formatted using the givenFirst order patterns if the name data is in the same script as the formatting locale.


**`de_CH` formatting patterns**

<pre>
&lt;personNames&gt;
   &lt;nameOrderLocales order="<strong>givenFirst</strong>"&gt;und <strong>de</strong>&lt;/nameOrderLocales&gt;
   &lt;nameOrderLocales order="surnameFirst"&gt;ko vi yue zh&lt;/nameOrderLocales&gt;
   &lt;foreignSpaceReplacemen xml:space="preserve"&gt;<span style="background-color:aqua"> </span>&lt;/foreignSpaceReplacement&gt;
   . . .
   &lt;personName order="givenFirst" length="medium" usage="referring" formality="formal"&gt;
      &lt;namePattern&gt;{given}<span style="background-color:aqua"> </span>{given2-initial}<span style="background-color:aqua"> </span>{surname}, {generation}&lt;/namePattern&gt;
   &lt;/personName&gt;
   . . .
   &lt;personName order="surnameFirst" length="medium" usage="referring" formality="formal"&gt;
* **&lt;namePattern&gt;{surname}, {given}<span**: &lt;namePattern&gt;{surname}<span style="background-color:aqua">, </span>{given}<span style="background-color:aqua"> </span>{given2-initial}<span style="background-color:aqua">,</span> {generation}&lt;/namePattern&gt;

   &lt;/personName&gt;
   . . .
&lt;/personNames&gt;`
```xml
</pre>
```

The name data would resolve as follows:
<!-- TODO Replace the following with a markdown table -->

<table>
  <tr>
   <td colspan="7" ><strong>formatting locale: ja_JP, </strong>script is Jpan which includes Hani, Hira and Kana</td>
  </tr>
  <tr>
   <td><strong>name locale</strong></td>
   <td><strong>given</strong></td>
   <td><strong>surname</strong></td>
   <td><strong>same<br/>script</strong></td>
   <td><strong>formatting<br/>locale</strong></td>
   <td><strong>order</strong></td>
   <td><strong>foreign<br/>space</strong></td>
  </tr>
  <tr>
   <td>de_Latn_CH</td>
   <td>Albert</td>
   <td><span style="text-decoration:underline;">Einstein</span></td>
   <td>NO</td>
   <td>de</td>
   <td>given First</td>
   <td></td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“Albert <span style="text-decoration:underline;">Einstein</span>”</td>
  </tr>
  <tr>
   <td>de_Jpan_CH</td>
   <td>アルベルト</td>
   <td><span style="text-decoration:underline;">アインシュタイン</span></td>
   <td>YES</td>
   <td>und</td>
   <td>given First</td>
   <td>“<span style="background-color:aqua">・</span>”</td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“アルベルト<span style="background-color:aqua">・</span><span style="text-decoration:underline;">アインシュタイン</span>”</td>
  </tr>
  <tr>
   <td>ja_Jpan_JP</td>
   <td>駿</td>
   <td><span style="text-decoration:underline;">宮崎</span></td>
   <td>YES</td>
   <td>ja</td>
   <td>surname First</td>
   <td></td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center"><span style="text-decoration:underline;">宮崎</span>駿</td>
  </tr>
</table>

<table>
  <tr>
   <td colspan="7" ><strong>formatting locale: de_CH</strong>, formatting locale script is Latn</td>
  </tr>
  <tr>
   <td><strong>name locale</strong></td>
   <td><strong>given</strong></td>
   <td><strong>surname</strong></td>
   <td><strong>same<br/>script</strong></td>
   <td><strong>formatting<br/>locale</strong></td>
   <td><strong>order</strong></td>
   <td><strong>foreign<br/>space</strong></td>
  </tr>
  <tr>
   <td>de_Latn_CH</td>
   <td>Albert</td>
   <td>Einstein</td>
   <td>YES</td>
   <td>de</td>
   <td>given First</td>
   <td></td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“Albert Einstein”</td>
  </tr>
  <tr>
   <td>de_Jpan_CH</td>
   <td>アルベルト</td>
   <td>アインシュタイン</td>
   <td>NO</td>
   <td>ja<br/>from script</td>
   <td>given First</td>
   <td>“<span style="background-color:aqua">・</span>”</td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“アルベルト<span style="background-color:aqua">・</span>アインシュタイン”</td>
  </tr>
  <tr>
   <td>und_Latn_JP</td>
   <td>Hayao</td>
   <td>Miyazaki</td>
   <td>YES</td>
   <td>und</td>
   <td>given First</td>
   <td>“<span style="background-color:aqua"> </span>”</td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“Hayao<span style="background-color:aqua"> </span>Miyazaki”</td>
  </tr>
</table>

### <a name="Formatting_examples" id="Formatting_examples" href="#Formatting_examples">Formatting examples</a>

The personName element contains:


> `<namePattern>{title} {given} {given2} {surname}, {credentials}</namePattern>`


The input PersonName object contains:

| `title` | `given` | `given2` | `surname` | `generation` |
| -------- | ------- | -------- | --------- | --------      |
|          | Raymond | J.       | Johnson   | Jr.           |

The output is:

> Raymond J. Johnson, Jr.

The “title” field is empty, and so both it and the space that follows it are omitted from the output, according to rule 1 above.

If, instead, the input PersonName object contains:

| `title` | `given` | `given2` | `surname` | `generation` |
| -------- | ------- | -------- | --------- | -------- |
|          | Raymond | J.       | Johnson   |          |

The output is:

> Raymond J. Johnson

The “title” field is empty, and so both it and the space that follows it are omitted from the output, according to rule 1 above.

The “generation” field is also empty, so it and both the comma and the space that precede it are omitted from the output, according to rule 2 above.

* To see how rule 3 interacts with the other rules, consider an imaginary language in which people generally have given and given2 (or middle)  names, and the given2 name is always written with parentheses around it, and the given name is usually written as an initial with a following period.


The personName element contains:

> `<namePattern>{given-initial}. ({given2}) {surname}</namePattern>`


The input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     | Bar      | Baz       |

The output is:

> F. (Bar) Baz

If, instead, the input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     |          | Baz       |

The output is:

> F. Baz

* The “given2” field is empty, so it and the surrounding parentheses are omitted from the output, as is one of the surrounding spaces, according to rule 3. The period after “{given-initial}” remains, because it is separated from the “{given2}” element by  space-- punctuation around a missing field is only deleted up until the closest space in each direction.


If there were no space between the period and the parentheses, as might happen if our hypothetical language didn’t use spaces:

> `<namePattern>{given-initial}.({given2}) {surname}</namePattern>`

The input PersonName object still contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     |          | Baz       |

The output is:

> F Baz

* Both the period after “{given-initial}” _and_ the parentheses around “{given2}” are omitted from the output, because there was no space between them — instead, we delete punctuation all the way up to the neighboring field. To solve this (making sure the “{given-initial}” field always has a period after it), you would add another namePattern:


> `<namePattern>{given-initial}.({given2}) {surname}</namePattern>`<br/>
> `<namePattern alt=”2”>{given-initial}. {surname}</namePattern>`

The first pattern would be used when the “given2” field is populated, and the second pattern would be used when the “given2” field is empty.

Rules 1 and 3 can conflict in similar ways. If the personName element contains (there’s a space between the period and the opening parenthesis again):

> `<namePattern>{given-initial}. ({given2}) {surname}</namePattern>`

And the input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
|         | Bar      | Baz       |

The output is:

> Bar) Baz

* Because the “given” field is empty, rule 1 not only has us delete it, but also all punctuation up to “{given2}”. This includes _both_ the period _and_ the opening parenthesis. Again, to solve this, you’d supply two namePatterns:


> `<namePattern>{given-initial}. ({given2}) {surname}</namePattern>`<br/>
> `<namePattern alt=”2”> ({given2}) {surname}</namePattern>`

The output would then be:

> (Bar) Baz

The first namePattern would be used if the “given” field was populated, and the second would be used if it was empty.

If, instead, the input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     |          | Baz       |

The output is:

> F. Baz

