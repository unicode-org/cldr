## <a name="Introduction" id="Introduction" href="#Introduction">Introduction</a>

One of the challenges in adapting software to work for
users with different languages and cultures is the need for **_<dfn>dynamic messages</dfn>_**.
Whenever a user interface needs to present data as part of a larger string,
that data needs to be formatted (and the message may need to be altered)
to make it culturally accepted and grammatically correct.

> For example, if your US English (`en-US`) interface has a message like:
>
> > Your item had 1,023 views on April 3, 2023
>
> You want the translated message to be appropriately formatted into French:
>
> > Votre article a eu 1 023 vues le 3 avril 2023
>
> Or Japanese:
>
> > あなたのアイテムは 2023 年 4 月 3 日に 1,023 回閲覧されました。

This specification defines the
data model, syntax, processing, and conformance requirements
for the next generation of _dynamic messages_.
It is intended for adoption by programming languages and APIs.
This will enable the integration of
existing internationalization APIs (such as the date and number formats shown above),
grammatical matching (such as plurals or genders),
as well as user-defined formats and message selectors.

The document is the successor to ICU MessageFormat.

### <a name="Conformance" id="Conformance" href="#Conformance">Conformance</a>

Everything in this specification is normative except for:
sections marked as non-normative,
all authoring guidelines, diagrams, examples, and notes.

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL
NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED",
"MAY", and "OPTIONAL" in this document are to be interpreted as
described in BCP 14 \[[RFC2119](https://www.rfc-editor.org/rfc/rfc2119)\]
\[[RFC8174](https://www.rfc-editor.org/rfc/rfc8174)\] when, and only when, they
appear in all capitals, as shown here.

### <a name="Terminology_and_Conventions" id="Terminology_and_Conventions" href="#Terminology_and_Conventions">Terminology and Conventions</a>

A **_term_** looks like this when it is defined in this specification.

A reference to a _term_ looks like this.

> Examples are non-normative and styled like this.

> [!IMPORTANT]
> Text marked "Important" like this are normative.

> [!NOTE]
> Notes are non-normative.

### <a name="Stability_Policy" id="Stability_Policy" href="#Stability_Policy">Stability Policy</a>

Updates to this specification will not make any _valid_ _message_ become not _valid_.

Updates to this specification will not specify an _error_ for any _message_
that previously did not specify an _error_.

Updates to this specification will not specify the use of a _fallback value_ for any _message_
that previously did not specify a _fallback value_.

Updates to this specification will not change the syntactical meaning
of any syntax defined in this specification.

Updates to this specification will not remove any _default functions_.

Updates to this specification will not remove any _options_ or _option values_
defined for _default functions_.

> [!IMPORTANT]
> _Functions_ that are not marked **Draft** are **Stable** and subject to
> the provisions of this stability policy.
>
> _Functions_ or _options_ marked as **Draft** are not stable.
> Their name, _operands_, and _options_/_option values_, and other requirements
> might change or be removed before being declared **Stable** in a future release.

> [!NOTE]
> The foregoing policies are _not_ a guarantee that the results of formatting will never change.
> Even when this specification or its implementation do not change,
> the _function handlers_ for date formatting, number formatting and so on
> can change their results over time or behave differently due to local runtime
> differences in implementation or changes to locale data
> (such as due to the release of new CLDR versions).

Updates to this specification will only reserve, define, or require
_identifiers_ which are _reserved identifiers_.

Future versions of this specification will not introduce changes
to the data model that would result in a data model representation
based on this version being invalid.

> For example, existing interfaces or fields will not be removed.

> [!IMPORTANT]
> This stability policy allows any of the following, non-exhaustive list, of changes
> in future versions of this specification:
> - Future versions may define new syntax and structures
>   that would not be supported by this version of the specification.
> - Future versions may add additional structure or meaning to existing syntax.
> - Future versions may define new _keywords_.
> - Future versions may make previously invalid _messages_ valid.
> - Future versions may define additional _default functions_.
>   or may reserve the names of _functions_ for the purposes of interoperability.
> - Future versions may define additional _options_ to existing functions.
> - Future versions may define additional _option values_ for existing _options_.
> - Future versions may deprecate (but not remove) _keywords_, _functions_, _options_, or _option values_.
> - Future versions of this specification may introduce changes
>   to the data model that would result in future data model representations
>   not being valid for implementations of this version of the data model.
>   - For example, a future version could introduce a new _keyword_,
>     whose data model representation would be a new interface
>     that is not recognized by this version's data model.

