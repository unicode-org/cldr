## <a name="Choice_Patterns" id="Choice_Patterns" href="#Choice_Patterns">Choice Patterns</a>

A choice pattern is a string that chooses among a number of strings, based on numeric value. It has the following form:

```text
<choice_pattern> = <choice> ( '|' <choice> )*
<choice> = <number><relation><string>
<number> = ('+' | '-')? ('∞' | [0-9]+ ('.' [0-9]+)?)
<relation> = '<' | ' ≤'
```

* The interpretation of a choice pattern is that given a number N, the pattern is scanned from right to left, for each choice evaluating ``<number>` `<relation>` N`. The first choice that matches results in the corresponding string. If no match is found, then the first string is used. For example:


<!-- HTML: rowspan -->

<table>
<tbody>
<tr><th>Pattern</th><th>N</th><th>Result</th></tr>
<tr><td rowspan="4">0≤Rf|1≤Ru|1&lt;Re</td><td>-∞, -3, -1, -0.000001</td><td>Rf (defaulted to first string)</td></tr>
<tr><td>0, 0.01, 0.9999</td><td>Rf</td></tr>
<tr><td>1</td><td>Ru</td></tr>
<tr><td>1.00001, 5, 99, ∞</td><td>Re</td></tr>
</tbody>
</table>

Quoting is done using ' characters, as in date or number formats.

