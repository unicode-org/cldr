## <a name="Unicode_Namespace" id="Unicode_Namespace" href="#Unicode_Namespace">Unicode Namespace</a>

The `u:` _namespace_ is reserved for the definition of _options_
which affect the _function context_ of the specific _expressions_
in which they appear,
or for the definition of _options_ that are universally applicable
rather than function-specific.
It might also be used to define _functions_ in a future release.

The CLDR Technical Committee of the Unicode Consortium
manages the specification for this namespace, hence the _namespace_ `u:`.

### <a name="Unicode_Namespace_Options" id="Unicode_Namespace_Options" href="#Unicode_Namespace_Options">Unicode Namespace Options</a>

This section describes **_<dfn>`u:` options</dfn>_**.
When implemented, they apply to all _functions_ and _markup_,
including user-defined _functions_ in that implementation.

#### <a name="uid" id="uid" href="#uid">`u:id`</a>

Implementations providing a formatting target other than a concatenated string
SHOULD support this option.

A string value that is included as an `id` or other suitable value
in the formatted parts for the _placeholder_,
or any other structured formatted results.

> For example, `u:id` could be used to distinguish
> two otherwise matching placeholders from each other:
>
> ```text
> The first number was {$a :number u:id=first} and the second {$b :number u:id=second}.
> ```

Ignored when formatting a message to a string.

The `u:id` _option value_ MUST be a _literal_ or a
_variable_ whose _resolved value_ is either a string
or can be resolved to a string without error.
For other values, a _Bad Option_ error is emitted
and the `u:id` _option_ and its _option value_ are ignored.

#### <a name="udir" id="udir" href="#udir">`u:dir`</a>

Implementations SHOULD support this option.

Replaces the base directionality defined in
the _function context_ for this _expression_
and applies bidirectional isolation to it.

If this _option_ is set on _markup_, a _Bad Option_ error is emitted
and the `u:dir` _option_ and its _option value_ are ignored.

During processing, the `u:dir` _option_
MUST be removed from the resolved mapping of _options_
before calling the _function handler_.
Its value is retained in the _resolved value_ of the _expression_.

The `u:dir` _option value_ MUST be one of the following _literal_ values
or a _variable_ whose _resolved value_ is one of the following strings:
- `ltr`: left-to-right directionality
- `rtl`: right-to-left directionality
- `auto`: directionality determined from _expression_ contents
- `inherit` (default): directionality inherited from the _message_
   or from the _resolved value_ of the _operand_ without
   requiring isolation of the _expression_ value.

For other values, a _Bad Option_ error is emitted
and the `u:dir` _option_ and its _option value_ are ignored.

