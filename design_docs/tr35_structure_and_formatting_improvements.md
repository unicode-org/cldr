# Design Proposal: UTS #35 (LDML) Restructuring & Rendering Improvements

**Author:** Younies Mahmoud (younies@google.com)  
**Status:** Draft  
**Target Specifications:** Unicode Technical Standard #35 (LDML Parts 1–9)  
**Target Repository:** `cldr` (`docs/ldml/` and `tools/scripts/tr-archive/`)  

---

## 1. Executive Summary

Unicode Technical Standard #35 (LDML) is the primary specification governing locale data exchange across software platforms (ICU, ICU4X, CLDR, Android, iOS, Chrome, V8, Node.js, etc.). 

To make UTS #35 significantly easier to **implement**, **test**, and **audit**, this proposal outlines a comprehensive structural, technical, and toolchain modernization strategy:

1. **Granular Linkability**: Stable, unique anchor IDs for every rule, subsection, and item point (e.g., `#misc-patterns-approximately`).
2. **Modular "Bulleted" Structure ("Bulletability")**: Deconstructing dense prose into single-purpose normative statements with explicit **Base Rule**, **Exception**, **Fallback**, and **Special Case** sub-clauses.
3. **Visual Hierarchy & Color Differentiation**: Distinct color schemes, typography, and badges separating section headers from sub-point keys/tokens (`approximately`, `atMost`) to eliminate visual collapse.
4. **Syntax-Highlighted Code Blocks**: Enforcing fenced code snippets (XML, DTD, BCP 47, EBNF) with color themes and copy tools.
5. **Machine-Readable Conformance Test Fixtures**: Standardized JSON/YAML test files alongside the spec for automated engine validation.
6. **Interactive Schema Cross-Referencing**: Linking XML element/attribute references directly to DTD schema definitions and sample CLDR data.
7. **Formal Grammars & Syntax Diagrams**: Providing explicit EBNF/ABNF definitions for all pattern mini-languages.
8. **Version Badging & Deprecation Markers**: Visual tags (`[Added in v45]`, `[Deprecated in v46]`) and version diff tools.
9. **Interactive Terminology Tooltips & Glossary**: Hoverable tooltips for technical terms (`skeleton`, `pattern`, `myriad`).

---

## 2. Motivation & Problem Statement

Currently, UTS #35 faces several implementer pain points:
- **Visual Hierarchy Collapse**: Section headers (e.g., `Miscellaneous Patterns`) and individual item sub-points (e.g., `approximately`, `atMost`, `atLeast`) use the same font style and black color, making it impossible to visually distinguish a major section from a sub-item.
- **Unlinkable Sub-Points**: Sub-items under a section (such as specific pattern keys or attributes) are rendered as plain bold text without link anchors, preventing implementers from directly linking to individual keys in conformance tests or codebase comments.
- **Dense Prose**: Paragraphs often mix primary rules, historical notes, fallback behaviors, and edge-case exceptions together.
- **Unformatted DTD/XML Snippets**: DTD declarations and XML structures are sometimes embedded as inline plain text or unhighlighted `<pre>` blocks.

---

## 3. Core Proposal Pillars

### 3.1 Pillar 1: Granular Linkability (Point-Level Anchors)

#### Requirements
- Every normative statement, rule point, sub-item key (`approximately`, `atMost`), fallback clause, and exception must be directly addressable via a deep URL permalink (e.g., `tr35-numbers.html#misc-patterns-approximately`).
- Links must remain stable across spec revisions.

#### Design & Implementation
1. **Anchor Naming Convention**:
   - Standardized human-readable ID convention:  
     `#<section>-<topic>-<clause_type_or_key>`  
     *Example:* `#misc-patterns-approximately` or `#number-format-fallback-rule-1`

2. **Definition List & List Item Anchors**:
   ```html
   <dl class="spec-items">
     <dt id="misc-patterns-approximately">
       <a href="#misc-patterns-approximately" class="anchor-link">§</a> <code>approximately</code>
     </dt>
     <dd>Indicates an approximate number, such as “~99”.</dd>
   </dl>
   ```

3. **Tooling & Build Pipeline Extensions**:
   - **`AnchorJS` Integration**: Update `archive.js` to attach hover anchor icons (`§` or `#`) to `<dt id="...">` and `<li id="...">` elements.
   - **Anchor Target Registry**: Update `extract-link-targets.js` to track all item-level anchors in `tr35-*.anchors.json`.

---

### 3.2 Pillar 2: Visual Hierarchy & Color Differentiation

#### Requirements
- Clear, distinct visual contrast between **Section Headings** (`h1`–`h4`), **Item Keys / Attribute Tokens** (`approximately`, `atMost`), and **Body Text**.
- Sub-item keys must be rendered in distinct code/badge styling to immediately signify they are data keys/tokens, not section headings.

#### Color & Typography Palette

| Element | Visual Style | CSS Palette (Light Mode) | Example |
| :--- | :--- | :--- | :--- |
| **Section Headings (`h2`, `h3`)** | Bold, sans-serif, border bottom | `#0969da` (Deep Primary Blue) | **Miscellaneous Patterns** |
| **Sub-Item Keys (`dt`, `code`)** | Monospace, code badge, link anchor | `#0550ae` (Token Blue) on `#f6f8fa` bg | `approximately` |
| **DTD Declarations** | Monospace dark syntax container | `#24292e` bg, `#d73a49` tags | `<!ELEMENT miscPatterns ...>` |
| **Body Prose** | Standard serif/sans-serif body | `#1f2328` (Charcoal) | Indicates an approximate number... |

---

### 3.3 Pillar 3: "Bulletability" (Modular Rule Decomposition)

#### Requirements
- Paragraphs must contain **only one primary concept or rule**.
- Multi-faceted logic must be split into structured bulleted lists.
- Exceptions, fallbacks, and special cases must be explicitly segregated into labeled sub-clauses.

#### Standard Rule Blueprint
```markdown
### <Section Title>

* **Base Rule**: [Single concise normative requirement]
* **Exceptions**:
  * **[Exception-1]**: [Specific condition where base rule does not apply]
* **Fallback Behavior**:
  * **[Fallback-1]**: [Action taken when locale data is missing]
* **Special Cases**:
  * **[Special Case]**: [Edge-case handling for specific scripts/locales]
```

---

### 3.4 Pillar 4: Syntax-Highlighted Code & DTD Blocks

#### Requirements
- All XML snippets, DTD element declarations (`<!ELEMENT miscPatterns ...>`), BCP 47 tags, and pseudocode must be rendered in syntax-highlighted code containers with copy controls.

---

### 3.5 Pillar 5: Machine-Readable Conformance Test Fixtures

#### Requirements
- Standalone JSON test fixtures (`docs/ldml/testdata/*.json`) accompanying spec clauses for automated conformance testing across ICU4C, ICU4J, ICU4X, V8, and WebKit.

---

### 3.6 Pillar 6: XML Schema & DTD Cross-Referencing

#### Requirements
- XML elements (e.g. `<miscPatterns>`) automatically link to their official schema definitions in `common/dtd/ldml.dtd`.

---

### 3.7 Pillar 7: Formal Grammars (EBNF / ABNF)

#### Requirements
- Provide formal EBNF/ABNF syntax blocks for pattern languages (Date skeletons, Number patterns, Plural rules).

---

### 3.8 Pillar 8: Version Badging & Feature Deprecation

#### Requirements
- Visual badges for version additions (`[Added in v45]`) and deprecations (`[Deprecated in v46]`).

---

### 3.9 Pillar 9: Interactive Terminology Tooltips & Glossary

#### Requirements
- Inline hover tooltips for technical terms (*skeleton*, *myriad*, *exemplar set*) linked to a master glossary.

---

## 4. Concrete Example: Before vs. After Transformation

### Before (Current Spec Rendering)
> **Miscellaneous Patterns**  
> `<!ELEMENT miscPatterns (alias | (default*, pattern*, special*)) >`  
> The miscPatterns supply additional patterns for special purposes. The currently defined values are:  
> **approximately**  
> &nbsp;&nbsp;&nbsp;&nbsp;indicates an approximate number, such as: “~99”.  
> **atMost**  
> &nbsp;&nbsp;&nbsp;&nbsp;indicates a number or lower...  
*(Problem: Everything is plain black text; `approximately` and `atMost` look like headers but have no links or styling differentiation).*

---

### After (Redesigned with Hierarchy, Colors & Point Linkability)

#### <a id="Miscellaneous_Patterns" href="#Miscellaneous_Patterns">3.10 Miscellaneous Patterns</a> <span class="badge badge-v42">v42</span>

```dtd
<!ELEMENT miscPatterns (alias | (default*, pattern*, special*)) >
<!ATTLIST miscPatterns numberSystem CDATA #IMPLIED >
```

The `<miscPatterns>` element supplies additional patterns for special formatting purposes.

<dl class="spec-item-list">
  <dt id="misc-patterns-approximately">
    <a href="#misc-patterns-approximately" class="anchor-symbol">§</a>
    <code class="token-key">approximately</code>
  </dt>
  <dd>
    Indicates an approximate number format (e.g., “~99”).
    <div class="note-box"><strong>Note:</strong> See ICU-20163 for usage tracking.</div>
  </dd>

  <dt id="misc-patterns-atmost">
    <a href="#misc-patterns-atmost" class="anchor-symbol">§</a>
    <code class="token-key">atMost</code>
  </dt>
  <dd>
    Indicates an upper-bound maximum format (e.g., “≤99”).
  </dd>

  <dt id="misc-patterns-atleast">
    <a href="#misc-patterns-atleast" class="anchor-symbol">§</a>
    <code class="token-key">atLeast</code>
  </dt>
  <dd>
    Indicates a lower-bound minimum format (e.g., “99+”).
  </dd>
</dl>

---

## 5. Migration & Implementation Plan

1. **Phase 1: Infrastructure & Build Pipeline (Weeks 1–2)**
   - Update `tools/scripts/tr-archive/archive.js` and `tr35.css` for code syntax highlighting, deep anchor markers, visual color hierarchy, version badges, and tooltip styles.
2. **Phase 2: Authoring Guidelines & Test Harness (Weeks 2–3)**
   - Update `docs/ldml/README.md` for specification contributors.
   - Establish JSON schema for normative test fixtures (`docs/ldml/testdata/`).
3. **Phase 3: Progressive Specification Refactoring (Weeks 3–6)**
   - Incremental refactoring of UTS #35 Parts 1–9.
