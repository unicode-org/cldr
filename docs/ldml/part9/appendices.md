## <a name="Appendices" id="Appendices" href="#Appendices">Appendices</a>

### <a name="Security_Considerations" id="Security_Considerations" href="#Security_Considerations">Security Considerations</a>

Unicode MessageFormat _patterns_ are meant to allow a _message_ to include any string value
which users might normally wish to use in their environment.
Programming languages and other environments vary in what characters are permitted
to appear in a valid string.
In many cases, certain types of characters, such as invisible control characters,
require escaping by these host formats.
In other cases, strings are not permitted to contain certain characters at all.
Since _messages_ are subject to the restrictions and limitations of their
host environments, their serializations and resource formats,
that might be sufficient to prevent most problems.
However, MessageFormat itself does not supply such a restriction.

MessageFormat _messages_ permit nearly all Unicode code points
to appear in _literals_, including the text portions of a _pattern_.
This means that it can be possible for a _message_ to contain invisible characters
(such as bidirectional controls, ASCII control characters in the range U+0000 to U+001F,
or characters that might be interpreted as escapes or syntax in the host format)
that abnormally affect the display of the _message_
when viewed as source code, or in resource formats or translation tools,
but do not generate errors from MessageFormat parsers or processing APIs.

Bidirectional text containing right-to-left characters (such as used for Arabic or Hebrew)
also poses a potential source of confusion for users.
Since MessageFormat's syntax makes use of
keywords and symbols that are left-to-right or consist of neutral characters
(including characters subject to mirroring under the Unicode Bidirectional Algorithm),
it is possible to create messages that,
when displayed in source code, or in resource formats or translation tools,
have a misleading appearance or are difficult to parse visually.

For more information, see \[[UTS#55](https://unicode.org/reports/tr55/)\]
`<cite>`Unicode Source Code Handling`</cite>`.

MessageFormat implementations might allow end-users to install
_selectors_, _functions_, or _markup_ from third-party sources.
Such functionality can be a vector for various exploits,
including buffer overflow, code injection, user tracking,
fingerprinting, and other types of bad behavior.
Any installed code needs to be appropriately sandboxed.
In addition, end-users need to be aware of the risks involved.

### <a name="Nonnormative_Examples" id="Nonnormative_Examples" href="#Nonnormative_Examples">Non-normative Examples</a>

#### <a name="Pattern_Selection_Examples" id="Pattern_Selection_Examples" href="#Pattern_Selection_Examples">Pattern Selection Examples</a>

#### <a name="Selection_Example_1" id="Selection_Example_1" href="#Selection_Example_1">Selection Example 1</a>

Presuming a minimal implementation which only supports `:string` _function_
which matches keys by using string comparison,
and a formatting context in which
the variable reference `$foo` resolves to the string `'foo'` and
the variable reference `$bar` resolves to the string `'bar'`,
pattern selection proceeds as follows for this message:

```text
.input {$foo :string}
.input {$bar :string}
.match $foo $bar
bar bar {{All bar}}
foo foo {{All foo}}
* * {{Otherwise}}
```

1. Each selector is resolved, yielding the list `res` = `{foo, bar}`.
2. `bestVariant` is set to `UNSET`.
3. `keys` is set to `{bar, bar}`.
4. `match` is set to SelectorsMatch(`{foo, bar}`, `{bar, bar}`).
   The result of SelectorsMatch(`{foo, bar}`, `{bar, bar}`) is
   determined as follows:
   1. `result` is set to true.
   1. `i` is set to 0.
   1. `k` is set to the string `bar`.
   1. `sel` is set to a resolved value corresponding to the string `foo`.
   1. Match(`sel`, `'bar'`) is false.
   1. The result of SelectorsMatch(`{foo, bar}`, `{bar, bar}`) is false.
   Thus, `match` is set to false.
5. `keys` is set to `{foo, foo}`.
6. `match` is set to SelectorsMatch(`{foo, bar}`, `{foo, foo}`).
   The result of SelectorsMatch(`{foo, bar}`, `{foo, foo}`) is
   determined as follows:
   1. `result` is set to true.
   1. `i` is set to 0.
   1. `k` is set to the string `foo`.
   1. `sel` is set to a resolved value corresponding to the string `foo`.
   1. Match(`sel`, `'foo'`) is true.
   1. `i` is set to 1.
   1. `k` is set to the string `foo`.
   1. `sel` is set to a resolved value corresponding to the string `bar`.
   1. Match(`sel`, `'bar'`) is false.
   1. The result of SelectorsMatch(`{foo, bar}`, `{foo, foo}`) is false.
7. `keys` is set to `* *`.
8. The result of SelectorsMatch(`{foo, bar}`, `{*, *}`) is
   determined as follows:
   1. `result` is set to true.
   1. `i` is set to 0.
   1. `i` is set to 1.
   1. `i` is set to 2.
   1. The result of SelectorsMatch(`{foo, bar}`, `{*, *}`) is true.
9. `bestVariant` is set to the variant `* * {{Otherwise}}`
10. The pattern `Otherwise` is selected.

#### <a name="Selection_Example_2" id="Selection_Example_2" href="#Selection_Example_2">Selection Example 2</a>

Alternatively, with the same implementation and formatting context as in Example 1,
pattern selection would proceed as follows for this message:

```text
.input {$foo :string}
.input {$bar :string}
.match $foo $bar
* bar {{Any and bar}}
foo * {{Foo and any}}
foo bar {{Foo and bar}}
* * {{Otherwise}}
```

1. Each selector is resolved, yielding the list `res` = `{foo, bar}`.
2. `bestVariant` is set to `UNSET`.
3. `keys` is set to `{*, bar}`.
4. `match` is set to SelectorsMatch(`{foo, bar}`, `{*, bar}`)
   The result of SelectorsMatch(`{foo, bar}`, `{*, bar}`) is
   determined as follows:
   1. `result` is set to true.
   2. `i` is set to 0.
   3. `i` is set to 1.
   4. `k` is set to the string `bar`.
   5. `sel` is set to a resolved value corresponding to the string `bar`.
   6. Match(`sel`, `'bar'`) is true.
   7. `i` is set to 2.
   1. The result of SelectorsMatch(`{foo, bar}`, `{*, bar}`) is true.
5. `bestVariant` is set to the variant `* bar {{Any and bar}}`.
6. `keys` is set to `{foo, *}`.
7. `match` is set to SelectorsMatch(`{foo, bar}`, `{foo, *}`).
   The result of SelectorsMatch(`{foo, bar}`, `{foo, *}`) is
   determined as follows:
   1. `result` is set to true.
   2. `i` is set to 0.
   3. `k` is set to the string `foo`.
   4. `sel` is set to a resolved value corresponding to the string `foo`.
   5. Match(`sel`, `'foo'`) is true.
   6. `i` is set to 1.
   7. `i` is set to 2.
   8. The result of SelectorsMatch(`{foo, bar}`, `{foo, *}`) is true.
8. `bestVariantKeys` is set to `{*, bar}`.
9. SelectorsCompare(`{foo, bar}`, `{foo, *}`, `{*, bar}`) is
   determined as follows:
   1. `result` is set to false.
   1. `i` is set to 0.
   1. `key1` is set to `foo`.
   1. `key2` is set to `'*'`
   1. The result of SelectorsCompare(`{foo, bar}`, `{foo, *}`, `{*, bar}`) is true.
10. `bestVariant` is set to `foo * {{Foo and any}}`.
11. `keys` is set to `{foo, bar}`.
12. `match` is set to SelectorsMatch(`{foo, bar}`, `{foo, bar}`).
    1. `match` is true (details elided)
13. `bestVariantKeys` is set to `{foo, *}`.
14. SelectorsCompare(`{foo, bar}`, `{foo, bar}`, `{foo, *}`) is
    determined as follows:
    1. `result` is set to false.
    1. `i` is set to 0.
    1. `key1` is set to `foo`.
    1. `key2` is set to `foo`.
    1. `k1` is set to `foo`.
    1. `k2` is set to `foo`.
    1. `sel` is set to a resolved value corresponding to `foo`.
    1. `i` is set to 1.
    1. `key1` is set to `bar`.
    1. `key2` is set to `*`.
    1. The result of SelectorsCompare(`{foo, bar}`, `{foo, bar}`, `{foo, *}`)
       is true.
15. `bestVariant` is set to `foo bar {{Foo and bar}}`.
16. `keys` is set to `* *`.
17. `match` is set to true (details elided).
18. `bestVariantKeys` is set to `foo bar`.
19. SelectorsCompare(`{foo, bar}`, `{*, *}`, `{foo, bar}`} is false
    (details elided).

The pattern `{{Foo and bar}}` is selected.

#### <a name="Selection_Example_3" id="Selection_Example_3" href="#Selection_Example_3">Selection Example 3</a>

A more-complex example is the matching found in selection APIs
such as ICU's `PluralFormat`.
Suppose that this API is represented here by the function `:number`.
This `:number` function can match a given numeric value to a specific number _literal_
and **_also_** to a plural category (`zero`, `one`, `two`, `few`, `many`, `other`)
according to locale rules defined in CLDR.

Given a variable reference `$count` whose value resolves to the number `1`
and an `en` (English) locale,
the pattern selection proceeds as follows for this message:

```text
.input {$count :number}
.match $count
one {{Category match for {$count}}}
1   {{Exact match for {$count}}}
*   {{Other match for {$count}}}
```

1. Each selector is resolved, yielding the list `{1}`.
1. `bestVariant` is set to `UNSET`.
1. `keys` is set to `{one}`.
1. `match` is set to SelectorsMatch(`{1}`, `{one}`).
   The result of SelectorsMatch(`{1}`, `{one}`) is
   determined as follows:
   1. `result` is set to true.
   1. `i` is set to 0.
   1. `k` is set to `one`.
   1. `sel` is set to `1`.
   1. Match(`sel`, `one`) is true.
   1. `i` is set to 1.
   1. The result of SelectorsMatch(`{1}`, `{one}`) is true.
1. `bestVariant` is set to `one {{Category match for {$count}}}`.
1. `keys` is set to `1`.
1. `match` is set to SelectorsMatch(`{1}`, `{one}`).
   1. The details are the same as the previous case,
      as Match(`sel`, `1`) is also true.
1. `bestVariantKeys` is set to `{one}`.
1. SelectorsCompare(`{1}`, `{1}`, `{one}`) is determined as follows:
   1. `result` is set to false.
   1. `i` is set to 0.
   1. `key1` is set to `1`.
   1. `key2` is set to `one`.
   1. `k1` is set to `1`.
   1. `k2` is set to `one`.
   1. `sel` is set to `1`.
   1. `result` is set to BetterThan(`sel`, `1`, `one`), which is true.
      1. NOTE: The specification of the `:number` selector function
         states that the exact match `1` is a better match than
         the category match `one`.
   1. `bestVariant` is set to `1 {{Exact match for {$count}}}`.
1. `keys` is set to `*`
   1. Details elided; since `*` is the catch-all key,
      BetterThan(`{1}`, `{1}`, `{*}`) is false.
1. The pattern `{{Exact match for {$count}}}` is selected.

### <a name="Acknowledgments" id="Acknowledgments" href="#Acknowledgments">Acknowledgments</a>

Special thanks to the following people for their contributions to making the Unicode MessageFormat Standard.
The following people contributed to our github repo and are listed in order by contribution size:

Addison Phillips,
Eemeli Aro,
Romulo Cintra,
Tim Chevalier,
Stanisław Małolepszy,
Elango Cheran,
Richard Gibson,
Mark Davis,
Mihai Niță,
Steven R. Loomis,
Shane F. Carr,
Matt Radbourne,
Caleb Maclennan,
David Filip,
Christopher Dieringer,
Danny Gleckler,
Bruno Haible,
Daniel Minor,
George Rhoten,
Ujjwal Sharma,
Markus Scherer,
Lionel Rowe,
Luca Casonato,
Daniel Ehrenberg,
Zibi Braniecki,
and Rafael Xavier de Souza.

Eemeli Aro is the current chair of the working group.
Addison Phillips was chair of the working group from January 2023 to July 2025.
Prior to 2023, the group was governed by a chair group, consisting of
Romulo Cintra,
Elango Cheran,
Mihai Niță,
David Filip,
Nicolas Bouvrette,
Stanisław Małolepszy,
Rafael Xavier de Souza,
Addison Phillips,
and Daniel Minor.
Romulo Cintra chaired the chair group.

* * *

© 2001–2026 Unicode, Inc.
This publication is protected by copyright, and permission must be obtained from Unicode, Inc.
prior to any reproduction, modification, or other use not permitted by the [Terms of Use](https://www.unicode.org/copyright.html).
Specifically, you may make copies of this publication and may annotate and translate it solely for personal or internal business purposes and not for public distribution,
provided that any such permitted copies and modifications fully reproduce all copyright and other legal notices contained in the original.
* You may not make copies of or modifications to this publication for public distribution, or incorporate it in whole or in part into any product or publication without the express written permission of Unicode.


Use of all Unicode Products, including this publication, is governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html).
The authors, contributors, and publishers have taken care in the preparation of this publication,
* but make no express or implied representation or warranty of any kind and assume no responsibility or liability for errors or omissions or for consequential or incidental damages that may arise therefrom.

This publication is provided “AS-IS” without charge as a convenience to users.

Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.
