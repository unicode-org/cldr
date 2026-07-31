## <a name="Default_Functions" id="Default_Functions" href="#Default_Functions">Default Functions</a>

This section defines the **_<dfn>default functions</dfn>_**
which are REQUIRED for conformance with this specification,
along with _default functions_ that SHOULD be implemented to support
additional functionality.

To **_<dfn>accept</dfn>_** a function means that an implementation MUST NOT
emit an _Unknown Function_ error for that _function_'s _identifier_.
To _accept_ an _option_ means that a _function handler_ MUST NOT
emit a _Bad Option_ error for that _option_'s _identifier_ when used with the _function_
it is defined for
and MUST NOT emit a _Bad Option_ error for any of the _option values_
defined for that _option_.
Accepting a _function_ or its _options_ does not mean that a particular output is produced.
Implementations MAY emit an _Unsupported Operation_ error for _options_
or _option values_ that they cannot support.

_Functions_ can define _options_.
An _option_ can be REQUIRED or RECOMMENDED.

Implementations MUST _accept_ each REQUIRED _default function_ and
MUST _accept_ all _options_ defined as REQUIRED for those _functions_.

Implementations SHOULD _accept_ each RECOMMENDED _default function_.
For each such _function_, the implementation MUST accept all _options_
listed as REQUIRED for that _function_.

Implementations SHOULD _accept_ _options_ that are marked as RECOMMENDED.

Implementations MAY _accept_ _functions_ not defined in this specification.
In addition, implementations SHOULD provide mechanisms for users to
register and use user-defined _functions_ and their associated _function handlers_.
Functions not defined by any version of this specification SHOULD use
an implementation-defined or user-defined _namespace_.

Implementations MAY implement additional _options_ not defined
by any version of this specification for _default functions_.
Such _options_ MUST use an implementation-specific _namespace_.

Implementations MAY _accept_, for _options_ defined in this specification,
_option values_ which are not defined in this specification.
However, such values might become defined with a different meaning in the future,
including with a different, incompatible name
or using an incompatible value space.
Supporting implementation-specific _option values_ for _default functions_ is NOT RECOMMENDED.

Implementations MAY _accept_, for _operands_ or _options_ defined in this specification,
values with implementation-defined types.
Such values can be useful to users in cases where local usage and support exists
(including cases in which details vary from those defined by Unicode and CLDR).

> For example:
> - Implementations are encouraged to _accept_ some native representation
>   for currency amounts as the _operand_ in the _function_ `:currency`.
> - A Java implementation might _accept_ a `java.time.chrono.Chronology` object
>   as a value for the _date/time override option_ `calendar`

Future versions of this specification MAY define additional _options_ and _option values_,
subject to the rules in the [Stability Policy](#stability-policy),
for _functions_ found in this specification.
As implementations are permitted to ignore _options_ that they do not support,
it is possible to write _messages_ using _options_ not defined here
which currently format with no error, but which could produce errors
when formatted with a later edition of this specification.
Therefore, using _options_ not explicitly defined here is NOT RECOMMENDED.

### <a name="String_Value_Selection_and_Formatting" id="String_Value_Selection_and_Formatting" href="#String_Value_Selection_and_Formatting">String Value Selection and Formatting</a>

#### <a name="The_string_function" id="The_string_function" href="#The_string_function">The `:string` function</a>

The function `:string` provides string selection and formatting.

#### <a name="string_Operands" id="string_Operands" href="#string_Operands">`:string` Operands</a>

The _operand_ of `:string` is either any implementation-defined type
that is a string or for which conversion to a string is supported,
or any _literal_ value.
All other values produce a _Bad Operand_ error.

> For example, in Java, implementations of the `java.lang.CharSequence` interface
> (such as `java.lang.String` or `java.lang.StringBuilder`),
> the type `char`, or the class `java.lang.Character` might be considered
> as the "implementation-defined types".
> Such an implementation might also support other classes via the method `toString()`.
> This might be used to enable selection of a `enum` value by name, for example.
>
> Other programming languages would define string and character sequence types or
> classes according to their local needs, including, where appropriate,
> coercion to string.

#### <a name="string_Options" id="string_Options" href="#string_Options">`:string` Options</a>

The function `:string` has no _options_.

> [!NOTE]
> While `:string` has no built-in _options_,
> _options_ in the `u:` _namespace_ can be used.
> For example:
>
> ```text
> {$s :string u:dir=ltr u:id=my-string}
> ```

#### <a name="string_Resolved_Value" id="string_Resolved_Value" href="#string_Resolved_Value">`:string` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:string` _function_
contains the string value of the _operand_ of the annotated _expression_,
together with its resolved locale and directionality.
None of the _options_ set on the _expression_ are part of the _resolved value_.

#### <a name="Selection_with_string" id="Selection_with_string" href="#Selection_with_string">Selection with `:string`</a>

When implementing [Match(`resolvedSelector`, `key`)](#operations-on-resolved-values)
where `resolvedSelector` is the _resolved value_ of a _selector_
and `key` is a string,
the `:string` selector function performs as described below.

1. Let `compare` be the string value of `resolvedSelector`
   in Unicode Normalization Form C (NFC) [\[UAX#15\]](https://www.unicode.org/reports/tr15)
1. If `key` and `compare` consist of the same sequence of Unicode code points, then
   1. Return true.
1. Return false.

When implementing [BetterThan(`resolvedSelector`, `key1`, `key2`](#operations-on-resolved-values)
where `resolvedSelector` is the _resolved value_ of a _selector_
and `key1` and `key2` are strings,
the `:string` selector function performs as described below,
as the BetterThan operation should only be called on keys that match.

1. Return false.

> [!NOTE]
> Unquoted string literals in a _variant_ do not include spaces.
> If users wish to match strings that include whitespace
> (including U+3000 `IDEOGRAPHIC SPACE`)
> to a key, the `key` needs to be quoted.
>
> For example:
>
> ```text
> .input {$string :string}
> .match $string
> | space key | {{Matches the string " space key "}}
> *             {{Matches the string "space key"}}
> ```

#### <a name="string_Formatting" id="string_Formatting" href="#string_Formatting">`:string` Formatting</a>

The `:string` function returns the string value of the _resolved value_ of the _operand_.

> [!IMPORTANT]
> The function `:string` does not perform Unicode Normalization of its formatted output.
> Users SHOULD encode _messages_ and their parts in Unicode Normalization Form C (NFC)
> unless there is a very good reason not to.

### <a name="Numeric_Value_Selection_and_Formatting" id="Numeric_Value_Selection_and_Formatting" href="#Numeric_Value_Selection_and_Formatting">Numeric Value Selection and Formatting</a>

#### <a name="The_number_function" id="The_number_function" href="#The_number_function">The `:number` function</a>

The function `:number` is a selector and formatter for numeric values.

#### <a name="number_Operands" id="number_Operands" href="#number_Operands">`:number` Operands</a>

The function `:number` requires a _numeric operand_ as its _operand_.

#### <a name="number_Options" id="number_Options" href="#number_Options">`:number` Options</a>

Some options do not have default values defined in this specification.
The defaults for these options are implementation-dependent.
In general, the default values for such options depend on the locale,
the value of other options, or both.

> [!NOTE]
> The names of _options_ and their _option values_ were derived from the
> [options](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat#options)
> in JavaScript's `Intl.NumberFormat`.

The following _options_ are REQUIRED to be available on the function `:number`:

- `select` (see [Number Selection](#number-selection) below)
  - `plural` (default)
  - `ordinal`
  - `exact`
- `signDisplay`
  - `auto` (default)
  - `always`
  - `exceptZero`
  - `negative`
  - `never`
- `useGrouping`
  - `auto` (default)
  - `always`
  - `never`
  - `min2`
- `minimumIntegerDigits`
  - _digit size option_, default: `1`
- `minimumFractionDigits`
  - _digit size option_
- `maximumFractionDigits`
  - _digit size option_
- `minimumSignificantDigits`
  - _digit size option_
- `maximumSignificantDigits`
  - _digit size option_
- `trailingZeroDisplay`
  - `auto` (default)
  - `stripIfInteger`
- `roundingPriority`
  - `auto` (default)
  - `morePrecision`
  - `lessPrecision`
- `roundingIncrement`
  - 1 (default), 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 2500, and 5000
- `roundingMode`
  - `ceil`
  - `floor`
  - `expand`
  - `trunc`
  - `halfCeil`
  - `halfFloor`
  - `halfExpand` (default)
  - `halfTrunc`
  - `halfEven`

If the _operand_ of the _expression_ is an implementation-defined type,
such as the _resolved value_ of an _expression_ with a `:number` or `:integer` _annotation_,
it can include option values.
These are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.

> For example, the _placeholder_ in this _message_:
>
> ```text
> .input {$n :number minimumFractionDigits=2 signDisplay=always}
> {{{$n :number minimumFractionDigits=1}}}
> ```
>
> would be formatted with the resolved options
> `{ minimumFractionDigits: '1', signDisplay: 'always' }`.

#### <a name="number_Resolved_Value" id="number_Resolved_Value" href="#number_Resolved_Value">`:number` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:number` _function_
contains an implementation-defined numerical value
of the _operand_ of the annotated _expression_,
together with the resolved options' values.

#### <a name="Selection_with_number" id="Selection_with_number" href="#Selection_with_number">Selection with `:number`</a>

The _function_ `:number` performs selection as described in [Number Selection](#number-selection) below.

#### <a name="The_integer_function" id="The_integer_function" href="#The_integer_function">The `:integer` function</a>

The function `:integer` is a selector and formatter for matching or formatting numeric
values as integers.

#### <a name="integer_Operands" id="integer_Operands" href="#integer_Operands">`:integer` Operands</a>

The function `:integer` requires a _numeric operand_ as its _operand_.

#### <a name="integer_Options" id="integer_Options" href="#integer_Options">`:integer` Options</a>

Some options do not have default values defined in this specification.
The defaults for these options are implementation-dependent.
In general, the default values for such options depend on the locale,
the value of other options, or both.

> [!NOTE]
> The names of _options_ and their _option values_ were derived from the
> [options](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat#options)
> in JavaScript's `Intl.NumberFormat`.

The following _options_ are REQUIRED to be available on the function `:integer`:

- `select` (see [Number Selection](#number-selection) below)
  - `plural` (default)
  - `ordinal`
  - `exact`
- `signDisplay`
  - `auto` (default)
  - `always`
  - `exceptZero`
  - `negative`
  - `never`
- `useGrouping`
  - `auto` (default)
  - `always`
  - `never`
  - `min2`
- `minimumIntegerDigits`
  - _digit size option_, default: `1`
- `maximumSignificantDigits`
  - _digit size option_

If the _operand_ of the _expression_ is an implementation-defined type,
such as the _resolved value_ of an _expression_ with a `:number` or `:integer` _annotation_,
it can include option values.
In general, these are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.
Options with the following names are however discarded if included in the _operand_:

- `minimumFractionDigits`
- `maximumFractionDigits`
- `minimumSignificantDigits`

#### <a name="integer_Resolved_Value" id="integer_Resolved_Value" href="#integer_Resolved_Value">`:integer` Resolved Value</a>

The _resolved value_ of an _expression_ with an `:integer` _function_
contains the implementation-defined integer value
of the _operand_ of the annotated _expression_,
together with the resolved options' values.

#### <a name="Selection_with_integer" id="Selection_with_integer" href="#Selection_with_integer">Selection with `:integer`</a>

The _function_ `:integer` performs selection as described in [Number Selection](#number-selection) below.

#### <a name="The_offset_function" id="The_offset_function" href="#The_offset_function">The `:offset` function</a>

The _function_ `:offset` is a _selector_ and _formatter_ for matching or formatting
numeric values to which an offset has been applied.
The "offset" is a small integer adjustment of the _operand_'s value.

> This function is useful for selection and formatting of values that
> differ from the input value by a specified amount.
> For example, it can be used in a _message_ such as this:
>
> ```text
> .input {$like_count :integer}
> .local $others_count = {$like_count :offset subtract=1}
> .match $like_count $others_count
> 0 *   {{Your post has no likes.}}
> 1 *   {{{$name} liked your post.}}
> * one {{{$name} and {$others_count} other user liked your post.}}
> * *   {{{$name} and {$others_count} other users liked your post.}}
> ```

> [!NOTE]
> The purpose of this _function_ is to supply compatibility with
> ICU's `PluralFormat` and its `offset` feature, also found in ICU MessageFormat.

#### <a name="offset_Operands" id="offset_Operands" href="#offset_Operands">`:offset` Operands</a>

The function `:offset` requires a _numeric operand_ as its _operand_.

#### <a name="offset_Options" id="offset_Options" href="#offset_Options">`:offset` Options</a>

The _options_ on `:offset` are exclusive with each other,
and exactly one _option_ is always required.
The _options_ do not have default values.

The following _options_ are REQUIRED to be available on the function `:offset`:

- `add`
  - _digit size option_
- `subtract`
  - _digit size option_

If no _options_ or more than one _option_ is set,
or if an _option value_ is not a _digit size option_,
a _Bad Option_ error is emitted
and a _fallback value_ used as the _resolved value_ of the _expression_.

#### <a name="offset_Resolved_Value" id="offset_Resolved_Value" href="#offset_Resolved_Value">`:offset` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:offset` _function_
contains the implementation-defined numeric value
of the _operand_ of the annotated _expression_.

If the `add` _option_ is set,
the numeric value of the _resolved value_ is formed by incrementing
the numeric value of the _operand_ by the integer value of the _digit size option_.

If the `subtract` _option_ is set,
the numeric value of the _resolved value_ is formed by decrementing
the numeric value of the _operand_ by the integer value of the _digit size option_.

If the _operand_ of the _expression_ is an implementation-defined numeric type,
such as the _resolved value_ of an _expression_ with a `:number` or `:integer` _annotation_,
it can include option values.
These are included in the resolved option values of the _expression_.
The `:offset` _options_ are not included in the resolved option values.

> [!NOTE]
> Implementations can encounter practical limits with `:offset` _expressions_,
> such as the result of adding two integers exceeding
> the storage or precision of some implementation-defined number type.
> In such cases, implementations can emit an _Unsupported Operation_ error
> or they might just silently overflow the underlying data value.

#### <a name="Selection_with_offset" id="Selection_with_offset" href="#Selection_with_offset">Selection with `:offset`</a>

The _function_ `:offset` performs selection as described in [Number Selection](#number-selection) below.

#### <a name="The_currency_function" id="The_currency_function" href="#The_currency_function">The `:currency` function</a>

The _function_ `:currency` is a _formatter_ for currency values,
which is a specialized form of numeric formatting.

#### <a name="currency_Operands" id="currency_Operands" href="#currency_Operands">`:currency` Operands</a>

The _operand_ of the `:currency` function can be one of any number of
implementation-defined types,
each of which contains a numerical `value` and a `currency`;
or it can be a _numeric operand_, as long as the _option_
`currency` is provided.
The _option_ `currency` MUST NOT be used to override the currency of an implementation-defined type.
Using this _option_ in such a case results in a _Bad Option_ error.

The value of the _operand_'s `currency` MUST be either a string containing a
well-formed [Unicode Currency Identifier](tr35.md#UnicodeCurrencyIdentifier)
or an implementation-defined currency type.
Currency codes are case-insensitive.
A well-formed Unicode Currency Identifier matches the production `currency_code` in this ABNF:

```abnf
currency_code = 3ALPHA
```

A _numeric operand_ without a `currency` _option_ results in a _Bad Operand_ error.

> [!NOTE]
> For example, in ICU4J, the type `com.ibm.icu.util.CurrencyAmount` can be used
> to set the amount and currency.

> [!NOTE]
> The `currency` is only required to be well-formed rather than checked for validity.
> This allows new currency codes to be defined
> (there are many recent examples of this occuring).
> It also avoids requiring implementations to check currency codes for validity,
> although implementations are permitted to emit _Bad Option_ or _Bad Operand_ for invalid codes.

> [!NOTE]
> For runtime environments that do not provide a ready-made data structure,
> class, or type for currency values, the implementation ought to provide
> a data structure, convenience function, or documentation on how to encode
> the value and currency code for formatting.
> For example, such an implementation might define a "currency operand"
> to include a key-value structure with specific keys to be the
> local currency operand, which might look like the following:
>
> ```text
> {
>    "value": 123.45,
>    "currency": "EUR"
> }
> ```

#### <a name="currency_Options" id="currency_Options" href="#currency_Options">`:currency` Options</a>

Some options do not have default values defined in this specification.
The defaults for these options are implementation-dependent.
In general, the default values for such options depend on the locale,
the currency,
the value of other options, or all of these.

Fraction digits for currency values behave differently than for other numeric formatters.
The number of fraction digits displayed is usually set by the currency used.
For example, USD uses 2 fraction digits, while JPY uses none.
Setting some other number of `fractionDigits` allows greater precision display
(such as when performing currency conversions or other specialized operations)
or disabling fraction digits if set to `0`.

The _option_ `trailingZeroDisplay` has an _option value_ `stripIfInteger` that is useful
for displaying currencies with their fraction digits removed when the fraction
part of the _operand_ is zero.
This is sometimes used in _messages_ to make the displayed value omit the fraction part
automatically.

> For example, this _message_:
>
> ```text
> The special price is {$price :currency trailingZeroDisplay=stripIfInteger}.
> ```
>
> When used with the value `5.00 USD` in the `en-US` locale displays as:
>
> ```text
> The special price is $5.
> ```
>
> But like this when when value is `5.01 USD`:
>
> ```text
> The special price is $5.01.
> ```

Implementations MAY internally alias _option values_ that they do not have data or a backing implementation for.
Notably, the `currencyDisplay` option has a rich set of values that mirrors developments in CLDR data.
Some implementations might not be able to produce all of these formats for every currency.

> [!NOTE]
> Except where noted otherwise, the names of _options_ and their _option values_ were derived from the
> [options](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat#options)
> in JavaScript's `Intl.NumberFormat`.

The following _options_ are REQUIRED to be available on the function `:currency`:

- `currency`
  - well-formed [Unicode Currency Identifier](tr35.md#UnicodeCurrencyIdentifier)
    (no default)
- `currencySign`
  - `accounting`
  - `standard` (default)
- `currencyDisplay`
  - `narrowSymbol`
  - `symbol` (default)
  - `name`
  - `code`
  - `never` (this is called `hidden` in ICU)
- `useGrouping`
  - `auto` (default)
  - `always`
  - `never`
  - `min2`
- `minimumIntegerDigits`
  - _digit size option_, default: `1`
- `fractionDigits` (unlike number/integer formats, the fraction digits for currency formatting are fixed)
  - `auto` (default) (the number of digits used by the currency)
  - _digit size option_
- `minimumSignificantDigits`
  - _digit size option_
- `maximumSignificantDigits`
  - _digit size option_
- `trailingZeroDisplay`
  - `auto` (default)
  - `stripIfInteger`
- `roundingPriority`
  - `auto` (default)
  - `morePrecision`
  - `lessPrecision`
- `roundingIncrement`
  - 1 (default), 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 2500, and 5000
- `roundingMode`
  - `ceil`
  - `floor`
  - `expand`
  - `trunc`
  - `halfCeil`
  - `halfFloor`
  - `halfExpand` (default)
  - `halfTrunc`
  - `halfEven`

If the _operand_ of the _expression_ is an implementation-defined type,
such as the _resolved value_ of an _expression_ with a `:currency` _annotation_,
it can include option values.
These are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.

> For example, the _placeholder_ in this _message_:
>
> ```text
> .input {$n :currency currency=USD trailingZeroDisplay=stripIfInteger}
> {{{$n :currency currencySign=accounting}}}
> ```
>
> would be formatted with the resolved options
> `{ currencySign: 'accounting', trailingZeroDisplay: 'stripIfInteger', currency: 'USD' }`.

#### <a name="currency_Resolved_Value" id="currency_Resolved_Value" href="#currency_Resolved_Value">`:currency` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:currency` _function_
contains an implementation-defined currency value
of the _operand_ of the annotated _expression_,
together with the resolved options' values.

#### <a name="The_percent_function" id="The_percent_function" href="#The_percent_function">The `:percent` function</a>

The function `:percent` is a selector and formatter for percent values.

#### <a name="percent_Operands" id="percent_Operands" href="#percent_Operands">`:percent` Operands</a>

The function `:percent` requires a _numeric operand_ as its _operand_.

When either selecting or formatting the _expression_,
the numeric value of the _operand_ is multiplied by 100.

#### <a name="percent_Options" id="percent_Options" href="#percent_Options">`:percent` Options</a>

Some options do not have default values defined in this specification.
The defaults for these options are implementation-dependent.
In general, the default values for such options depend on the locale,
the value of other options, or both.

> [!NOTE]
> The names of _options_ and their _option values_ were derived from the
> [options](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat/NumberFormat#options)
> in JavaScript's `Intl.NumberFormat`.

The following _options_ are REQUIRED to be available on the function `:percent`:

- `signDisplay`
  - `auto` (default)
  - `always`
  - `exceptZero`
  - `negative`
  - `never`
- `useGrouping`
  - `auto` (default)
  - `always`
  - `never`
  - `min2`
- `minimumFractionDigits`
  - _digit size option_, default: `0`
- `maximumFractionDigits`
  - _digit size option_, default: `0`
- `minimumSignificantDigits`
  - _digit size option_
- `maximumSignificantDigits`
  - _digit size option_
- `trailingZeroDisplay`
  - `auto` (default)
  - `stripIfInteger`
- `roundingPriority`
  - `auto` (default)
  - `morePrecision`
  - `lessPrecision`
- `roundingMode`
  - `ceil`
  - `floor`
  - `expand`
  - `trunc`
  - `halfCeil`
  - `halfFloor`
  - `halfExpand` (default)
  - `halfTrunc`
  - `halfEven`

The numeric value of the _operand_ is multiplied by 100
at the start of formatting or selection.
Each _option_ is applied to the formatted (or selected) value
rather than the unaltered value of the _operand_.

> For example, this _placeholder_:
>
> ```text
> {0.1234 :percent maximumFractionDigits=1}
> ```
>
> might be formatted as "12.3%" in an English locale.

If the _operand_ of the _expression_ is an implementation-defined type,
such as the _resolved value_ of an _expression_ with a `:number` or `:integer` _annotation_,
it can include option values.
In general, these are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.
Options with the following names are however discarded if included in the _operand_:

- `minimumIntegerDigits`
- `roundingIncrement`
- `select`

#### <a name="percent_Resolved_Value" id="percent_Resolved_Value" href="#percent_Resolved_Value">`:percent` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:percent` _function_
contains an implementation-defined numerical value
of the _operand_ of the annotated _expression_
together with the resolved options' values.
The numerical value of the _resolved value_ of the _expression_
is the same as the numerical value of its _operand_;
it is not multiplied by 100.

#### <a name="Selection_with_percent" id="Selection_with_percent" href="#Selection_with_percent">Selection with `:percent`</a>

The _function_ `:percent` performs selection as described in [Number Selection](#number-selection) below.
This selection always uses the `plural` selection mode,
and is performed on the numerical value of the _operand_
multiplied by 100.

> For example, this _message_:
> ```text
> .local $pct = {1 :percent}
> .match $pct
> 1   {{Would match with 0.01 as the operand}}
> 100 {{Matches 💯}}
> *   {{Otherwise}}
> ```
>
> would be formatted as "Matches 💯".

#### <a name="The_unit_function" id="The_unit_function" href="#The_unit_function">The `:unit` function</a>

> [!IMPORTANT]
> The _function_ `:unit` has a status of **Draft**.
> It is proposed for inclusion in a future release of this specification and is not Stable.

The _function_ `:unit` is proposed to be a RECOMMENDED formatter for unitized values,
that is, for numeric values associated with a unit of measurement.
This is a specialized form of numeric formatting.

#### <a name="unit_Operands" id="unit_Operands" href="#unit_Operands">`:unit` Operands</a>

The _operand_ of the `:unit` function can be one of any number of
implementation-defined types,
each of which contains a numerical `value` plus a `unit`
or it can be a _numeric operand_, as long as the _option_
`unit` is provided.

Valid values of the _operand_'s `unit` are either a string containing a
valid [Unit Identifier](tr35-general.md#unit-identifiers)
or an implementation-defined unit type.

A _numeric operand_ without a `unit` _option_ results in a _Bad Operand_ error.

> [!NOTE]
> For example, in ICU4J, the type `com.ibm.icu.util.Measure` might be used
> as an _operand_ for `:unit` because it contains the `value` and `unit`.

> [!NOTE]
> For runtime environments that do not provide a ready-made data structure,
> class, or type for unit values, the implementation ought to provide
> a data structure, convenience function, or documentation on how to encode
> the value and unit for formatting.
> For example, such an implementation might define a "unit operand"
> to include a key-value structure with specific keys to be the
> local unit operand, which might look like the following:
>
> ```text
> {
>    "value": 123.45,
>    "unit": "kilometer-per-hour"
> }
> ```

#### <a name="unit_Options" id="unit_Options" href="#unit_Options">`:unit` Options</a>

Some _options_ do not have default values defined in this specification.
The defaults for these _options_ are implementation-dependent.
In general, the default values for such _options_ depend on the locale,
the unit,
the value of other _options_, or all of these.

The following _options_ are REQUIRED to be available on the function `:unit`,
unless otherwise indicated:

- `unit`
  - valid [Unit Identifier](tr35-general.md#unit-identifiers)
    (no default)
- `usage` \[RECOMMENDED\]
  - valid [Unicode Unit Preference](tr35-info.md#unit-preferences)
    (no default, see [Unit Conversion](#unit-conversion) below)
- `unitDisplay`
  - `short` (default)
  - `narrow`
  - `long`
- `signDisplay`
  - `auto` (default)
  - `always`
  - `exceptZero`
  - `negative`
  - `never`
- `useGrouping`
  - `auto` (default)
  - `always`
  - `never`
  - `min2`
- `minimumIntegerDigits`
  - _digit size option_, default: `1`
- `minimumFractionDigits`
  - _digit size option_
- `maximumFractionDigits`
  - _digit size option_
- `minimumSignificantDigits`
  - _digit size option_
- `maximumSignificantDigits`
  - _digit size option_
- `roundingPriority`
  - `auto` (default)
  - `morePrecision`
  - `lessPrecision`
- `roundingIncrement`
  - 1 (default), 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 2500, and 5000
- `roundingMode`
  - `ceil`
  - `floor`
  - `expand`
  - `trunc`
  - `halfCeil`
  - `halfFloor`
  - `halfExpand` (default)
  - `halfTrunc`
  - `halfEven`

If the _operand_ of the _expression_ is an implementation-defined type,
such as the _resolved value_ of an _expression_ with a `:unit` _annotation_,
it can include option values.
These are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.

> For example, the _placeholder_ in this _message_:
>
> ```text
> .input {$n :unit unit=furlong minimumFractionDigits=2}
> {{{$n :unit minimumIntegerDigits=1}}}
> ```
>
> would have the resolved options:
> `{ unit: 'furlong', minimumFractionDigits: '2', minimumIntegerDigits: '1' }`.

#### <a name="unit_Resolved_Value" id="unit_Resolved_Value" href="#unit_Resolved_Value">`:unit` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:unit` _function_
consist of an implementation-defined unit value
of the _operand_ of the annotated _expression_,
together with the resolved options and their resolved values.

#### <a name="Unit_Conversion" id="Unit_Conversion" href="#Unit_Conversion">Unit Conversion</a>

Implementations MAY support conversion to the locale's preferred units via the `usage` _option_.
Implementing this _option_ is optional.
Not all `usage` _option values_ are compatible with a given unit.
Implementations SHOULD emit an _Unsupported Operation_ error if the requested conversion is not supported.

> For example, trying to convert a `length` unit (such as "meters")
> to a `volume` usage (which might be a unit akin to "liters" or "gallons", depending on the locale)
> could produce an _Unsupported Operation_ error.

Implementations MUST NOT substitute the unit without performing the associated conversion.

> For example, consider the value:
>
> ```text
> {
>    "value": 123.5,
>    "unit": "meter"
> }
> ```
>
> The following _message_ might convert the formatted result to U.S. customary units
> in the `en-US` locale:
>
> ```text
> You have {$v :unit usage=road maximumFractionDigits=0} to go.
> ```
>
> This can produce "You have 405 feet to go."

#### <a name="Numeric_Operands" id="Numeric_Operands" href="#Numeric_Operands">Numeric Operands</a>

A **_<dfn>numeric operand<dfn>_** is either an implementation-defined type or
a _literal_ whose contents match the following `number-literal` production.
All other values produce a _Bad Operand_ error.

```abnf
number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
```

> For example, in Java, any subclass of `java.lang.Number` plus the primitive
> types (`byte`, `short`, `int`, `long`, `float`, `double`, etc.)
> might be considered as the "implementation-defined numeric types".
> Implementations in other programming languages would define different types
> or classes according to their local needs.

> [!NOTE]
> String values passed as variables in the _formatting context_'s
> _input mapping_ can be formatted as numeric values as long as their
> contents match the `number-literal` production.
>
> For example, if the value of the variable `num` were the string
> `-1234.567`, it would behave identically to the local
> variable in this example:
>
> ```text
> .local $example = {|-1234.567| :number}
> {{{$num :number} == {$example}}}
> ```

> [!NOTE]
> Implementations are encouraged to provide support for compound types or data structures
> that provide additional semantic meaning to the formatting of number-like values.
> For example, in ICU4J, the type `com.ibm.icu.util.Measure` can be used to communicate
> a value that includes a unit
> or the type `com.ibm.icu.util.CurrencyAmount` can be used to set the currency and related
> options (such as the number of fraction digits).

#### <a name="Digit_Size_Options" id="Digit_Size_Options" href="#Digit_Size_Options">Digit Size Options</a>

Some _options_ of number _functions_ are defined to take a _digit size option_.
The _function handlers_ for number _functions_ use these _options_ to control aspects of numeric display
such as the number of fraction, integer, or significant digits.

A **_<dfn>digit size option</dfn>_** is an _option_
whose _option value_ is interpreted by the _function_
as a small integer greater than or equal to zero.
Implementations MAY define upper and lower limits on the _resolved value_
of a _digit size option_ consistent with that implementation's practical limits.

In most cases, the value of a _digit size option_ will be a string that
encodes the value as a non-negative integer.
Implementations MAY also accept implementation-defined types as the _option value_.
When provided as a string, the representation of a _digit size option_ matches the following ABNF:

```abnf
digit-size-option = "0" / (("1"-"9") [DIGIT])
```

If the value of a _digit size option_ does not evaluate as a non-negative integer,
or if the value exceeds any implementation-defined and option-specific upper or lower limit,
the implementation will emit a _Bad Option Error_
and ignore the _option_.
An implementation MAY replace a _digit size option_
that exceeds an implementation-defined or option-specific upper or lower limit
with an implementation-defined value rather than ignoring the _option_.
Any such replacement value becomes the _resolved value_ of that _option_.

> For example, if an implementation imposed an upper limit of 20 on the _option_
> `minimumIntegerDigits` for the function `:number`
> then the _resolved value_ of the _option_ `minimumIntegerDigits`
> for both `$x` and `$y` in the following _message_ would be 20:
> ```text
> .input {$x :number minimumIntegerDigits=999}
> .local $y = {$x}
> {{{$y}}}
> ```

#### <a name="Number_Selection" id="Number_Selection" href="#Number_Selection">Number Selection</a>

The _option value_ of the `select` _option_ MUST be set by a _literal_.
Allowing a _variable_ _option value_ for `select` would produce a _message_ that
is impossible to translate because the set of _keys_ is tied to the _selector_ chosen.
If the _option value_ is a _variable_ or
if the `select` option is set by an implementation-defined type used as an _operand_,
a _Bad Option Error_ is emitted and
the _resolved value_ of the expression MUST NOT support selection.
The formatting of the _resolved value_ is not affected by the `select` _option_.

Number selection has three modes:

- `exact` selection matches the operand to explicit numeric keys exactly
- `plural` selection matches the operand to explicit numeric keys exactly
  followed by a plural rule category if there is no explicit match
- `ordinal` selection matches the operand to explicit numeric keys exactly
  followed by an ordinal rule category if there is no explicit match

When implementing [Match(`resolvedSelector`, `key`)](#operations-on-resolved-values)
where `resolvedSelector` is the _resolved value_ of a _selector_
and `key` is a string,
numeric selectors perform as described below.

1. Let `exact` be the serialized representation of the numeric value of `resolvedSelector`.
   (See [Exact Literal Match Serialization](#exact-literal-match-serialization) for details)
1. Let `keyword` be a string which is the result of [rule selection](#rule-selection) on `resolvedSelector`.
1. If the value of `key` matches the production `number-literal`, then
      1. If `key` and `exact` consist of the same sequence of Unicode code points, then
         1. Return true.
      1. Return false.
1. If `key` is one of the keywords `zero`, `one`, `two`, `few`, `many`, or `other`, then
      1. If `key` and `keyword` consist of the same sequence of Unicode code points, then
         1. Return true.
      1. Return false.
1. Emit a _Bad Variant Key_ error.

When implementing [BetterThan(`resolvedSelector`, `key1`, `key2`)](#operations-on-resolved-values)
where `resolvedSelector` is the _resolved value_ of a _selector_
and `key1` and `key2` are strings,
numeric selectors perform as described below.

1. Assert that Match(`resolvedSelector`, `key1`) is true.
1. Assert that Match(`resolvedSelector`, `key2`) is true.
1. If the value of `key1` matches the production `number-literal`, then
   1. If the value of `key2` does not match the production `number-literal`, then
      1. Return true.
1. Return false.

> [!NOTE]
> Implementations are not required to implement this exactly as written.
> However, the observed behavior must be consistent with what is described here.

#### <a name="Default_Value_of_select_Option" id="Default_Value_of_select_Option" href="#Default_Value_of_select_Option">Default Value of `select` Option</a>

The _option value_ `plural` is the default for the _option_ `select`
because it is the most common use case for numeric selection.
It can be used for exact value matches but also allows for the grammatical needs of
languages using CLDR's plural rules.
This might not be noticeable in the source language (particularly English),
but can cause problems in target locales that the original developer is not considering.

> For example, a naive developer might use a special message for the value `1` without
> considering a locale's need for a `one` plural:
>
> ```text
> .input {$var :number}
> .match $var
> 1   {{You have one last chance}}
> one {{You have {$var} chance remaining}}
> *   {{You have {$var} chances remaining}}
> ```
>
> The `one` variant is needed by languages such as Polish or Russian.
> Such locales typically also require other keywords such as `two`, `few`, and `many`.

#### <a name="Rule_Selection" id="Rule_Selection" href="#Rule_Selection">Rule Selection</a>

Rule selection is intended to support the grammatical matching needs of different
languages/locales in order to support plural or ordinal numeric values.

If the `select` _option value_ is `exact`, rule-based selection is not used.
Otherwise rule selection matches the _operand_, as modified by function _options_, to exactly one of these keywords:
`zero`, `one`, `two`, `few`, `many`, or `other`.
The keyword `other` is the default.

> [!NOTE]
> Since valid keys cannot be the empty string in a numeric expression, returning the
> empty string disables keyword selection.

The meaning of the keywords is locale-dependent and implementation-defined.
A _key_ that matches the rule-selected keyword is a stronger match than the fallback key `*`
but a weaker match than any exact match _key_ value.

The rules for a given locale might not produce all of the keywords.
A given _operand_ value might produce different keywords depending on the locale.

Apply the rules to the _resolved value_ of the _operand_ and the relevant function _options_,
and return the resulting keyword.
If no rules match, return `other`.

If the `select` _option value_ is `plural`, the rules applied to selection SHOULD be
the CLDR plural rule data of type `cardinal`.
See [charts](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html)
for examples.

If the `select` _option value_ is `ordinal`, the rules applied to selection SHOULD be
the CLDR plural rule data of type `ordinal`.
See [charts](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html)
for examples.

> **Example.**
> In CLDR 44, the Czech (`cs`) plural rule set can be found
> [here](https://www.unicode.org/cldr/charts/44/supplemental/language_plural_rules.html#cs).
>
> A message in Czech might be:
>
> ```text
> .input {$numDays :number}
> .match $numDays
> one  {{{$numDays} den}}
> few  {{{$numDays} dny}}
> many {{{$numDays} dne}}
> *    {{{$numDays} dní}}
> ```
>
> Using the rules found above, the results of various _operand_ values might look like:
> | Operand value | Keyword | Formatted Message |
> |---|---|---|
> | 1 | `one` | 1 den |
> | 2 | `few` | 2 dny |
> | 5 | `other` | 5 dní |
> | 22 | `few` | 22 dny |
> | 27 | `other` | 27 dní |
> | 2.4 | `many` | 2,4 dne |

#### <a name="Exact_Literal_Match_Serialization" id="Exact_Literal_Match_Serialization" href="#Exact_Literal_Match_Serialization">Exact Literal Match Serialization</a>

If the numeric value of `resolvedSelector` is an integer
and none of the following options are set for `resolvedSelector`,
the serialized form of the numeric value MUST match the ABNF defined below for `integer`,
representing its decimal value:

- `minimumFractionDigits`
- `minimumIntegerDigits`
- `minimumSignificantDigits`
- `maximumSignificantDigits`

```abnf
integer = "0" / ["-"] ("1"-"9") *DIGIT
```

Otherwise, the serialized form of the numeric value is implementation-defined.

> [!IMPORTANT]
> The exact behavior of exact literal match is only well defined
> for integer values without leading zeros.
> Functions that use fraction digits or significant digits
> might work in specific implementation-defined ways.
> Users should avoid depending on these types of keys in message selection.

### <a name="Date_and_Time_Value_Formatting" id="Date_and_Time_Value_Formatting" href="#Date_and_Time_Value_Formatting">Date and Time Value Formatting</a>

This subsection describes the _functions_ and _options_ for date/time formatting.

> [!IMPORTANT]
> The _functions_ in this section have a status of **Draft**.
> They are proposed for inclusion in a future release and are not Stable.
> The _options_ and _option values_ used by `:datetime`, `:date`, and `:time`
> are based on [Semantic Skeletons], which are in technical preview.
> The set of _options_ and _option values_ will be extended by later versions of this specification.

> [!NOTE]
> Selection based on date/time types is not required by this release of MessageFormat.
> Use care when defining implementation-specific _selectors_ based on date/time types.
> The types of queries found in implementations such as `java.time.TemporalAccessor`
> are complex and user expectations might be inconsistent with good I18N practices.

[Semantic Skeletons]: https://www.unicode.org/reports/tr35/tr35-dates.html#Semantic_Skeletons

#### <a name="The_datetime_function" id="The_datetime_function" href="#The_datetime_function">The `:datetime` function</a>

The function `:datetime` is used to format a date/time value.
Its formatted result will always include both the date and the time,
and optionally a timezone.

If no options are specified, this function defaults to the following:

- `{$d :datetime}` is the same as<br>
  `{$d :datetime dateFields=year-month-day timePrecision=minute}`

> [!NOTE]
> The formatting behavior of `:datetime` is inconsistent with `Intl.DateTimeFormat`
> in JavaScript and with `{d,date}` in ICU MessageFormat 1.0.
> This is because, unlike those implementations, `:datetime` is distinct from `:date` and `:time`.

#### <a name="datetime_Operands" id="datetime_Operands" href="#datetime_Operands">`:datetime` Operands</a>

The _operand_ of the `:datetime` function is either
an implementation-defined date/time type
or a _date/time literal value_, as defined in [Date and Time Operand](#date-and-time-operands).
All other _operand_ values produce a _Bad Operand_ error.

#### <a name="datetime_Options" id="datetime_Options" href="#datetime_Options">`:datetime` Options</a>

The following _options_ are REQUIRED to be available on the function `:datetime`:

- `dateFields`
  - `weekday`
  - `day-weekday`
  - `month-day`
  - `month-day-weekday`
  - `year-month-day` (default)
  - `year-month-day-weekday`
- `dateLength`
  - `long`
  - `medium` (default)
  - `short`
- `timePrecision`
  - `hour`
  - `minute` (default)
  - `second`
- `timeZoneStyle`
  - `long`
  - `short`
- _Date/time override options_

If the `timeZoneStyle` _option_ is not included in the _expression_,
its formatted result will not include a timezone indicator.

Except for _date/time override options_,
each `:datetime` _option value_ MUST be set by a _literal_.
If such an _option value_ is a _variable_,
a _Bad Option Error_ is emitted and
the _option_ is ignored when formatting the _expression_.

If the _operand_ of the _expression_ is an implementation-defined date/time type,
it can include other option values.
Any _date/time override options_ of the operand are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.
Any _operand_ options not matching the _date/time override options_ are ignored.

#### <a name="datetime_Resolved_Value" id="datetime_Resolved_Value" href="#datetime_Resolved_Value">`:datetime` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:datetime` _function_
contains an implementation-defined date/time value
of the _operand_ of the annotated _expression_,
together with the resolved options values.

#### <a name="The_date_function" id="The_date_function" href="#The_date_function">The `:date` function</a>

The function `:date` is used to format the date portion of date/time values.

If no options are specified, this function defaults to the following:

- `{$d :date}` is the same as `{$d :date fields=year-month-day length=medium}`

#### <a name="date_Operands" id="date_Operands" href="#date_Operands">`:date` Operands</a>

The _operand_ of the `:date` function is either
an implementation-defined date/time type
or a _date/time literal value_, as defined in [Date and Time Operand](#date-and-time-operands).
All other _operand_ values produce a _Bad Operand_ error.

#### <a name="date_Options" id="date_Options" href="#date_Options">`:date` Options</a>

The following _options_ are REQUIRED to be available on the function `:date`:

- `fields`
  - `weekday`
  - `day-weekday`
  - `month-day`
  - `month-day-weekday`
  - `year-month-day` (default)
  - `year-month-day-weekday`
- `length`
  - `long`
  - `medium` (default)
  - `short`
- _Date/time override options_

The `fields` and `length` _option values_ MUST each be set by a _literal_.
If such an _option value_ is a _variable_,
a _Bad Option Error_ is emitted and
the _option_ is ignored when formatting the _expression_.

If the _operand_ of the _expression_ is an implementation-defined date/time type,
it can include other option values.
Any _date/time override options_ of the operand are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.
Any _operand_ options not matching the _date/time override options_ are ignored.

#### <a name="date_Resolved_Value" id="date_Resolved_Value" href="#date_Resolved_Value">`:date` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:date` _function_
is implementation-defined.

An implementation MAY emit a _Bad Operand_ or _Bad Option_ error (as appropriate)
when a _variable_ annotated directly or indirectly by a `:date` _annotation_
is used as an _operand_ or an _option value_.

#### <a name="The_time_function" id="The_time_function" href="#The_time_function">The `:time` function</a>

The function `:time` is used to format the time portion of date/time values.
Its formatted result will always include the time,
and optionally a timezone.

If no options are specified, this function defaults to the following:

- `{$t :time}` is the same as `{$t :time precision=minute}`

#### <a name="time_Operands" id="time_Operands" href="#time_Operands">`:time` Operands</a>

The _operand_ of the `:time` function is either
an implementation-defined date/time type
or a _date/time literal value_, as defined in [Date and Time Operand](#date-and-time-operands).
All other _operand_ values produce a _Bad Operand_ error.

#### <a name="time_Options" id="time_Options" href="#time_Options">`:time` Options</a>

The following _options_ are REQUIRED to be available on the function `:time`:

- `precision`
  - `hour`
  - `minute` (default)
  - `second`
- `timeZoneStyle`
  - `long`
  - `short`
- _Date/time override options_

If the `timeZoneStyle` _option_ is not included in the _expression_,
its formatted result will not include a timezone indicator.

The `precision` and `timeZoneStyle` _option values_ MUST each be set by a _literal_.
If such an _option value_ is a _variable_,
a _Bad Option Error_ is emitted and
the _option_ is ignored when formatting the _expression_.

If the _operand_ of the _expression_ is an implementation-defined date/time type,
it can include other option values.
Any _date/time override options_ of the operand are included in the resolved option values of the _expression_,
with _options_ on the _expression_ taking priority over any options of the _operand_.
Any _operand_ options not matching the _date/time override options_ are ignored.

#### <a name="time_Resolved_Value" id="time_Resolved_Value" href="#time_Resolved_Value">`:time` Resolved Value</a>

The _resolved value_ of an _expression_ with a `:time` _function_
is implementation-defined.

An implementation MAY emit a _Bad Operand_ or _Bad Option_ error (as appropriate)
when a _variable_ annotated directly or indirectly by a `:time` _annotation_
is used as an _operand_ or an _option value_.

#### <a name="Date_and_Time_Operands" id="Date_and_Time_Operands" href="#Date_and_Time_Operands">Date and Time Operands</a>

The _operand_ of a date/time function is either
an implementation-defined date/time type
or a _date/time literal value_, as defined below.
All other _operand_ values produce a _Bad Operand_ error.

A **_<dfn>date/time literal value</dfn>_** is a non-empty string consisting of an ISO 8601 date,
or an ISO 8601 datetime optionally followed by a timezone offset.
As implementations differ slightly in their parsing of such strings,
ISO 8601 date and datetime values not matching the following regular expression MAY also be supported.
Furthermore, matching this regular expression does not guarantee validity,
given the variable number of days in each month.

```text
(?!0000)[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])(T([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\.[0-9]{1,3})?(Z|[+-]((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?)?
```

When the time is not present, implementations SHOULD use `00:00:00` as the time.
When the offset is not present, implementations SHOULD use a floating time type
(such as Java's `java.time.LocalDateTime`) to represent the time value.
For more information, see [Working with Timezones](https://w3c.github.io/timezone).

> [!IMPORTANT]
> The [ABNF](#messageabnf) and [syntax](#syntax) of Unicode MessageFormat
> do not formally define date/time literals.
> This means that a _message_ can be syntactically valid but produce
> a _Bad Operand_ error at runtime.

> [!NOTE]
> String values passed as variables in the _formatting context_'s
> _input mapping_ can be formatted as date/time values as long as their
> contents are date/time literals.
>
> For example, if the value of the variable `now` were the string
> `2024-02-06T16:40:00Z`, it would behave identically to the local
> variable in this example:
>
> ```text
> .local $example = {|2024-02-06T16:40:00Z| :datetime}
> {{{$now :datetime} == {$example}}}
> ```

> [!NOTE]
> True time zone support in serializations is expected to coincide with the adoption
> of Temporal in JavaScript.
> The form of these serializations is known and is a de facto standard.
> Support for these extensions is expected to be required in the post-tech preview.
> See: https://datatracker.ietf.org/doc/draft-ietf-sedate-datetime-extended/

#### <a name="Date_and_Time_Override_Options" id="Date_and_Time_Override_Options" href="#Date_and_Time_Override_Options">Date and Time Override Options</a>

**_<dfn>Date/time override options</dfn>_** are _options_ that allow an _expression_ to
override values set by the current locale,
or provided by the _formatting context_ (such as the default time zone),
or embedded in an implementation-defined date/time _operand_ value.

> [!NOTE]
> These _options_ do not have default values because they are only to be used
> as overrides for locale-and-value dependent implementation-defined defaults.

The following _option_ is REQUIRED to be available on
the functions `:datetime`, `:date`, and `:time`.

- `timeZone`
  - A valid time zone identifier
    (see [TZDB](https://www.iana.org/time-zones)
    and [LDML](tr35-dates.md#Time_Zone_Names)
    for information on identifiers)
  - `input`
  - `UTC`

The default value for `timeZone` is the default time zone provided by the _formatting context_.

The value `input` corresponds to the time zone of the _operand_.
If it is used and the _resolved value_ of the _operand_ does not include a time zone or offset,
a _Bad Operand_ error is emitted and the default time zone is used to format the _expression_.

If the _resolved value_ of the _operand_ includes a time zone or offset,
and the _resolved value_ of the `timeZone` _option_ is different from that,
an implementation SHOULD convert the _resolved value_ of the _operand_
to the time zone indicated by the _resolved value_ of the `timeZone` _option_.
If such conversion is not supported, an implementation MAY alternatively
emit a _Bad Option_ error and use a _fallback value_ as the _resolved value_ of the _expression_.

The following _option_ is REQUIRED to be available on
the functions `:datetime` and `:time`:

- `hour12`
  - `true`
  - `false`

The following _option_ is RECOMMENDED to be available on
the functions `:datetime`, `:date`, and `:time`.

- `calendar`
  - valid [Unicode Calendar Identifier](tr35.md#UnicodeCalendarIdentifier)

