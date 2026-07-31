# Master Prompt: Generating Unicode Technical Standard #35 (LDML) in mdBook from Scratch

You are an expert technical documentation architect and software engineer specializing in Unicode technical standards, CLDR, Markdown, and mdBook.

Your task is to take the 11 raw Unicode Technical Standard #35 (LDML) specification files (`tr35*.md` or source HTML/Markdown) from the CLDR repository and transform them into a modern, accessible, world-class mdBook publication suite while strictly **preserving 100% of the normative technical meaning, rules, DTD semantics, EBNF grammars, and examples**, and **maintaining the original 11-file structure** so that it remains simple and familiar for Unicode/CLDR specification editors.

---

## 1. Project Directory & File Structure

Maintain the canonical 11 specification files directly in `docs/ldml/`:

```
docs/ldml/
├── book.toml                          # mdBook configuration
├── SUMMARY.md                         # Table of contents linking all 11 parts
├── theme/
│   ├── custom.css                     # Executive specification styles & high-contrast themes
│   └── sidebar-subsections.js         # In-page subsection navigation & scroll-spy
├── tr35.md                            # Part 1: Core
├── tr35-general.md                    # Part 2: General
├── tr35-numbers.md                    # Part 3: Numbers
├── tr35-dates.md                      # Part 4: Dates & Times
├── tr35-collation.md                  # Part 5: Collation
├── tr35-info.md                       # Part 6: Supplemental Metadata
├── tr35-keyboards.md                  # Part 7: Keyboards
├── tr35-personNames.md                # Part 8: Person Names
├── tr35-messageFormat.md              # Part 9: MessageFormat
├── tr35-modifications.md              # Appendix A: Modifications
└── tr35-acknowledgments.md            # Appendix B: Acknowledgments
```

---

## 2. Table of Contents (`SUMMARY.md`)

Create `docs/ldml/SUMMARY.md` linking the 11 specification files directly:

```markdown
# Summary

- [Part 1: Core](tr35.md)
- [Part 2: General](tr35-general.md)
- [Part 3: Numbers](tr35-numbers.md)
- [Part 4: Dates & Times](tr35-dates.md)
- [Part 5: Collation](tr35-collation.md)
- [Part 6: Supplemental Metadata](tr35-info.md)
- [Part 7: Keyboards](tr35-keyboards.md)
- [Part 8: Person Names](tr35-personNames.md)
- [Part 9: MessageFormat](tr35-messageFormat.md)

---

- [Appendix A: Modifications](tr35-modifications.md)
- [Appendix B: Acknowledgments](tr35-acknowledgments.md)
```

---

## 3. Configuration (`book.toml`)

Create `docs/ldml/book.toml`:

```toml
[book]
title = "Unicode Technical Standard #35: Unicode Locale Data Markup Language (LDML)"
authors = ["Unicode Consortium", "CLDR Technical Committee"]
description = "Unicode Technical Standard #35: LDML specification rendered with mdBook"
language = "en"
src = "."

[build]
build-dir = "book"
create-missing = false

[output.html]
git-repository-url = "https://github.com/unicode-org/cldr"
edit-url-template = "https://github.com/unicode-org/cldr/edit/main/docs/ldml/{path}"
default-theme = "light"
preferred-dark-theme = "navy"
smart-punctuation = true
additional-css = ["theme/custom.css"]
additional-js = ["theme/sidebar-subsections.js"]

[output.html.search]
enable = true
limit-results = 30
use-boolean-and = true
boost-title = 2
boost-hierarchy = 1
boost-paragraph = 1
expand = true
heading-split-level = 3

[output.html.print]
enable = true
page-break = true
```

*Note: Do NOT include unsupported fields such as `multilingual` under `[book]` or `copy-js` under `[output.html]`.*

---

## 4. Strict Markdown & Syntax Rules

1. **No Manual Anchor Tags**:
   * Strip all raw `<a id="...">` or `<a name="...">` tags from markdown headings and text. mdBook automatically generates and manages heading anchors and URL fragments.
2. **Strict Code Fence Pairing & Tagging**:
   * Every code block must start with a valid opening fence (e.g. ````xml`, ````dtd`, ````ebnf`, ````text`) and close with a clean ```` ` (three backticks with no language suffix).
   * Ensure closing fences never have language identifiers (e.g., ````xml` at the end of a block is an error that inverts parser state and corrupts subsequent markdown).
3. **DTD Declarations**:
   * Wrap all DTD element and attribute list definitions (`<!ELEMENT ...>`, `<!ATTLIST ...>`, `<!ENTITY ...>`) in ````dtd ` code blocks.
4. **Prose XML Tag Escaping**:
   * Escape XML tags occurring in prose outside code blocks with inline code backticks (e.g., `` `<territoryAlias>` ``, `` `<numberingSystem>` ``) so CommonMark does not interpret them as unclosed HTML tags.
5. **No Table Fencing**:
   * Ensure HTML tables (`<table>...</table>`) are never enclosed within code block fences (` ````xml <table>... ```` `). Tables must remain raw HTML/Markdown to render natively.
6. **Hierarchical Bullet Subpoints for Definitions**:
   * Convert definition items (such as Number Symbols, Numbering Systems, and Conversion Rules) into structured, indented bullet points rather than disconnected blockquotes (`> ...`):
     ```markdown
     * `decimal`:
       * **Definition**: Separates the integer and fractional part of the number.
     * `group`:
       * **Definition**: Separates clusters of integer digits to make large numbers more legible.
       * **Grouping Sizes**: ...
     ```

---

## 5. Executive Header Cards

Place a standardized executive header card at the top of each of the 11 `.md` files:

```html
<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 3: Numbers</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Mark Davis (Google)</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>
```

---

## 6. Styling & Multi-Theme Support (`theme/custom.css`)

In `theme/custom.css`, ensure:
1. **Monospace Code Typography**: Large, readable code font size (`0.95rem` / `15px`, `line-height: 1.6`) using developer fonts (*JetBrains Mono*, *SF Mono*, *Fira Code*).
2. **High-Contrast Tables**: Prominent header backgrounds (`#0f172a` in dark, `#f1f5f9` in light), `0.85rem 1.15rem` header padding, `0.75rem 1.15rem` cell padding, and alternating zebra striping.
3. **Multi-Theme Sidebar Contrast**: Use `var(--sidebar-fg)` and `var(--sidebar-active)` for sidebar subsection links so they maintain high contrast across **Light, Rust, Coal, Navy, and Ayu** themes.

---

## 7. Dynamic In-Page Sidebar Navigation (`theme/sidebar-subsections.js`)

Implement a lightweight vanilla JavaScript module that:
1. Identifies the active chapter link in `#sidebar`.
2. Gathers all `<h2>` and `<h3>` headings within the active page.
3. Dynamically injects an `<ol class="section subsection-list">` under the active chapter with smooth-scroll navigation.
4. Uses `IntersectionObserver` to highlight the current subsection in the sidebar in real time as the user scrolls.

---

## 8. Verification

Verify the entire build locally:
```bash
cd docs/ldml
mdbook build
mdbook serve -p 3001 --open
```

Confirm that:
- `mdbook build` finishes with exit code `0` and zero warnings.
- The sidebar displays all 11 parts, with in-page subsections appearing dynamically under the active chapter.
- Text contrast is bright and high-contrast in all themes (Light, Rust, Coal, Navy, Ayu).
