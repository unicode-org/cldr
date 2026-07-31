## <a name="Syntax" id="Syntax" href="#Syntax">Syntax</a>

This section defines the formal grammar describing the syntax of a single message.

### <a name="Design_Goals" id="Design_Goals" href="#Design_Goals">Design Goals</a>

_This section is non-normative._

The design goals of the syntax specification are as follows:

1. The syntax should leverage the familiarity with ICU MessageFormat 1.0
   in order to lower the barrier to entry and increase the chance of adoption.
   At the same time,
   the syntax should fix the pain points of ICU MessageFormat 1.0.

   - _Non-Goal_: Be backwards-compatible with the ICU MessageFormat 1.0 syntax.

1. The syntax inside translatable content should be easy to understand for humans.
   This includes making it clear which parts of the message body _are_ translatable content,
   which parts inside it are placeholders for expressions,
   as well as making the selection logic predictable and easy to reason about.

   - _Non-Goal_: Make the syntax intuitive enough for non-technical translators to hand-edit.
     Instead, we assume that most translators will work with MessageFormat
     by means of GUI tooling, CAT workbenches etc.

1. The syntax surrounding translatable content should be easy to write and edit
   for developers, localization engineers, and easy to parse by machines.

1. The syntax should make a single message easily embeddable inside many container formats:
   `.properties`, YAML, XML, inlined as string literals in programming languages, etc.
   This includes a future _MessageResource_ specification.

   - _Non-Goal_: Support unnecessary escape sequences, which would theirselves require
     additional escaping when embedded. Instead, we tolerate direct use of nearly all
     characters (including line breaks, control characters, etc.) and rely upon escaping
     in those outer formats to aid human comprehension (e.g., depending upon container
     format, a U+000A LINE FEED might be represented as `\n`, `\012`, `\x0A`, `\u000A`,
     `\U0000000A`, `&#xA;`, `&NewLine;`, `%0A`, `<LF>`, or something else entirely).

### <a name="Design_Restrictions" id="Design_Restrictions" href="#Design_Restrictions">Design Restrictions</a>

_This section is non-normative._

The syntax specification takes into account the following design restrictions:

1. Whitespace outside the translatable content should be insignificant.
   It should be possible to define a message entirely on a single line with no ambiguity,
   as well as to format it over multiple lines for clarity.

1. The syntax should define as few special characters and sigils as possible.
   Note that this necessitates extra care when presenting messages for human consumption,
   because they may contain invisible characters such as U+200B ZERO WIDTH SPACE,
   control characters such as U+0000 NULL and U+0009 TAB, permanently reserved noncharacters
   (U+FDD0 through U+FDEF and U+<i>n</i>FFFE and U+<i>n</i>FFFF where <i>n</i> is 0x0 through 0x10),
   private-use code points (U+E000 through U+F8FF, U+F0000 through U+FFFFD, and
   U+100000 through U+10FFFD), unassigned code points, unpaired surrogates (U+D800 through U+DFFF),
   and other potentially confusing content.

### <a name="Messages_and_their_Syntax" id="Messages_and_their_Syntax" href="#Messages_and_their_Syntax">Messages and their Syntax</a>

The purpose of MessageFormat is to allow content to vary at runtime.
This variation might be due to placing a value into the content
or it might be due to selecting a different bit of content based on some data value
or it might be due to a combination of the two.

MessageFormat calls the template for a given formatting operation a _message_.

The values passed in at runtime (which are to be placed into the content or used
to select between different content items) are called _external variables_.
The author of a _message_ can also assign _local variables_, including
variables that modify _external variables_.

This part of the MessageFormat specification defines the syntax for a _message_,
along with the concepts and terminology needed when processing a _message_
during the [formatting](#formatting) of a _message_ at runtime.

The complete formal syntax of a _message_ is described by the [ABNF](#messageabnf).

#### <a name="Wellformed_vs_Valid_Messages" id="Wellformed_vs_Valid_Messages" href="#Wellformed_vs_Valid_Messages">Well-formed vs. Valid Messages</a>

A _message_ is **_<dfn>well-formed</dfn>_** if it satisfies all the rules of the grammar.
Attempting to parse a _message_ that is not _well-formed_ will result in a _Syntax Error_.

A _message_ is **_<dfn>valid</dfn>_** if it is _well-formed_ and
**also** meets the additional content restrictions
and semantic requirements about its structure defined below for
_declarations_, _matcher_, and _options_.
Attempting to parse a _message_ that is not _valid_ will result in a _Data Model Error_.

### <a name="The_Message" id="The_Message" href="#The_Message">The Message</a>

A **_<dfn>message</dfn>_** is the complete template for a specific message formatting request.

A **_<dfn>variable</dfn>_** is a _name_ associated to a _resolved value_.

An **_<dfn>external variable</dfn>_** is a _variable_
whose _name_ and initial value are supplied by the caller
to MessageFormat or available in the _formatting context_.
Only an _external variable_ can appear as an _operand_ in an _input declaration_.

A **_<dfn>local variable</dfn>_** is a _variable_ created as the result of a _local declaration_.

> [!NOTE]
> This syntax is designed to be embeddable into many different programming languages and formats.
> As such, it avoids constructs, such as character escapes, that are specific to any given file
> format or processor.
> In particular, it avoids using quote characters common to many file formats and formal languages
> so that these do not need to be escaped in the body of a _message_.

> [!NOTE]
> _Text_ and _quoted literals_ allow unpaired surrogate code points
> (`U+D800` to `U+DFFF`).
> This is for compatibility with formats or data structures
> that use the UTF-16 encoding
> and do not check for unpaired surrogates.
> (Strings in Java or JavaScript are examples of this.)
> Unpaired surrogate code points are likely an indication of mistakes
> or errors in the creation, serialization, or processing of the _message_.
> Many processes will convert them to
> &#xfffd; U+FFFD REPLACEMENT CHARACTER
> during processing or display.
> Implementations not based on UTF-16 might not be able to represent
> a _message_ containing such code points.

> [!NOTE]
> In general (and except where required by the syntax), whitespace carries no meaning in the structure
> of a _message_. While many of the examples in this spec are written on multiple lines, the formatting
> shown is primarily for readability.
>
> > **Example** This _message_:
> >
> > ```text
> > .local $foo   =   { |horse| }
> > {{You have a {$foo}!}}
> > ```
> >
> > Can also be written as:
> >
> > ```text
> > .local $foo={|horse|}{{You have a {$foo}!}}
> > ```
> >
> > An exception to this is: whitespace inside a _pattern_ is **always** significant.

> [!NOTE]
> The MessageFormat syntax assumes that each _message_ will be displayed
> with a left-to-right display order
> and be processed in the logical character order.
> The syntax permits the use of right-to-left characters in _identifiers_,
> _literals_, and other values.
> This can result in confusion when viewing the message
> or users might incorrectly insert bidi controls or marks that negatively affect the output
> of the message.
>
> To assist with this, the syntax permits the use of various controls and
> strongly-directional markers in both optional and required _whitespace_
> in a _message_, as well was encouraging the use of isolating controls
> with _expressions_ and _quoted patterns_.
> See: [whitespace](#whitespace) (below) for more information.

A _message_ can be a _simple message_ or it can be a _complex message_.

```abnf
message = simple-message / complex-message
```

A **_<dfn>simple message</dfn>_** contains a single _pattern_,
with restrictions on its first non-whitespace character.
An empty string is a _valid_ _simple message_.

Whitespace at the start or end of a _simple message_ is significant,
and a part of the _text_ of the _message_.

```abnf
simple-message = o [simple-start pattern]
simple-start   = simple-start-char / escaped-char / placeholder
```

A **_<dfn>complex message</dfn>_** is any _message_ that contains _declarations_,
a _matcher_, or both.
A _complex message_ always begins with either a keyword that has a `.` prefix or a _quoted pattern_
and consists of:

1. an optional list of _declarations_, followed by
2. a _complex body_

Whitespace at the start or end of a _complex message_ is not significant,
and does not affect the processing of the _message_.

```abnf
complex-message = o *(declaration o) complex-body o
```

#### <a name="Declarations" id="Declarations" href="#Declarations">Declarations</a>

A **_<dfn>declaration</dfn>_** binds a _variable_ identifier to a value within the scope of a _message_.
This _variable_ can then be used in other _expressions_ within the same _message_.
_Declarations_ are optional: many messages will not contain any _declarations_.

An **_<dfn>input-declaration</dfn>_** binds a _variable_ to an external input value.
The _variable-expression_ of an _input-declaration_
MAY include a _function_ that is applied to the external value.

A **_<dfn>local-declaration</dfn>_** binds a _variable_ to the _resolved value_ of an _expression_.

```abnf
declaration       = input-declaration / local-declaration
input-declaration = input o variable-expression
local-declaration = local s variable o "=" o expression
```

_Variables_, once declared, MUST NOT be redeclared.
A _message_ that does any of the following is not _valid_ and will produce a
_Duplicate Declaration_ error during processing:
- A _declaration_ MUST NOT bind a _variable_
  that appears as a _variable_ anywhere within a previous _declaration_.
- An _input-declaration_ MUST NOT bind a _variable_
  that appears anywhere within the _function_ of its _variable-expression_.
- A _local-declaration_ MUST NOT bind a _variable_ that appears in its _expression_.

A _local-declaration_ MAY overwrite an external input value as long as the
external input value does not appear in a previous _declaration_.

> [!NOTE]
> These restrictions only apply to _declarations_.
> A _placeholder_ can apply a different _function_ to a _variable_
> than one applied to the same _variable_ named in a _declaration_.
> For example, this message is _valid_:
> ```text
> .input {$var :number maximumFractionDigits=0}
> .local $var2 = {$var :number maximumFractionDigits=2}
> .match $var2
> 0 {{The selector can apply a different function to {$var} for the purposes of selection}}
> * {{A placeholder in a pattern can apply a different function to {$var :number maximumFractionDigits=3}}}
> ```
> (See the [Errors](#errors) section for examples of invalid messages)

#### <a name="Complex_Body" id="Complex_Body" href="#Complex_Body">Complex Body</a>

The **_<dfn>complex body</dfn>_** of a _complex message_ is the part that will be formatted.
The _complex body_ consists of either a _quoted pattern_ or a _matcher_.

```abnf
complex-body = quoted-pattern / matcher
```

### <a name="Pattern" id="Pattern" href="#Pattern">Pattern</a>

A **_<dfn>pattern</dfn>_** contains a sequence of _text_ and _placeholders_ to be formatted as a unit.
Unless there is an error, resolving a _message_ always results in the formatting
of a single _pattern_.

```abnf
pattern = *(text-char / escaped-char / placeholder)
```
A _pattern_ MAY be empty.

A _pattern_ MAY contain an arbitrary number of _placeholders_ to be evaluated
during the formatting process.

#### <a name="Quoted_Pattern" id="Quoted_Pattern" href="#Quoted_Pattern">Quoted Pattern</a>

A **_<dfn>quoted pattern</dfn>_** is a _pattern_ that is "quoted" to prevent
interference with other parts of the _message_.
A _quoted pattern_ starts with a sequence of two U+007B LEFT CURLY BRACKET `{{`
and ends with a sequence of two U+007D RIGHT CURLY BRACKET `}}`.

```abnf
quoted-pattern = "{{" pattern "}}"
```

A _quoted pattern_ MAY be empty.

> An empty _quoted pattern_:
>
> ```text
> {{}}
> ```

#### <a name="Text" id="Text" href="#Text">Text</a>

**_<dfn>text</dfn>_** is the translateable content of a _pattern_.
Any Unicode code point is allowed, except for U+0000 NULL.

The characters U+005C REVERSE SOLIDUS `\`,
U+007B LEFT CURLY BRACKET `{`, and U+007D RIGHT CURLY BRACKET `}`
MUST be escaped as `\\`, `\{`, and `\}` respectively.

In the ABNF, _text_ is represented by non-empty sequences of
`simple-start-char`, `text-char`, `escaped-char`, and `s`.
The production `simple-start-char` represents the first non-whitespace in a _simple message_
and matches `text-char` except for not allowing U+002E FULL STOP `.`.

Whitespace in _text_, including tabs, spaces, and newlines is significant and MUST
be preserved during formatting.

```abnf
simple-start-char = %x01-08        ; omit NULL (%x00), HTAB (%x09) and LF (%x0A)
                  / %x0B-0C        ; omit CR (%x0D)
                  / %x0E-1F        ; omit SP (%x20)
                  / %x21-2D        ; omit . (%x2E)
                  / %x2F-5B        ; omit \ (%x5C)
                  / %x5D-7A        ; omit { (%x7B)
                  / %x7C           ; omit } (%x7D)
                  / %x7E-2FFF      ; omit IDEOGRAPHIC SPACE (%x3000)
                  / %x3001-10FFFF
text-char         = %x01-5B        ; omit NULL (%x00) and \ (%x5C)
                  / %x5D-7A        ; omit { (%x7B)
                  / %x7C           ; omit } (%x7D)
                  / %x7E-10FFFF
quoted-char       = %x01-5B        ; omit NULL (%x00) and \ (%x5C)
                  / %x5D-7B        ; omit | (%x7C)
                  / %x7D-10FFFF
```

> [!NOTE]
> Unpaired surrogate code points (`U+D800` through `U+DFFF` inclusive)
> are allowed for compatibility with UTF-16 based implementations
> that do not check for this encoding error.

When a _pattern_ is quoted by embedding the _pattern_ in curly brackets, the
resulting _message_ can be embedded into
various formats regardless of the container's whitespace trimming rules.
Otherwise, care must be taken to ensure that pattern-significant whitespace is preserved.

> **Example**
> In a Java `.properties` file, the values `hello` and `hello2` both contain
> an identical _message_ which consists of a single _pattern_.
> This _pattern_ consists of _text_ with exactly three spaces before and after the word "Hello":
>
> ```text
> hello = {{   Hello   }}
> hello2=\   Hello  \
> ```

#### <a name="Placeholder" id="Placeholder" href="#Placeholder">Placeholder</a>

A **_<dfn>placeholder</dfn>_** is an _expression_ or _markup_ that appears inside of a _pattern_
and which will be replaced during the formatting of a _message_.

```abnf
placeholder = expression / markup
```

### <a name="Matcher" id="Matcher" href="#Matcher">Matcher</a>

A **_<dfn>matcher</dfn>_** is the _complex body_ of a _message_ that allows runtime selection
of the _pattern_ to use for formatting.
This allows the form or content of a _message_ to vary based on values
determined at runtime.

A _matcher_ consists of the keyword `.match` followed by at least one _selector_
and at least one _variant_.

When the _matcher_ is processed, the result will be a single _pattern_ that serves
as the template for the formatting process.

A _message_ can only be considered _valid_ if the following requirements are satisfied;
otherwise, a corresponding _Data Model Error_ will be produced during processing:

- _Variant Key Mismatch_:
  The number of _keys_ on each _variant_ MUST be equal to the number of _selectors_.
- _Missing Fallback Variant_:
  At least one _variant_ MUST exist whose _keys_ are all equal to the "catch-all" key `*`.
- _Missing Selector Annotation_:
  Each _selector_ MUST be a _variable_ that
  directly or indirectly references a _declaration_ with a _function_.
- _Duplicate Variant_:
  Each _variant_ MUST use a list of _keys_ that is unique from that
  of all other _variants_ in the _message_.
  _Literal_ _keys_ are compared by their _string values_, not their syntactical appearance.

```abnf
matcher         = match-statement s variant *(o variant)
match-statement = match 1*(s selector)
```

> A _message_ with a _matcher_:
>
> ```text
> .input {$count :number}
> .match $count
> one {{You have {$count} notification.}}
> *   {{You have {$count} notifications.}}
> ```

> A _message_ containing a _matcher_ formatted on a single line:
>
> ```text
> .local $os = {:platform} .match $os windows {{Settings}} * {{Preferences}}
> ```

#### <a name="Selector" id="Selector" href="#Selector">Selector</a>

A **_<dfn>selector</dfn>_** is a _variable_ whose _resolved value_ ranks or excludes the
_variants_ based on the value of the corresponding _key_ in each _variant_.
The combination of _selectors_ in a _matcher_ thus determines
which _pattern_ will be used during formatting.

```abnf
selector = variable
```

There MUST be at least one _selector_ in a _matcher_.
There MAY be any number of additional _selectors_.

> A _message_ with a single _selector_ that uses a custom _function_
> `:ns:hasCase` which is a _selector_ that allows the _message_ to choose a _pattern_
> based on grammatical case:
>
> ```text
> .local $hasCase = {$userName :ns:hasCase}
> .match $hasCase
> vocative {{Hello, {$userName :ns:person case=vocative}!}}
> accusative {{Please welcome {$userName :ns:person case=accusative}!}}
> * {{Hello!}}
> ```

> A message with two _selectors_:
>
> ```text
> .input {$numLikes :integer}
> .input {$numShares :integer}
> .match $numLikes $numShares
> 0   0   {{Your item has no likes and has not been shared.}}
> 0   one {{Your item has no likes and has been shared {$numShares} time.}}
> 0   *   {{Your item has no likes and has been shared {$numShares} times.}}
> one 0   {{Your item has {$numLikes} like and has not been shared.}}
> one one {{Your item has {$numLikes} like and has been shared {$numShares} time.}}
> one *   {{Your item has {$numLikes} like and has been shared {$numShares} times.}}
> *   0   {{Your item has {$numLikes} likes and has not been shared.}}
> *   one {{Your item has {$numLikes} likes and has been shared {$numShares} time.}}
> *   *   {{Your item has {$numLikes} likes and has been shared {$numShares} times.}}
> ```

#### <a name="Variant" id="Variant" href="#Variant">Variant</a>

A **_<dfn>variant</dfn>_** is a _quoted pattern_ associated with a list of _keys_ in a _matcher_.
Each _variant_ MUST begin with a sequence of _keys_,
and terminate with a _valid_ _quoted pattern_.
The number of _keys_ in each _variant_ MUST match the number of _selectors_ in the _matcher_.

Each _key_ is separated from each other by whitespace.
Whitespace is permitted but not required between the last _key_ and the _quoted pattern_.

```abnf
variant = key *(s key) o quoted-pattern
key     = literal / "*"
```

#### <a name="Key" id="Key" href="#Key">Key</a>

A **_<dfn>key</dfn>_** is a value in a _variant_ for use by a _selector_ when ranking
or excluding _variants_ during the _matcher_ process.
A _key_ can be either a _literal_ value or the "catch-all" key `*`.

The **_<dfn>catch-all key</dfn>_** is a special key, represented by `*`,
that matches all values for a given _selector_.

> [!NOTE]
> To represent a _key_ consisting of the character `*` U+002A ASTERISK,
> use a _quoted literal_:
> ```text
> .input {$value :string}
> .match $value
> |*| {{Matches the string *}}
> *   {{Matches any other string}}
> ```

The value of each _literal_ _key_ MUST be treated as if it were in
[Unicode Normalization Form C](https://unicode.org/reports/tr15/) ("NFC").
Two _literal_ _keys_ are considered equal if their _string values_ are canonically equivalent strings,
that is, if they consist of the same sequence of Unicode code points after
Unicode Normalization Form C has been applied to both.

### <a name="Expressions" id="Expressions" href="#Expressions">Expressions</a>

An **_<dfn>expression</dfn>_** is a part of a _message_ that will be determined
during the _message_'s formatting.

An _expression_ MUST begin with U+007B LEFT CURLY BRACKET `{`
and end with U+007D RIGHT CURLY BRACKET `}`.
An _expression_ MUST NOT be empty.
An _expression_ cannot contain another _expression_.
An _expression_ MAY contain one more _attributes_.

A **_<dfn>literal-expression</dfn>_** contains a _literal_,
optionally followed by a _function_.

A **_<dfn>variable-expression</dfn>_** contains a _variable_,
optionally followed by a _function_.

A **_<dfn>function-expression</dfn>_** contains a _function_ without an _operand_.

```abnf
expression          = literal-expression
                    / variable-expression
                    / function-expression
literal-expression  = "{" o literal [s function] *(s attribute) o "}"
variable-expression = "{" o variable [s function] *(s attribute) o "}"
function-expression = "{" o function *(s attribute) o "}"
```

There are several types of _expression_ that can appear in a _message_.
All _expressions_ share a common syntax. The types of _expression_ are:

1. The value of a _local-declaration_
2. A kind of _placeholder_ in a _pattern_

Additionally, an _input-declaration_ can contain a _variable-expression_.

> Examples of different types of _expression_
>
> Declarations:
>
> ```text
> .input {$x :ns:func option=value}
> .local $y = {|This is an expression|}
> ```
>
> Placeholders:
>
> ```text
> This placeholder contains a literal expression: {|literal|}
> This placeholder contains a variable expression: {$variable}
> This placeholder references a function on a variable: {$variable :ns:func with=options}
> This placeholder contains a function expression with a variable-valued option: {:ns:func option=$variable}
> ```

#### <a name="Operand" id="Operand" href="#Operand">Operand</a>

An **_<dfn>operand</dfn>_** is the _literal_ of a _literal-expression_ or
the _variable_ of a _variable-expression_.

#### <a name="Function" id="Function" href="#Function">Function</a>

A **_<dfn>function</dfn>_** is named functionality in an _expression_.
_Functions_ are used to evaluate, format, select, or otherwise process data
values during formatting.

A _function_ can appear in an _expression_ by itself or following a single _operand_.
When following an _operand_, the _operand_ serves as input to the _function_.

The resolution of a _function_ relies on an implementation-defined _function handler_.
Some _functions_ can be used both as a _selector_ as well as in a _placeholder_;
others are only valid in one of these positions.
_Functions_ also differ in their requirements on the _operand_ and _options_ that they accept.
See [Function Resolution](#function-resolution)
and [Default Functions](#default-functions) for more information.

A _function_ starts with a prefix sigil `:` followed by an _identifier_.
The _identifier_ MAY be followed by one or more _options_.
_Options_ are not required.

```abnf
function = ":" identifier *(s option)
```

> A _message_ with a _function_ operating on the _variable_ `$now`:
>
> ```text
> It is now {$now :datetime}.
> ```

###### <a name="Options" id="Options" href="#Options">Options</a>

An **_<dfn>option</dfn>_** is a key-value pair
containing a named argument that is passed to a _function_.

An _option_ has an _identifier_ and an _option value_.
The _identifier_ is separated from the _option value_ by an U+003D EQUALS SIGN `=` along with
optional whitespace.
The **_<dfn>option value</dfn>_** can be either a _literal_ or a _variable_.

Multiple _options_ are permitted in a _function_.
_Options_ are separated from the preceding _function_ _identifier_
and from each other by whitespace.
Each _option_'s _identifier_ MUST be unique within the _function_:
a _function_ with duplicate _option_ _identifiers_ is not _valid_
and will produce a _Duplicate Option Name_ error during processing.

The order of _options_ is not significant.

```abnf
option = identifier o "=" o (literal / variable)
```

> Examples of _functions_ with _options_
>
> A _message_ using the `:date` function.
> The _option_ `length` has the literal `long` as its value:
>
> ```text
> Today is {$now :date length=long}!
> ```

> A _message_ using the `:date` function.
> The _option_ `length` has a variable `$dateLength` as its value:
>
> ```text
> Today is {$now :date length=$dateLength}!
> ```

### <a name="Markup" id="Markup" href="#Markup">Markup</a>

**_<dfn>Markup</dfn>_** _placeholders_ are _pattern_ parts
that can be used to represent non-language parts of a _message_,
such as inline elements or styling that should apply to a span of parts.

_Markup_ MUST begin with U+007B LEFT CURLY BRACKET `{`
and end with U+007D RIGHT CURLY BRACKET `}`.
_Markup_ MAY contain one more _attributes_.

_Markup_ comes in three forms:

**_<dfn>Markup-open</dfn>_** starts with U+0023 NUMBER SIGN `#` and
represents an opening element within the _message_,
such as markup used to start a span.
It MAY include _options_.

**_<dfn>Markup-standalone</dfn>_** starts with U+0023 NUMBER SIGN `#`
and has a U+002F SOLIDUS `/` immediately before its closing `}`
representing a self-closing or standalone element within the _message_.
It MAY include _options_.

**_<dfn>Markup-close</dfn>_** starts with U+002F SOLIDUS `/` and
is a _pattern_ part ending a span.

```abnf
markup = "{" o "#" identifier *(s option) *(s attribute) o ["/"] "}"  ; open and standalone
       / "{" o "/" identifier *(s option) *(s attribute) o "}"  ; close
```

> A _message_ with one `button` markup span and a standalone `img` markup element:
>
> ```text
> {#button}Submit{/button} or {#img alt=Cancel src=|../cancel.jpg| /}.
> ```

> A _message_ containing _markup_ that uses _options_ to pair
> two closing markup _placeholders_ to the one open markup _placeholder_:
>
> ```text
> {#ansi attr=|bold,italic|}Bold and italic{/ansi attr=bold} italic only {/ansi attr=italic} no formatting.}
> ```

A _markup-open_ can appear without a corresponding _markup-close_.
A _markup-close_ can appear without a corresponding _markup-open_.
_Markup_ _placeholders_ can appear in any order without making the _message_ invalid.
However, specifications or implementations defining _markup_ might impose requirements
on the pairing, ordering, or contents of _markup_ during _formatting_.

### <a name="Attributes" id="Attributes" href="#Attributes">Attributes</a>

An **_<dfn>attribute</dfn>_** is an _identifier_ with an optional value
that appears in an _expression_ or in _markup_.
During formatting, _attributes_ have no effect,
and they can be treated as code comments.

_Attributes_ are prefixed by a U+0040 COMMERCIAL AT `@` sign,
followed by an _identifier_.
An _attribute_ MAY have a _literal_ value which is separated from the _identifier_
by an U+003D EQUALS SIGN `=` along with optional whitespace.

Multiple _attributes_ are permitted in an _expression_ or _markup_.
Each _attribute_ is separated by whitespace.

Each _attribute_'s _identifier_ SHOULD be unique within the _expression_ or _markup_:
all but the last _attribute_ with the same _identifier_ are ignored.
The order of _attributes_ is not otherwise significant.

```abnf
attribute = "@" identifier [o "=" o literal]
```

> Examples of _expressions_ and _markup_ with _attributes_:
>
> A _message_ including a _literal_ that should not be translated:
>
> ```text
> In French, "{|bonjour| @translate=no}" is a greeting
> ```
>
> A _message_ with _markup_ that can be copied:
>
> ```text
> Have a {#span @can-copy}great and wonderful{/span @can-copy} birthday!
> ```

### <a name="Other_Syntax_Elements" id="Other_Syntax_Elements" href="#Other_Syntax_Elements">Other Syntax Elements</a>

This section defines common elements used to construct _messages_.

#### <a name="Keywords" id="Keywords" href="#Keywords">Keywords</a>

A **_<dfn>keyword</dfn>_** is a reserved token that has a unique meaning in the _message_ syntax.

The following three keywords are defined: `.input`, `.local`, and `.match`.
Keywords are always lowercase and start with U+002E FULL STOP `.`.

```abnf
input = %s".input"
local = %s".local"
match = %s".match"
```

#### <a name="Literals" id="Literals" href="#Literals">Literals</a>

A **_<dfn>literal</dfn>_** is a character sequence that appears outside
of _text_ in various parts of a _message_.
A _literal_ can appear
as a _key_ value,
as the _operand_ of a _literal-expression_,
or as an _option value_.
A _literal_ MAY include any Unicode code point except for U+0000 NULL.

All code points are preserved.

> [!IMPORTANT]
> Most text, including that produced by common keyboards and input methods,
> is already encoded in the canonical form known as
> [Unicode Normalization Form C](https://unicode.org/reports/tr15) ("NFC").
> A few languages, legacy character encoding conversions, or operating environments
> can result in _literal_ values that are not in this form.
> Some uses of _literals_ in MessageFormat,
> notably as the value of _keys_,
> apply NFC to the _literal_ value during processing or comparison.
> While there is no requirement that the _literal_ value actually be entered
> in a normalized form,
> users are cautioned to employ the same character sequences
> for equivalent values and, whenever possible, ensure _literals_ are in NFC.

A **_<dfn>quoted literal</dfn>_** begins and ends with U+005E VERTICAL BAR `|`.
The characters `\` and `|` within a _quoted literal_ MUST be
escaped as `\\` and `\|`.

> [!NOTE]
> Unpaired surrogate code points (`U+D800` through `U+DFFF` inclusive)
> are allowed in _quoted literals_ for compatibility with UTF-16 based
> implementations that do not check for this encoding error.

An **_<dfn>unquoted literal</dfn>_** is a _literal_ that does not require the `|`
quotes around it to be distinct from the rest of the _message_ syntax.
An _unquoted literal_ MAY be used when the _string value_ of the _literal_
matches the `unquoted-literal` production.
It will thus contain no whitespace (nor certain other characters).
Implementations MUST NOT distinguish between _quoted literals_ and _unquoted literals_
that have the same sequence of code points.

_Unquoted literals_ can contain any characters also valid in _name_,
less _name_'s additional restrictions on the first character.

```abnf
literal          = quoted-literal / unquoted-literal
quoted-literal   = "|" *(quoted-char / escaped-char) "|"
unquoted-literal = 1*name-char
```
The **_<dfn>string value</dfn>_** of a _literal_
for _unquoted literals_ is the text content of that _literal_;
or for _quoted literals_, the text content of that _literal_
after removing the enclosing `|` characters
then unescaping any escaped characters.

#### <a name="Names_and_Identifiers" id="Names_and_Identifiers" href="#Names_and_Identifiers">Names and Identifiers</a>

A **_<dfn>name</dfn>_** is a character sequence used in an _identifier_
or as the name for a _variable_
or the value of an _unquoted literal_.

A _name_ can be preceded or followed by bidirectional marks or isolating controls
to aid in presenting names that contain right-to-left or neutral characters.
These characters are **not** part of the value of the _name_ and MUST be treated as if they were not present
when matching _name_ or _identifier_ strings or _unquoted literal_ values.

_Variable_ _names_ are prefixed with `$`.

Two _names_ are considered equal if they are canonically equivalent strings,
that is, if they consist of the same sequence of Unicode code points after
[Unicode Normalization Form C](https://unicode.org/reports/tr15/) ("NFC")
has been applied to both.

The _names_ are [immutable identifiers](https://www.unicode.org/reports/tr31/#Immutable_Identifier_Syntax).

> [!NOTE]
> Implementations are not required to normalize all _names_.
> Comparisons of _name_ values only need be done "as-if" normalization
> has occured.
> Since most text in the wild is already in NFC
> and since checking for NFC is fast and efficient,
> implementations can often substitute checking for actually applying normalization
> to _name_ values.

> [!NOTE]
> _External variables_ can be passed in that are not valid _names_.
> Such variables cannot be referenced in a _message_,
> but are not otherwise errors.

An **_<dfn>identifier</dfn>_** is a character sequence that
identifies a _function_, _markup_, or _option_.
Each _identifier_ consists of a _name_ optionally preceeded by
a **_<dfn>namespace</dfn>_**.
When present, the _namespace_ is separated from the _name_ by a
U+003A COLON `:`.
Built-in _functions_ and their _options_ do not have a _namespace_ identifier.

The _namespace_ `u` (U+0075 LATIN SMALL LETTER U)
is reserved for future standardization.

_Function_ _identifiers_ are prefixed with `:`.
_Markup_ _identifiers_ are prefixed with `#` or `/`.
_Option_ _identifiers_ have no prefix.

Examples:
> A variable:
>```text
> This has a {$variable}
>```
>
> A default function:
> ```text
> This has an {42 :integer}
> ```
>
> A function from the `ns` namespace:
> ```text
> This has a {:ns:function}
> ```
>
> Options with and without a namespace:
> ```text
> This has {:ns:function option=value ns:option=value}
> ```

Support for _namespaces_ and their interpretation is implementation-defined
in this release.

```abnf
variable   = "$" name
option     = identifier o "=" o (literal / variable)

identifier = [namespace ":"] name
namespace  = name
name       = [bidi] name-start *name-char [bidi]
name-start = ALPHA
                                    ;          omit Cc: %x0-1F, Whitespace: « », Ascii: «!"#$%&'()*»
                  / %x2B            ; «+»      omit Ascii: «,-./0123456789:;<=>?@» «[\]^»
                  / %x5F            ; «_»      omit Cc: %x7F-9F, Whitespace: %xA0, Ascii: «`» «{|}~»
                  / %xA1-61B        ;          omit BidiControl: %x61C
                  / %x61D-167F      ;          omit Whitespace: %x1680
                  / %x1681-1FFF     ;          omit Whitespace: %x2000-200A
                  / %x200B-200D     ;          omit BidiControl: %x200E-200F
                  / %x2010-2027     ;          omit Whitespace: %x2028-2029 %x202F, BidiControl: %x202A-202E
                  / %x2030-205E     ;          omit Whitespace: %x205F
                  / %x2060-2065     ;          omit BidiControl: %x2066-2069
                  / %x206A-2FFF     ;          omit Whitespace: %x3000
                  / %x3001-D7FF     ;          omit Cs: %xD800-DFFF
                  / %xE000-FDCF     ;          omit NChar: %xFDD0-FDEF
                  / %xFDF0-FFFD     ;          omit NChar: %xFFFE-FFFF
                  / %x10000-1FFFD   ;          omit NChar: %x1FFFE-1FFFF
                  / %x20000-2FFFD   ;          omit NChar: %x2FFFE-2FFFF
                  / %x30000-3FFFD   ;          omit NChar: %x3FFFE-3FFFF
                  / %x40000-4FFFD   ;          omit NChar: %x4FFFE-4FFFF
                  / %x50000-5FFFD   ;          omit NChar: %x5FFFE-5FFFF
                  / %x60000-6FFFD   ;          omit NChar: %x6FFFE-6FFFF
                  / %x70000-7FFFD   ;          omit NChar: %x7FFFE-7FFFF
                  / %x80000-8FFFD   ;          omit NChar: %x8FFFE-8FFFF
                  / %x90000-9FFFD   ;          omit NChar: %x9FFFE-9FFFF
                  / %xA0000-AFFFD   ;          omit NChar: %xAFFFE-AFFFF
                  / %xB0000-BFFFD   ;          omit NChar: %xBFFFE-BFFFF
                  / %xC0000-CFFFD   ;          omit NChar: %xCFFFE-CFFFF
                  / %xD0000-DFFFD   ;          omit NChar: %xDFFFE-DFFFF
                  / %xE0000-EFFFD   ;          omit NChar: %xEFFFE-EFFFF
                  / %xF0000-FFFFD   ;          omit NChar: %xFFFFE-FFFFF
                  / %x100000-10FFFD ;          omit NChar: %x10FFFE-10FFFF
name-char  = name-start / DIGIT / "-" / "."
```

> [!NOTE]
> Syntactically, the definitions of `identifier` and `name-char` provide backwards compatibility over time by allowing a stable,
> wide range of characters.
> So when there is a new character in a version of Unicode, it can be used in any conformant implementation of MessageFormat.
> The definition currently excludes:
> * Most ASCII except for letters and characters used for numbers
>    * This avoids conflicts with syntax characters, and reserves some characters for future syntax.
> * Bidirectional controls (`Bidi_C`)
> * Control characters (`GC=Cc`, but not Format characters: `GC=Cf`)
> * Whitespace characters (`WSpace`)
> * Surrogate code points (`GC=Cs`)
> * Non-Characters (`NChar`)

A **_<dfn>reserved identifier</dfn>_** is one that satisfies the following conditions:
- Includes no _namespace_ or uses a _namespace_ consisting of a single letter
  in the ranges a-z and A-Z.
- Has a _name_ that matches the following ABNF:
```abnf
reserved-identifier = ALPHA *[ALPHA / DIGIT / "." / "-" / "_"]
```

A **_<dfn>custom identifier</dfn>_** is any _identifier_ that is not a _reserved identifier_.

> [!NOTE]
> Choose a _custom identifier_ for any _functions_, _markup_, or _attributes_ not defined by this specification.
> Use a _namespace_ in a _custom identifier_ to identify a _function_ that is not a _default function_
> or when defining a custom _option_ for a _default function_.
>
> _Variable_ _names_ are encouraged to use _reserved identifiers_.
> _Option_ _names_ for custom _functions_ are encouraged to use _reserved identifiers_.

The syntax allows a wide range of characters in _names_ and _identifiers_.
Implementers and authors of _functions_ and _messages_,
including _functions_, _options_, and _variables_,
SHOULD avoid creating _names_ that could produce confusion or harm usability
by choosing _names_ consistent with the following guidelines.
MessageFormat tools, such as linters, SHOULD warn when _names_ chosen by users
violate these constraints.
>
> 1. [Unicode Default Identifier Syntax](https://www.unicode.org/reports/tr31/#Default_Identifier_Syntax)
> 2. [Unicode General Security Profile for Identifiers](https://www.unicode.org/reports/tr39/#General_Security_Profile)

### <a name="Escape_Sequences" id="Escape_Sequences" href="#Escape_Sequences">Escape Sequences</a>

An **_<dfn>escape sequence</dfn>_** is a two-character sequence starting with
U+005C REVERSE SOLIDUS `\`.

An _escape sequence_ allows the appearance of lexically meaningful characters
in the body of _text_ or _quoted literal_ sequences.
Each _escape sequence_ represents the literal character immediately following the initial `\`.

```abnf
escaped-char = backslash ( backslash / "{" / "|" / "}" )
backslash    = %x5C ; U+005C REVERSE SOLIDUS "\"
```

> [!NOTE]
> The `escaped-char` rule allows escaping some characters in places where
> they do not need to be escaped, such as braces in a _quoted literal_.
> For example, `|foo {bar}|` and `|foo \{bar\}|` are synonymous.

When writing or generating a _message_, escape sequences SHOULD NOT be used
unless required by the syntax.
That is, inside _literals_ only escape `|`
and inside _patterns_ only escape `{` and `}`.

#### <a name="Whitespace" id="Whitespace" href="#Whitespace">Whitespace</a>

Outside of the _text_ parts of _patterns_ and outside of _quoted literals_
the syntax limits whitespace characters to the following:
`U+0009 CHARACTER TABULATION` (tab),
`U+000A LINE FEED` (new line),
`U+000D CARRIAGE RETURN`,
`U+3000 IDEOGRAPHIC SPACE`,
or `U+0020 SPACE`.

In the _text_ parts of _patterns_ and in _quoted literals_,
whitespace is part of the content and is recorded and stored verbatim.
Whitespace is not significant outside translatable text, except where required by the syntax.

There are two whitespace productions in the syntax.
**_<dfn>Optional whitespace</dfn>_** is whitespace that is not required by the syntax,
but which users might want to include to increase the readability of a _message_.
**_<dfn>Required whitespace</dfn>_** is whitespace that is required by the syntax.

Both types of whitespace optionally permit the use of the bidirectional isolate controls
and certain strongly directional marks.
These can assist users in presenting _messages_ that contain right-to-left
text, _literals_, or _names_ (including those for _functions_, _options_,
_option values_, and _keys_)

_Messages_ that contain right-to-left (aka RTL) characters SHOULD use one of the
following mechanisms to make messages display intelligibly in plain-text editors:

1. Use paired isolating bidi controls `U+2066 LEFT-TO-RIGHT ISOLATE` ("LRI")
   and `U+2069 POP DIRECTIONAL ISOLATE` ("PDI") as permitted by the ABNF around
   parts of any _message_ containing RTL characters:
   - _inside_ of _placeholder_ markers `{` and `}`
   - _outside_ _quoted-pattern_ markers `{{` and `}}`
   - _outside_ of _variable_, _function_, _markup_, or _attribute_,
     including the identifying sigil (e.g. ``<LRI>`$var`</PDI>`` or ``<LRI>`:ns:name`</PDI>``)
2. Use the 'local-effect' bidi marks
   `U+061C ARABIC LETTER MARK`, `U+200E LEFT-TO-RIGHT MARK` or
   `U+200F RIGHT-TO-LEFT MARK` as permitted by the ABNF before or after _identifiers_,
   _names_, unquoted _literals_, or _option values_,
   especially when the values contain a mix of neutral, weakly directional, and
   strongly directional characters.

> [!IMPORTANT]
> Always take care **not** to add bidirectional controls or marks
> where they would be semantically significant
> or where they would unintentionally become part of the _message_'s output:
> - do not put them inside of a _literal_ except when they are part of the value,
>   (instead put them outside of _literal_ quotes, such as ``<LRM>`|...|`<LRM>``)
> - do not put them inside quoted _patterns_ except when they are part of the text,
>   (instead put them outside of quoted _patterns_, such as ``<LRI>`{{...}}`<PDI>``)
> - do not put them outside _placeholders_,
>   (instead put them inside the _placeholder_, such as `{`<LRI>`$foo :number`<PDI>`}`)
>
> Controls placed inside _literal_ quotes or quoted _patterns_ are part of the _literal_
> or _pattern_.
> Controls in a _pattern_ will appear in the output of the message.
> Controls inside _literal_ quotes are part of the _literal_ and
> will be considered in operations such as matching a _key_ to a _selector_.

> [!NOTE]
> Users cannot be expected to create or manage bidirectional controls or
> marks in _messages_, since the characters are invisible and can be difficult
> to manage.
> Tools (such as resource editors or translation editors)
> and other implementations of MessageFormat serialization are strongly
> encouraged to provide paired isolates around any right-to-left
> syntax as described above so that _messages_ display appropriately as plain text.

These definitions of _whitespace_ implement
[UAX#31 Requirement R3a-2](https://www.unicode.org/reports/tr31/#R3a-2).
It is a profile of R3a-1 in that specification because:
- The following pattern whitespace characters are not allowed:
  `U+000B FORM FEED`,
  `U+000C VERTICAL TABULATION`,
  `U+0085 NEXT LINE`,
  `U+2028 LINE SEPARATOR` and
  `U+2029 PARAGRAPH SEPARATOR`.
- The character `U+3000 IDEOGRAPHIC SPACE`
  _is_ interpreted as whitespace.
 - The following directional marks and isolates
   are treated as ignorable format controls:
   `U+061C ARABIC LETTER MARK`,
   `U+200E LEFT-TO-RIGHT MARK`,
   `U+200F RIGHT-TO-LEFT MARK`,
   `U+2066 LEFT-TO-RIGHT ISOLATE`,
   `U+2067 RIGHT-TO-LEFT ISOLATE`,
   `U+2068 FIRST STRONG ISOLATE`,
   and `U+2069 POP DIRECTIONAL ISOLATE`.
   (The character `U+061C` is an addition according to R3a.)

> [!NOTE]
> The character U+3000 IDEOGRAPHIC SPACE is included in whitespace for
> compatibility with certain East Asian keyboards and input methods,
> in which users might accidentally create these characters in a _message_.

```abnf
; Required whitespace
s = *bidi ws o

; Optional whitespace
o = *(s / bidi)

; Bidirectional marks and isolates
; ALM / LRM / RLM / LRI, RLI, FSI & PDI
bidi = %x061C / %x200E / %x200F / %x2066-2069

; Whitespace characters
ws = SP / HTAB / CR / LF / %x3000
```

### <a name="Complete_ABNF" id="Complete_ABNF" href="#Complete_ABNF">Complete ABNF</a>

The grammar is formally defined in [`message.abnf`](#messageabnf)
using the ABNF notation [[STD68](https://www.rfc-editor.org/info/std68)],
including the modifications found in [RFC 7405](https://www.rfc-editor.org/rfc/rfc7405).

RFC7405 defines a variation of ABNF that is case-sensitive.
Some ABNF tools are only compatible with the specification found in
[RFC 5234](https://www.rfc-editor.org/rfc/rfc5234).
To make `message.abnf` compatible with that version of ABNF, replace
the rules of the same name with this block:

```abnf
input = %x2E.69.6E.70.75.74  ; ".input"
local = %x2E.6C.6F.63.61.6C  ; ".local"
match = %x2E.6D.61.74.63.68  ; ".match"
```

