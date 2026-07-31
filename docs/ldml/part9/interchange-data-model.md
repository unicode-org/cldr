## <a name="Interchange_Data_Model" id="Interchange_Data_Model" href="#Interchange_Data_Model">Interchange Data Model</a>

This section defines a data model representation of Unicode MessageFormat _messages_.

Implementations are not required to use this data model for their internal representation of messages.
Neither are they required to provide an interface that accepts or produces
representations of this data model.

The major reason this specification provides a data model is to allow interchange of
the logical representation of a _message_ between different implementations.
This includes mapping legacy formatting syntaxes (such as ICU MessageFormat)
to a Unicode MessageFormat implementation.
Another use would be in converting to or from translation formats without
the need to continually parse and serialize all or part of a message.

Implementations that expose APIs supporting the production, consumption, or transformation of a
_message_ as a data structure are encouraged to use this data model.

This data model provides these capabilities:
- any Unicode MessageFormat _message_ can be parsed into this representation
- this data model representation can be serialized as a well-formed
  Unicode MessageFormat _message_
- parsing a Unicode MessageFormat _message_ into a data model representation
  and then serializing it results in an equivalently functional message

This data model might also be used to:
- parse non Unicode MessageFormat messages into a data model
  (and therefore re-serialize it as Unicode MessageFormat).
  Note that this depends on compatibility between the two syntaxes.
- re-serialize a Unicode MessageFormat _message_ into some other format
  including (but not limited to) other formatting syntaxes
  or translation formats.

To ensure compatibility across all platforms,
this interchange data model is defined here using TypeScript notation.
An equivalent JSON Schema definition [`message.json`](#messagejson) is also provided,
for use with message data encoded as JSON or compatible formats, such as YAML.

Note that while the data model description below is the canonical one,
the JSON Schema definition is intended for interchange between systems and processors.
To that end, it relaxes some aspects of the data model, such as allowing
declarations, options, and attributes to be optional rather than required properties.

> [!IMPORTANT]
> The data model uses the field name `name` to denote various interface identifiers.
> In the Unicode MessageFormat [syntax](#syntax), the source for these `name` fields
> sometimes uses the production `identifier`.
> This happens when the named item, such as a _function_, supports namespacing.

### <a name="Message_Model" id="Message_Model" href="#Message_Model">Message Model</a>

A `SelectMessage` corresponds to a syntax message that includes _selectors_.
A message without _selectors_ and with a single _pattern_ is represented by a `PatternMessage`.

In the syntax,
a `PatternMessage` may be represented either as a _simple message_ or as a _complex message_,
depending on whether it has declarations and if its `pattern` is allowed in a _simple message_.

```ts
type Message = PatternMessage | SelectMessage;

interface PatternMessage {
  type: "message";
  declarations: Declaration[];
  pattern: Pattern;
}

interface SelectMessage {
  type: "select";
  declarations: Declaration[];
  selectors: VariableRef[];
  variants: Variant[];
}
```

Each message _declaration_ is represented by a `Declaration`,
which connects the `name` of a _variable_
with its _expression_ `value`.
The `name` does not include the initial `$` of the _variable_.

The `name` of an `InputDeclaration` MUST be the same
as the `name` in the `VariableRef` of its `VariableExpression` `value`.

```ts
type Declaration = InputDeclaration | LocalDeclaration;

interface InputDeclaration {
  type: "input";
  name: string;
  value: VariableExpression;
}

interface LocalDeclaration {
  type: "local";
  name: string;
  value: Expression;
}
```

In a `SelectMessage`,
the `keys` and `value` of each _variant_ are represented as an array of `Variant`.
For the `CatchallKey`, a string `value` may be provided to retain an identifier.
This is always `'*'` in the Unicode MessageFormat syntax, but may vary in other formats.

```ts
interface Variant {
  keys: Array<Literal | CatchallKey>;
  value: Pattern;
}

interface CatchallKey {
  type: "*";
  value?: string;
}
```

### <a name="Pattern_Model" id="Pattern_Model" href="#Pattern_Model">Pattern Model</a>

Each `Pattern` contains a linear sequence of text and placeholders corresponding to potential output of a message.

Each element of the `Pattern` MUST either be a non-empty string, an `Expression`, or a `Markup` object.
String values represent literal _text_.
String values include all processing of the underlying _text_ values,
including escape sequence processing.
`Expression` wraps each of the potential _expression_ shapes.
`Markup` wraps each of the potential _markup_ shapes.

Implementations MUST NOT rely on the set of `Expression` and
`Markup` interfaces defined in this document being exhaustive.
Future versions of this specification might define additional
expressions or markup.

```ts
type Pattern = Array<string | Expression | Markup>;

type Expression =
  | LiteralExpression
  | VariableExpression
  | FunctionExpression;

interface LiteralExpression {
  type: "expression";
  arg: Literal;
  function?: FunctionRef;
  attributes: Attributes;
}

interface VariableExpression {
  type: "expression";
  arg: VariableRef;
  function?: FunctionRef;
  attributes: Attributes;
}

interface FunctionExpression {
  type: "expression";
  arg?: never;
  function: FunctionRef;
  attributes: Attributes;
}
```

### <a name="Expression_Model" id="Expression_Model" href="#Expression_Model">Expression Model</a>

The `Literal` and `VariableRef` correspond to the the _literal_ and _variable_ syntax rules.
When they are used as the `body` of an `Expression`,
they represent _expression_ values with no _function_.

`Literal` represents all literal values, both _quoted literal_ and _unquoted literal_.
The presence or absence of quotes is not preserved by the data model.
The `value` of `Literal` is the "cooked" value (i.e. escape sequences are processed).

In a `VariableRef`, the `name` does not include the initial `$` of the _variable_.

```ts
interface Literal {
  type: "literal";
  value: string;
}

interface VariableRef {
  type: "variable";
  name: string;
}
```

A `FunctionRef` represents a _function_.
The `name` does not include the `:` starting sigil.

`Options` is a key-value mapping containing options,
and is used to represent the _function_ and _markup_ _options_.

```ts
interface FunctionRef {
  type: "function";
  name: string;
  options: Options;
}

type Options = Map<string, Literal | VariableRef>;
```

### <a name="Markup_Model" id="Markup_Model" href="#Markup_Model">Markup Model</a>

A `Markup` object has a `kind` of either `"open"`, `"standalone"`, or `"close"`,
each corresponding to _open_, _standalone_, and _close_ _markup_.
The `name` in these does not include the starting sigils `#` and `/`
or the ending sigil `/`.
The `options` for markup use the same key-value mapping as `FunctionRef`.

```ts
interface Markup {
  type: "markup";
  kind: "open" | "standalone" | "close";
  name: string;
  options: Options;
  attributes: Attributes;
}
```

### <a name="Attribute_Model" id="Attribute_Model" href="#Attribute_Model">Attribute Model</a>

`Attributes` is a key-value mapping
used to represent the _expression_ and _markup_ _attributes_.

_Attributes_ with no value are represented by `true` here.

```ts
type Attributes = Map<string, Literal | true>;
```

### <a name="Model_Extensions" id="Model_Extensions" href="#Model_Extensions">Model Extensions</a>

Implementations MAY extend this data model with additional interfaces,
as well as adding new fields to existing interfaces.
When encountering an unfamiliar field, an implementation MUST ignore it.
For example, an implementation could include a `span` field on all interfaces
encoding the corresponding start and end positions in its source syntax.

In general,
implementations MUST NOT extend the sets of values for any defined field or type
when representing a valid message.
However, when using this data model to represent an invalid message,
an implementation MAY do so.
This is intended to allow for the representation of "junk" or invalid content within messages.

### <a name="messagejson" id="messagejson" href="#messagejson">`message.json`</a>

```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "$id": "https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model/message.json",

  "oneOf": [{ "$ref": "#/$defs/message" }, { "$ref": "#/$defs/select" }],

  "$defs": {
    "literal": {
      "type": "object",
      "properties": {
        "type": { "const": "literal" },
        "value": { "type": "string" }
      },
      "required": ["type", "value"]
    },
    "variable": {
      "type": "object",
      "properties": {
        "type": { "const": "variable" },
        "name": { "type": "string" }
      },
      "required": ["type", "name"]
    },
    "literal-or-variable": {
      "oneOf": [{ "$ref": "#/$defs/literal" }, { "$ref": "#/$defs/variable" }]
    },

    "options": {
      "type": "object",
      "additionalProperties": { "$ref": "#/$defs/literal-or-variable" }
    },
    "attributes": {
      "type": "object",
      "additionalProperties": {
        "oneOf": [{ "$ref": "#/$defs/literal" }, { "const": true }]
      }
    },

    "function": {
      "type": "object",
      "properties": {
        "type": { "const": "function" },
        "name": { "type": "string" },
        "options": { "$ref": "#/$defs/options" }
      },
      "required": ["type", "name"]
    },
    "expression": {
      "type": "object",
      "properties": {
        "type": { "const": "expression" },
        "arg": { "$ref": "#/$defs/literal-or-variable" },
        "function": { "$ref": "#/$defs/function" },
        "attributes": { "$ref": "#/$defs/attributes" }
      },
      "anyOf": [
        { "required": ["type", "arg"] },
        { "required": ["type", "function"] }
      ]
    },

    "markup": {
      "type": "object",
      "properties": {
        "type": { "const": "markup" },
        "kind": { "enum": ["open", "standalone", "close"] },
        "name": { "type": "string" },
        "options": { "$ref": "#/$defs/options" },
        "attributes": { "$ref": "#/$defs/attributes" }
      },
      "required": ["type", "kind", "name"]
    },

    "pattern": {
      "type": "array",
      "items": {
        "oneOf": [
          { "type": "string" },
          { "$ref": "#/$defs/expression" },
          { "$ref": "#/$defs/markup" }
        ]
      }
    },

    "input-declaration": {
      "type": "object",
      "properties": {
        "type": { "const": "input" },
        "name": { "type": "string" },
        "value": {
          "allOf": [
            { "$ref": "#/$defs/expression" },
            {
              "properties": {
                "arg": { "$ref": "#/$defs/variable" }
              },
              "required": ["arg"]
            }
          ]
        }
      },
      "required": ["type", "name", "value"]
    },
    "local-declaration": {
      "type": "object",
      "properties": {
        "type": { "const": "local" },
        "name": { "type": "string" },
        "value": { "$ref": "#/$defs/expression" }
      },
      "required": ["type", "name", "value"]
    },
    "declarations": {
      "type": "array",
      "items": {
        "oneOf": [
          { "$ref": "#/$defs/input-declaration" },
          { "$ref": "#/$defs/local-declaration" }
        ]
      }
    },

    "variant-key": {
      "oneOf": [
        { "$ref": "#/$defs/literal" },
        {
          "type": "object",
          "properties": {
            "type": { "const": "*" },
            "value": { "type": "string" }
          },
          "required": ["type"]
        }
      ]
    },
    "message": {
      "type": "object",
      "properties": {
        "type": { "const": "message" },
        "declarations": { "$ref": "#/$defs/declarations" },
        "pattern": { "$ref": "#/$defs/pattern" }
      },
      "required": ["type", "declarations", "pattern"]
    },
    "select": {
      "type": "object",
      "properties": {
        "type": { "const": "select" },
        "declarations": { "$ref": "#/$defs/declarations" },
        "selectors": {
          "type": "array",
          "items": { "$ref": "#/$defs/variable" }
        },
        "variants": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "keys": {
                "type": "array",
                "items": { "$ref": "#/$defs/variant-key" }
              },
              "value": { "$ref": "#/$defs/pattern" }
            },
            "required": ["keys", "value"]
          }
        }
      },
      "required": ["type", "declarations", "selectors", "variants"]
    }
  }
}
```

