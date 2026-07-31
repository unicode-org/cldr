## <a name="Formatting" id="Formatting" href="#Formatting">Formatting</a>

This section defines the behavior of a MessageFormat implementation
when formatting a _message_ for display in a user interface, or for some later processing.

To start, we presume that a _message_ has either been parsed from its syntax
or created from a data model description.
If the resulting _message_ is not _well-formed_, a _Syntax Error_ is emitted.
If the resulting _message_ is _well-formed_ but is not _valid_, a _Data Model Error_ is emitted.

The formatting of a _message_ is defined by the following operations:

- **_<dfn>Pattern Selection</dfn>_** determines which of a message's _patterns_ is formatted.
  For a message with no _selectors_, this is simple as there is only one _pattern_.
  With _selectors_, this will depend on their resolution.

- **_<dfn>Formatting</dfn>_** takes the _resolved values_ of
  the _text_ and _placeholder_ parts of the selected _pattern_,
  and produces the formatted result for the _message_.
  Depending on the implementation, this result could be a single concatenated string,
  an array of objects, an attributed string, or some other locally appropriate data type.

- **_<dfn>Expression and Markup Resolution</dfn>_** determines the value of an _expression_ or _markup_,
  with reference to the current _formatting context_.
  This can include multiple steps,
  such as looking up the value of a variable and calling formatting functions.
  The form of the _resolved value_ is implementation defined and the
  value might not be evaluated or formatted yet.
  However, it needs to be "formattable", i.e. it contains everything required
  by the eventual formatting.

  The resolution of _text_ is rather straightforward,
  and is detailed under _literal resolution_.

Implementations are not required to expose
the _expression resolution_ and _pattern selection_ operations to their users,
or even use them in their internal processing,
as long as the final _formatting_ result is made available to users
and the observable behavior of the _formatting_ matches that described here.

_Attributes_ MUST NOT have any effect on the formatted output of a _message_,
nor be made available to _function handlers_.

> [!IMPORTANT]
>
> **This specification does not require either eager or lazy _expression resolution_ of _message_
> parts; do not construe any requirement in this document as requiring either.**
>
> Implementations are not required to evaluate all parts of a _message_ when
> parsing, processing, or formatting.
> In particular, an implementation MAY choose not to evaluate or resolve the
> value of a given _expression_ until it is actually used by a
> selection or formatting process.
> However, when an _expression_ is resolved, it MUST behave as if all preceding
> _declarations_ affecting _variables_ referenced by that _expression_
> have already been evaluated in the order in which the relevant _declarations_
> appear in the _message_.
> An implementation MUST ensure that every _expression_ in a _message_
> is evaluated at most once.

> [!IMPORTANT]
>
> Implementations with lazy evaluation MUST NOT use a
> call-by-name evaluation strategy. Instead, they must evaluate expressions
> at most once ("call-by-need").
> This is to prevent _expressions_ from having different values
> when used in different parts of a given _message_.
> _Function handlers_ are not necessarily pure: they can access
> external mutable state such as the current system clock time.
> Thus, evaluating the same _expression_ more than once
> could yield different results. That behavior violates this specification.

> [!IMPORTANT]
> Implementations and users SHOULD NOT create _function handlers_
> that mutate external program state,
> particularly since such a _function handler_ can present a remote execution hazard.
>

### <a name="Formatting_Context" id="Formatting_Context" href="#Formatting_Context">Formatting Context</a>

A _message_'s **_<dfn>formatting context</dfn>_** represents the data and procedures that are required
for the _message_'s _expression resolution_, _pattern selection_ and _formatting_.

At a minimum, it includes:

- Information on the current **_[locale](https://www.w3.org/TR/i18n-glossary/#dfn-locale)_**,
  potentially including a fallback chain of locales.
  This will be passed on to formatting functions.

- Information on the base directionality of the _message_ and its _text_ tokens.
  This will be used by strategies for bidirectional isolation,
  and can be used to set the base direction of the _message_ upon display.

- An **_<dfn>input mapping</dfn>_** of string identifiers to values,
  defining variable values that are available during _variable resolution_.
  This is often determined by a user-provided argument of a formatting function call.

- A mapping of string identifiers to the _function handlers_
  that are available during _function resolution_.

- Optionally, a fallback string to use for the _message_ if it is not _valid_.

Implementations MAY include additional fields in their _formatting context_.

### <a name="Resolved_Values" id="Resolved_Values" href="#Resolved_Values">Resolved Values</a>

A **_<dfn>resolved value</dfn>_** is the result of resolving a _text_, _literal_, _variable_, _expression_, or _markup_.
The _resolved value_ is determined using the _formatting context_.
The form of the _resolved value_ is implementation-defined.

In a _declaration_, the _resolved value_ of an _expression_ is bound to a _variable_,
which makes it available for use in later _expressions_ and _markup_ _options_.

> For example, in
> ```text
> .input {$a :number minimumFractionDigits=3}
> .local $b = {$a :integer useGrouping=never}
> .match $a
> 0 {{The value is zero.}}
> * {{Without grouping separators, the value {$a} is rendered as {$b}.}}
> ```
> the _resolved value_ bound to `$a` is used as the _operand_
> of the `:integer` _function_ when resolving the value of the _variable_ `$b`,
> as a _selector_ in the `.match` statement,
> as well as for formatting the _placeholder_ `{$a}`.

In an _input-declaration_, the _variable_ operand of the _variable-expression_
identifies not only the name of the external input value,
but also the _variable_ to which the _resolved value_ of the _variable-expression_ is bound.

In a _pattern_, the _resolved value_ of an _expression_ or _markup_ is used in its _formatting_.
To support the _Default Bidi Strategy_,
the _resolved value_ of each _expression_
SHOULD include information about the directionality
of its formatted string representation,
as well as a flag to indicate whether
its formatted representation requires isolation
from the surrounding text.
(See ["Handling Bidirectional Text"](#handling-bidirectional-text).)

For each _option value_, the _resolved value_ MUST indicate if the value
was directly set with a _literal_, as opposed to being resolved from a _variable_.
This is to allow _function handlers_ to require specific _options_ to be set using _literals_.

> For example, the _default functions_ `:number` and `:integer` require that the _option_
> `select` be set with a _literal_ _option value_ (`plural`, `ordinal`, or `exact`).

The form that _resolved values_ take is implementation-dependent,
and different implementations MAY choose to perform different levels of resolution.

> While this specification does not require it,
> a _resolved value_ could be implemented by requiring each _function handler_ to
> return a value matching the following interface:
>
> ```ts
> interface MessageValue {
>   formatToString(): string
>   formatToX(): X // where X is an implementation-defined type
>   unwrap(): unknown
>   resolvedOptions(): { [key: string]: MessageValue }
>   match(key: string): boolean
>   betterThan(key1: string, key2: string): boolean
>   directionality(): 'LTR' | 'RTL' | 'unknown'
>   isolate(): boolean
>   isLiteralOptionValue(): boolean
> }
> ```
>
> With this approach:
> - An _expression_ could be used as a _placeholder_ if
>   calling the `formatToString()` or `formatToX()` method of its _resolved value_
>   did not emit an error.
> - A _variable_ could be used as a _selector_ if
>   calling the `match(key)` and `betterThan(key1, key2)`  methods of its _resolved value_
>   did not emit an error.
> - The _resolved value_ of an _expression_
>   could be used as an _operand_ or _option value_ if
>   calling the `unwrap()` method of its _resolved value_ did not emit an error.
>   (This requires an intermediate _variable_ _declaration_.)
>   In this use case, the `resolvedOptions()` method could also
>   provide a set of option values that could be taken into account by the called function.
>   - The `unwrap()` method returns the _function_-specific result
>     of the _function_'s operation.
>     For example, the handlers for the following _functions_ might
>     behave as follows:
>     - The handler for the _default function_ `:number` returns a value
>       whose `unwrap()` method returns
>       the implementation-defined numeric value of the _operand_.
>     - The handler for a custom `:uppercase` _function_ might return a value
>       whose `unwrap()` method returns
>       an uppercase string in place of the original _operand_ value.
>     - The handler for a custom _function_ that extracts a field from a data structure
>       might return a value whose `unwrap()` method returns
>       the extracted value.
>     - Other _functions_' handlers might return a value
>       whose `unwrap()` method returns
>       the original _operand_ value.
> - The `directionality()`, `isolate()`, and `isLiteralOptionValue()` methods
>   fulfill requirements and recommendations mentioned elsewhere in this specification.
>
> Extensions of the base `MessageValue` interface could be provided for different data types,
> such as numbers or strings,
> for which the `unknown` return type of `unwrap()` and
> the generic `MessageValue` type used in `resolvedOptions()`
> could be narrowed appropriately.
> An implementation could also allow `MessageValue` values to be passed in as input variables,
> or automatically wrap each variable as a `MessageValue` to provide a uniform interface
> for custom functions.

### <a name="Expression_and_Markup_Resolution" id="Expression_and_Markup_Resolution" href="#Expression_and_Markup_Resolution">Expression and Markup Resolution</a>

_Expressions_ are used in _declarations_ and _patterns_.
_Markup_ is only used in _patterns_.
_Options_ are used in _expressions_ and _markup_.

#### <a name="Expression_Resolution" id="Expression_Resolution" href="#Expression_Resolution">Expression Resolution</a>

**_<dfn>Expression resolution</dfn>_** determines the value of an _expression_.
Depending on the presence or absence of a _variable_ or _literal_ operand and a _function_,
the _resolved value_ of the _expression_ is determined as follows:

If the _expression_ contains a _function_,
its _resolved value_ is defined by _function resolution_.

Else, if the _expression_ consists of a _variable_,
its _resolved value_ is defined by _variable resolution_.
An implementation MAY perform additional processing
when resolving the value of an _expression_
that consists only of a _variable_.

> For example, it could apply _function resolution_ using a _function_
> and a set of _options_ chosen based on the value or type of the _variable_.
> So, given a _message_ like this:
>
> ```text
> Today is {$date}
> ```
>
> If the value passed in the _variable_ were a date object,
> such as a JavaScript `Date` or a Java `java.util.Date` or `java.time.Temporal`,
> the implementation could interpret the _placeholder_ `{$date}` as if
> the pattern included the function `:datetime` with some set of default options.

Else, the _expression_ consists of a _literal_.
Its _resolved value_ is defined by _literal resolution_.

> [!NOTE]
> This means that a _literal_ value with no _function_
> is always treated as a string.
> To represent values that are not strings as a _literal_,
> a _function_ needs to be provided:
>
> ```text
> .local $aNumber = {1234 :number}
> .local $aDate = {|2023-08-30| :datetime}
> .local $aFoo = {|some foo| :ns:foo}
> {{You have {42 :number}}}
> ```

#### <a name="Literal_Resolution" id="Literal_Resolution" href="#Literal_Resolution">Literal Resolution</a>

**_<dfn>Literal resolution</dfn>_** : The _resolved value_ of a _text_ or a _literal_ contains
the character sequence of the _text_ or _literal_
after any character escape has been converted to the escaped character.

When a _literal_ is used as an _operand_ or as an _option value_,
the formatting function MUST treat its _resolved value_ the same
whether its value was originally a _quoted literal_ or an _unquoted literal_.

> For example,
> the _option_ `foo=42` and the _option_ `foo=|42|` are treated as identical.

> For example, in a JavaScript formatter,
> the _resolved value_ of a _text_ or a _literal_ could have the following implementation:
>
> ```ts
> class MessageLiteral implements MessageValue {
>   constructor(value: string) {
>     this.formatToString = () => value;
>     this.getValue = () => value;
>   }
>   resolvedOptions: () => ({});
>   match(_key: string) {
>     throw Error("Selection on unannotated literals is not supported");
>   }
> }
> ```

#### <a name="Variable_Resolution" id="Variable_Resolution" href="#Variable_Resolution">Variable Resolution</a>

**_<dfn>Variable resolution</dfn>_** : To resolve the value of a _variable_,
its _name_ is used to identify either a local variable or an input variable.
If a _declaration_ exists for the _variable_, its _resolved value_ is used.
Otherwise, the _variable_ is an implicit reference to an input value,
and its value is looked up from the _formatting context_ _input mapping_.

The resolution of a _variable_ fails if no value is identified for its _name_.
If this happens, an _Unresolved Variable_ error is emitted
and a _fallback value_ is used as the _resolved value_ of the _variable_.

If the _resolved value_ identified for the _variable_ _name_ is a _fallback value_,
a _fallback value_ is used as the _resolved value_ of the _variable_.

The _fallback value_ representation of a _variable_ has a string representation
consisting of the U+0024 DOLLAR SIGN `$` followed by the _name_ of the _variable_.

#### <a name="Function_Resolution" id="Function_Resolution" href="#Function_Resolution">Function Resolution</a>

**_<dfn>Function resolution</dfn>_** : To resolve an _expression_ with a _function_,
the following steps are taken:

1. If the _expression_ includes an _operand_, resolve its value.
   If this is a _fallback value_,
   return a _fallback value_ as the _resolved value_ of the _expression_.

2. Resolve the _identifier_ of the _function_ and
   find the appropriate _function handler_ to call.
   If the implementation cannot find the _function handler_,
   or if the _identifier_ includes a _namespace_ that the implementation does not support,
   emit an _Unknown Function_ error
   and return a _fallback value_ as the _resolved value_ of the _expression_.

   Implementations are not required to implement _namespaces_ or
   support _functions_ other than the _default functions_.

3. Perform _option resolution_.

4. Determine the _function context_ for calling the _function handler_.

   The **_<dfn>function context</dfn>_** contains the context necessary for
   the _function handler_ to resolve the _expression_. This includes:

   - The current _locale_,
     potentially including a fallback chain of locales.
   - The base directionality of the _expression_.
     By default, this is undefined or empty.

   If the resolved mapping of _options_ includes any _`u:` options_
   supported by the implementation, process them as specified.
   Such `u:` options MAY be removed from the resolved mapping of _options_.

5. Call the _function handler_ with the following arguments:

   - The _function context_.
   - The resolved mapping of _options_.
   - If the _expression_ includes an _operand_, its _resolved value_.

   The form that resolved _operand_ and _option values_ take is implementation-defined.

   An implementation MAY pass additional arguments to the _function handler_,
   as long as reasonable precautions are taken to keep the function interface
   simple and minimal, and avoid introducing potential security vulnerabilities.

6. If the call succeeds,
   resolve the value of the _expression_ as the result of that function call.
   The value MUST NOT be marked as a _literal_ _option value_.

   If the call fails or does not return a valid value,
   emit the appropriate _Message Function Error_ for the failure.

   Implementations MAY provide a mechanism for the _function handler_ to provide
   additional detail about internal failures.
   Specifically, if the cause of the failure was that the datatype, value, or format of the
   _operand_ did not match that expected by the _function_,
   the _function_ SHOULD cause a _Bad Operand_ error to be emitted.

   In all failure cases, return a _fallback value_ as the _resolved value_ of the _expression_.

###### <a name="Function_Handler" id="Function_Handler" href="#Function_Handler">Function Handler</a>

A **_<dfn>function handler</dfn>_** is an implementation-defined process
such as a function or method
which accepts a set of arguments and returns a _resolved value_.
A _function handler_ is required to resolve a _function_.

An implementation MAY define its own functions and their handlers.
An implementation MAY allow custom functions to be defined by users.

Implementations that provide a means for defining custom functions
MUST provide a means for _function handlers_
to return _resolved values_ that contain enough information
to be used as _operands_ or _option values_ in subsequent _expressions_.

The _resolved value_ returned by a _function handler_
MAY be different from the value of the _operand_ of the _function_.
It MAY be an implementation specified type.
It is not required to be the same type as the _operand_.

A _function handler_ MAY include resolved options in its _resolved value_.
The resolved options MAY be different from the _options_ of the function.

A _function handler_ SHOULD emit a
_Bad Operand_ error for _operands_ whose _resolved value_
or type is not supported.

_Function handler_ access to the _formatting context_ MUST be minimal and read-only,
and execution time SHOULD be limited.

Implementation-defined _functions_ SHOULD use an implementation-defined _namespace_.

#### <a name="Markup_Resolution" id="Markup_Resolution" href="#Markup_Resolution">Markup Resolution</a>

**_<dfn>Markup resolution</dfn>_** determines the value of _markup_.
Unlike _functions_, the resolution of _markup_ is not customizable.

The _resolved value_ of _markup_ includes the following fields:

- The type of the markup: open, standalone, or close
- The _identifier_ of the _markup_
- The resolved mapping of _options_ after _option resolution_.

If the resolved mapping of _options_ includes any _`u:` options_
supported by the implementation, process them as specified.
Such `u:` options MAY be removed from the resolved mapping of _options_.

The resolution of _markup_ MUST always succeed.
(Any errors emitted by _option resolution_
are non-fatal.)

#### <a name="Option_Resolution" id="Option_Resolution" href="#Option_Resolution">Option Resolution</a>

**_<dfn>Option resolution</dfn>_** is the process of computing the _options_
for a given _expression_ or _markup_.
_Option resolution_ results in a mapping of string _identifiers_ to _resolved values_.
The order of _options_ MUST NOT be significant.

> For example, the following _message_ treats both both placeholders identically:
> ```text
> {$x :ns:func option1=foo option2=bar} {$x :ns:func option2=bar option1=foo}
> ```

For each _option_:

1. Let `res` be a new empty mapping.
1. For each _option_:
   1. Let `id` be the string value of the _identifier_ of the _option_.
   1. Let `rv` be the _resolved value_ of the _option value_.
   1. If `rv` is a _fallback value_:
      1. Emit a _Bad Option_ error, if supported.
   1. Else:
      1. If the _option value_ consists of a _literal_:
         1. Mark `rv` as a _literal_ _option value_.
      1. Set `res[id]` to be `rv`.
1. Return `res`.

> [!NOTE]
> If the _resolved value_ of an _option value_ is a _fallback value_,
> the _option_ is intentionally omitted from the mapping of resolved options.

The result of _option resolution_ MUST be a (possibly empty) mapping
of string identifiers to values;
that is, errors MAY be emitted, but such errors MUST NOT be fatal.
This mapping can be empty.

> [!NOTE]
> The _resolved value_ of a _function_ _operand_
> can also include resolved option values.
> These are not included in the _option resolution_ result,
> and need to be processed separately by a _function handler_.

#### <a name="Fallback_Resolution" id="Fallback_Resolution" href="#Fallback_Resolution">Fallback Resolution</a>

A **_<dfn>fallback value</dfn>_** is the _resolved value_ for
an _expression_ or _variable_ when that _expression_ or _variable_ fails to resolve.
It contains a string representation that is used for its formatting.
All _options_ are removed.

The _resolved value_ of _text_, _literal_, and _markup_ MUST NOT be a _fallback value_.

A _variable_ fails to resolve when no value is identified for its _name_.
The string representation of its _fallback value_ is
U+0024 DOLLAR SIGN `$` followed by the _name_ of the _variable_.

An _expression_ fails to resolve when:

- A _variable_ used as its _operand_ resolves to a _fallback value_.
  Note that an _expression_ does not necessarily fail to resolve
  if an _option value_ resolves with a _fallback value_.
- No _function handler_ is found for a _function_ _identifier_.
- Calling a _function handler_ fails or does not return a valid value.

The string representation of the _fallback value_ of an _expression_ depends on its contents:

- _expression_ with a _literal_ _operand_ (either quoted or unquoted):
  U+007C VERTICAL LINE `|`
  followed by the value of the _literal_
  with escaping applied to U+005C REVERSE SOLIDUS `\` and U+007C VERTICAL LINE `|`,
  and then by U+007C VERTICAL LINE `|`.

  > Examples:
  > In a context where `:ns:func` fails to resolve,
  > `{42 :ns:func}` resolves to a _fallback value_ with a string representation `|42|` and
  > `{|C:\\| :ns:func}` resolves to a _fallback value_ with a string representation `|C:\\|`.

- _expression_ with _variable_ _operand_:
  the _fallback value_ representation of that _variable_,
  U+0024 DOLLAR SIGN `$` followed by the _name_ of the _variable_

  > Examples:
  > In a context where `$var` fails to resolve, `{$var}` and `{$var :number}`
  > both resolve to a _fallback value_ with a string representation `$var`
  > (even if `:number` fails to resolve).
  >
  > In a context where `:ns:func` fails to resolve,
  > the _placeholder_ in `.local $var = {|val| :ns:func} {{{$var}}}`
  > resolves to a _fallback value_ with a string representation `$var`.
  >
  > In a context where either `:ns:now` or `:ns:pretty` fails to resolve,
  > the _placeholder_ in
  > ```text
  > .local $time = {:ns:now format=iso8601}
  > {{{$time :ns:pretty}}}
  > ```
  > resolves to a _fallback value_ with a string representation `$time`.

- _function_ _expression_ with no _operand_:
  U+003A COLON `:` followed by the _function_ _identifier_

  > Example:
  > In a context where `:ns:func` fails to resolve,
  > `{:ns:func}` resolves to a _fallback value_ with a string representation `:ns:func`.

- Otherwise: the U+FFFD REPLACEMENT CHARACTER `�`

  This is not currently used by any expression, but may apply in future revisions.

_Options_ and _attributes_ are not included in the _fallback value_.

_Pattern selection_ is not supported for _fallback values_.

> For example, in a JavaScript formatter
> the _fallback value_ could have the following implementation,
> where `source` is one of the above-defined strings:
>
> ```ts
> class MessageFallback implements MessageValue {
>   constructor(source: string) {
>     this.formatToString = () => `{${source}}`;
>     this.getValue = () => undefined;
>   }
>   resolvedOptions: () => ({});
>   match(_key: string) {
>     throw Error("Selection on fallback values is not supported");
>   }
> }
> ```

### <a name="Pattern_Selection" id="Pattern_Selection" href="#Pattern_Selection">Pattern Selection</a>

If the _message_ being formatted is not _well-formed_ and _valid_,
the result of pattern selection is a _pattern_ consisting of a single _fallback value_
using the _message_'s fallback string defined in the _formatting context_
or if this is not available or empty, the U+FFFD REPLACEMENT CHARACTER `�`.

If the _message_ being formatted does not contain a _matcher_,
the result of pattern selection is its _pattern_ value.

When a _message_ contains a _matcher_ with one or more _selectors_,
the implementation needs to determine which _variant_ will be used
to provide the _pattern_ for the formatting operation.
This is done by traversing the list of available _variant_ statements
and maintaining a provisional "best variant". Each subsequent _variant_
is compared to the previous best variant according to its _key_ values,
yielding a single best variant.

> [!NOTE]
> At least one _variant_ is required to have all of its _keys_ consist of
> the fallback value `*`.
> Some _selectors_ might be implemented in a way that the key value `*`
> cannot be selected in a _valid_ _message_.
> In other cases, this key value might be unreachable only in certain locales.
> This could result in the need in some locales to create
> one or more _variants_ that do not make sense grammatically for that language.
> > For example, in the `pl` (Polish) locale, this _message_ cannot reach
> > the `*` _variant_:
> > ```text
> > .input {$num :integer}
> > .match $num
> > 0    {{ }}
> > one  {{ }}
> > few  {{ }}
> > many {{ }}
> > *    {{Only used by fractions in Polish.}}
> > ```

The number of _keys_ in each _variant_ MUST equal the number of _selectors_.

Each _key_ corresponds to a _selector_ by its position in the _variant_.

> For example, in this message:
>
> ```text
> .input {$one :number}
> .input {$two :number}
> .input {$three :number}
> .match $one $two $three
> 1 2 3 {{ ... }}
> ```
>
> The first _key_ `1` corresponds to the first _selector_ (`$one`),
> the second _key_ `2` to the second _selector_ (`$two`),
> and the third _key_ `3` to the third _selector_ (`$three`).

This selection method is defined in more detail below.
An implementation MAY use any pattern selection method,
as long as its observable behavior matches the results of the method defined here.

#### <a name="Operations_on_Resolved_Values" id="Operations_on_Resolved_Values" href="#Operations_on_Resolved_Values">Operations on Resolved Values</a>

For a _resolved value_ to support selection,
the operations Match and BetterThan need to be defined on it.

If `rv` is a resolved value that supports selection,
then Match(`rv`, `k`) returns true for any key `k` that matches `rv`
and returns false otherwise.
BetterThan(`rv`, `k1`, `k2`) returns true
for any keys `k1` and `k2` for which Match(`rv`, `k1`) is true,
Match(`rv`, `k2`) is true, and `k1` is a better match than `k2`,
and returns false otherwise.
On any error, both operations return false.

Other than the Match(`rv`, `k`) and BetterThan(`rv`, `k1`, `k2`) operations
on resolved values,
the form of the _resolved values_ is determined by each implementation,
along with the manner of determining their support for selection.

#### <a name="Resolve_Selectors" id="Resolve_Selectors" href="#Resolve_Selectors">Resolve Selectors</a>

First, resolve the values of each _selector_:

1. Let `res` be a new empty list of _resolved values_ that support selection.
1. For each _selector_ `sel`, in source order,
   1. Let `rv` be the _resolved value_ of `sel`.
   1. If selection is supported for `rv`:
      1. Append `rv` as the last element of the list `res`.
   1. Else:
      1. Let `nomatch` be a _resolved value_ for which Match(`rv`, `k`) is false
         for any _key_ `k`.
      1. Append `nomatch` as the last element of the list `res`.
      1. Emit a _Bad Selector_ error.

#### <a name="Compare_Variants" id="Compare_Variants" href="#Compare_Variants">Compare Variants</a>

Next, using `res`:

1. Let `bestVariant` be `UNSET`.
1. For each _variant_ `var` of the message, in source order:
   1. Let `keys` be the _keys_ of `var`.
   1. Let `match` be SelectorsMatch(`res`, `keys`).
   1. If `match` is false:
      1. Continue the loop.
   1. If `bestVariant` is `UNSET`.
      1. Set `bestVariant` to `var`.
   1. Else:
      1. Let `bestVariantKeys` be the _keys_ of `bestVariant`.
      1. If SelectorsCompare(`res`, `keys`, `bestVariantKeys`) is true:
         1. Set `bestVariant` to `var`.
1. Assert that `bestVariant` is not `UNSET`.
1. Select the _pattern_ of `bestVariant`.

#### <a name="SelectorsMatch" id="SelectorsMatch" href="#SelectorsMatch">SelectorsMatch</a>

SelectorsMatch(`selectors`, `keys`) is defined as follows, where
`selectors` is a list of _resolved values_
and `keys` is a list of _keys_:

1. Let `i` be 0.
1. For each _key_ `key` in `keys`:
   1. If `key` is not the catch-all key `'*'`
      1. Let `k` be NormalizeKey(`key`).
      1. Let `sel` be the `i`th element of `selectors`.
      1. If Match(`sel`, `k`) is false:
         1. Return false.
   1. Set `i` to `i` + 1.
1. Return true.

#### <a name="SelectorsCompare" id="SelectorsCompare" href="#SelectorsCompare">SelectorsCompare</a>

SelectorsCompare(`selectors`, `keys1`, `keys2`) is defined as follows, where
`selectors` is a list of _resolved values_
and `keys1` and `keys2` are lists of _keys_.

1. Let `i` be 0.
1. For each _key_ `key1` in `keys1`:
   1. Let `key2` be the `i`th element of `keys2`.
   1. If `key1` is the catch-all _key_ `'*'` and `key2` is not the catch-all _key_:
      1. Return false.
   1. If `key1` is not the catch-all _key_ `'*'` and `key2` is the catch-all _key_:
      1. Return true.
   1. If `key1` and `key2` are both the catch-all _key_ `'*'`
      1. Set `i` to `i + 1`.
      1. Continue the loop.
   1. Let `k1` be NormalizeKey(`key1`).
   1. Let `k2` be NormalizeKey(`key2`).
   1. If `k1` and `k2` consist of the same sequence of Unicode code points, then:
      1. Set `i` to `i + 1`.
      1. Continue the loop.
   1. Let `sel` be the `i`th element of `selectors`.
   1. Let `result` be BetterThan(`sel`, `k1`, `k2`).
   1. Return `result`.
1. Return false.

#### <a name="NormalizeKey" id="NormalizeKey" href="#NormalizeKey">NormalizeKey</a>

NormalizeKey(`key`) is defined as follows, where
`key` is a _key_.

1. Let `rv` be the _resolved value_ of `key` (see [Literal Resolution](#literal-resolution).)
1. Let `k` be the string value of `rv`.
1. Let `k1` be the result of applying Unicode Normalization Form C [\[UAX#15\]](https://www.unicode.org/reports/tr15) to `k`.
1. Return `k1`.

For examples of how the algorithms work, see [the appendix](#non-normative-examples).

### <a name="Formatting_of_the_Selected_Pattern" id="Formatting_of_the_Selected_Pattern" href="#Formatting_of_the_Selected_Pattern">Formatting of the Selected Pattern</a>

After _pattern selection_,
each _text_ and _placeholder_ part of the selected _pattern_ is resolved and formatted.

_Resolved values_ cannot always be formatted by a given implementation.
When such an error occurs during _formatting_,
an appropriate _Message Function Error_ is emitted and
a _fallback value_ is used for the _placeholder_ with the error.

Implementations MAY represent the result of _formatting_ using the most
appropriate data type or structure. Some examples of these include:

- A single string concatenated from the parts of the resolved _pattern_.
- A string with associated attributes for portions of its text.
- A flat sequence of objects corresponding to each _resolved value_.
- A hierarchical structure of objects that group spans of _resolved values_,
  such as sequences delimited by _markup-open_ and _markup-close_ _placeholders_.

Implementations SHOULD provide _formatting_ result types that match user needs,
including situations that require further processing of formatted messages.
Implementations SHOULD encourage users to consider a formatted localised string
as an opaque data structure, suitable only for presentation.

When formatting to a string, the default representation of all _markup_
MUST be an empty string.
Implementations MAY offer functionality for customizing this,
such as by emitting XML-ish tags for each _markup_.

#### <a name="Formatting_Examples" id="Formatting_Examples" href="#Formatting_Examples">Formatting Examples</a>

_This section is non-normative._

1. An implementation might choose to return an interstitial object
   so that the caller can "decorate" portions of the formatted value.
   In ICU4J, the `NumberFormatter` class returns a `FormattedNumber` object,
   so a _pattern_ such as `This is my number {42 :number}` might return
   the character sequence `This is my number `
   followed by a `FormattedNumber` object representing the value `42` in the current locale.

2. A formatter in a web browser could format a message as a DOM fragment
   rather than as a representation of its HTML source.

#### <a name="Formatting_Fallback_Values" id="Formatting_Fallback_Values" href="#Formatting_Fallback_Values">Formatting Fallback Values</a>

If the resolved _pattern_ includes any _fallback values_
and the formatting result is a concatenated string or a sequence of strings,
the string representation of each _fallback value_ MUST be the concatenation of
a U+007B LEFT CURLY BRACKET `{`,
the _fallback value_ as a string,
and a U+007D RIGHT CURLY BRACKET `}`.

> For example,
> a _message_ that is not _well-formed_ would format to a string as `{�}`,
> unless a fallback string is defined in the _formatting context_,
> in which case that string would be used instead.

#### <a name="Handling_Bidirectional_Text" id="Handling_Bidirectional_Text" href="#Handling_Bidirectional_Text">Handling Bidirectional Text</a>

_Messages_ contain text. Any text can be
[bidirectional text](https://www.w3.org/TR/i18n-glossary/#dfn-bidirectional-text).
That is, the text can can consist of a mixture of left-to-right and right-to-left spans of text.
The display of bidirectional text is defined by the
[Unicode Bidirectional Algorithm](http://www.unicode.org/reports/tr9/) [UAX9].

The directionality of the formatted _message_ as a whole is provided by the _formatting context_.

> [!NOTE]
> Keep in mind the difference between the formatted output of a _message_,
> which is the topic of this section,
> and the syntax of _message_ prior to formatting.
> The processing of a _message_ depends on the logical sequence of Unicode code points,
> not on the presentation of the _message_.
> Affordances to allow users appropriate control over the appearance of the
> _message_'s syntax have been provided.

When a _message_ is formatted, _placeholders_ are replaced
with their formatted representation.
Applying the Unicode Bidirectional Algorithm to the text of a formatted _message_
(including its formatted parts)
can result in unexpected or undesirable
[spillover effects](https://www.w3.org/TR/i18n-glossary/#dfn-spillover-effects).
Applying [bidi isolation](https://www.w3.org/TR/i18n-glossary/#dfn-bidi-isolation)
to each affected formatted value helps avoid this spillover in a formatted _message_.

Note that both the _message_ and, separately, each _placeholder_ need to have
direction metadata for this to work.
If an implementation supports formatting to something other than a string
(such as a sequence of parts),
the directionality of each formatted _placeholder_ needs to be available to the caller.

If a formatted _expression_ itself contains spans with differing directionality,
its formatter SHOULD perform any necessary processing, such as inserting controls or
isolating such parts to ensure that the formatted value displays correctly in a plain text context.

> For example, an implementation could provide a `:currency` formatting function
> which inserts strongly directional characters, such as U+200F RIGHT-TO-LEFT MARK (RLM),
> U+200E LEFT-TO-RIGHT MARK (LRM), or U+061C ARABIC LETTER MARKER (ALM),
> to coerce proper display of the sign and currency symbol next to a formatted number.
> An example of this is formatting the value `-1234.56` as the currency `AED`
> in the `ar-AE` locale. The formatted value appears like this:
> ```text
> ‎-1,234.56 د.إ.‏
> ```
> The code point sequence for this string, as produced by the ICU4J `NumberFormat` function,
> includes **U+200F U+200E** at the start and **U+200F** at the end of the string.
> If it did not do this, the same string would appear like this instead:
>
> ![image](https://github.com/unicode-org/message-format-wg/assets/69082/6cc7f16f-8d9b-400b-a333-ae2ddb316edb)

A **_<dfn>bidirectional isolation strategy<dfn>_** is functionality in the formatter's
processing of a _message_ that produces bidirectional output text that is ready for display.

The **_<dfn>Default Bidi Strategy<dfn>_** is a _bidirectional isolation strategy_ that uses
isolating Unicode control characters around _placeholder_'s formatted values.
It is primarily intended for use in plain-text strings, where markup or other mechanisms
are not available.
The _Default Bidi Strategy_ MUST be the default _bidirectional isolation strategy_
when formatting a _message_ as a single string.

Implementations MAY provide other _bidirectional isolation strategies_.

Implementations MAY supply a _bidirectional isolation strategy_ that performs no processing.

The _Default Bidi Strategy_ is defined as follows:

1. Let `out` be the empty string.
1. Let `msgdir` be the directionality of the whole message,
   one of « `'LTR'`, `'RTL'`, `'unknown'` ».
   These correspond to the message having left-to-right directionality,
   right-to-left directionality, and to the message's directionality not being known.
1. For each part `part` in _pattern_:
   1. If `part` is a plain literal (text) part, append `part` to `out`.
   1. Else if `part` is a _markup_ _placeholder_:
      1. Let `fmt` be the formatted string representation of the _resolved value_ of `part`.
         Note that this is normally the empty string.
      1. Append `fmt` to `out`.
   1. Else:
      1. Let `resval` be the _resolved value_ of `part`.
      1. Let `fmt` be the formatted string representation of `resval`.
      1. Let `dir` be the directionality of `resval`,
         one of « `'LTR'`, `'RTL'`, `'unknown'` », with the same meanings as for `msgdir`.
      1. Let the boolean value `isolate` be
         True if the `u:dir` _option_ of `resval` has a value other than `'inherit'`,
          or False otherwise.
      1. If `dir` is `'LTR'`:
         1. If `msgdir` is `'LTR'` and `isolate` is False:
            1. Append `fmt` to `out`.
         1. Else:
            1. Append U+2066 LEFT-TO-RIGHT ISOLATE to `out`.
            1. Append `fmt` to `out`.
            1. Append U+2069 POP DIRECTIONAL ISOLATE to `out`.
      1. Else if `dir` is `'RTL'`:
         1. Append U+2067 RIGHT-TO-LEFT ISOLATE to `out.`
         1. Append `fmt` to `out`.
         1. Append U+2069 POP DIRECTIONAL ISOLATE to `out`.
      1. Else:
         1. Append U+2068 FIRST STRONG ISOLATE to `out`.
         1. Append `fmt` to `out`.
         1. Append U+2069 POP DIRECTIONAL ISOLATE to `out`.
1. Emit `out` as the formatted output of the message.

> [!NOTE]
> As mentioned in the "Resolved Values" section,
> the representation of a _resolved value_
> can track everything needed
> to determine the directionality
> of the formatted string representation
> of a _resolved value_.
> Each _function handler_ can have its own means
> for determining the directionality annotation
> on the _resolved value_ it returns.
> Alternately, an implementation could simply
> determine directionality
> based on the locale.

> [!IMPORTANT]
> Directionality SHOULD NOT be determined by introspecting
> the character sequence in the formatted string representation
> of `resval`.

