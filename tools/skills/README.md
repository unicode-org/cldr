# CLDR Agent Skills

This directory holds reusable "Agent Skills" for working on CLDR — packaged, version-controlled
instructions that any skills-compatible AI coding agent (not just one specific product) can load
on demand.

Skills follow the open [Agent Skills](https://github.com/agentskills/agentskills) format: a folder
containing a `SKILL.md` file with `name` and `description` frontmatter plus instructions, and
optionally `scripts/`, `references/`, or `assets/` subfolders for bundled code or reference
material.

```
tools/skills/
  add-rbnf/
    SKILL.md      # metadata + instructions
  another-skill/
    SKILL.md
    scripts/       # optional: executable code the skill relies on
    references/    # optional: heavier reference material
```

See [CLDR Agent Skills](https://cldr.unicode.org/development/agent-skills) on the CLDR site for how
to use these skills with various agents, and for guidance on adding new ones.

## Adding a new skill

1. Create `tools/skills/<skill-name>/SKILL.md` (`<skill-name>` should match the `name` field:
   lowercase letters, numbers, and hyphens only).
2. Add `name` and `description` frontmatter. The description should state when the skill applies
   (agents use it to decide whether to load the full instructions), not summarize its steps.
3. Write the instructions as plain, agent-agnostic Markdown: shell commands, file paths, and
   review steps any agent with file and shell access can follow. Avoid syntax specific to one
   product (e.g. Claude Code's `$ARGUMENTS` slash-command templating).
4. If the skill needs a helper script or heavy reference doc, put it under `scripts/` or
   `references/` inside the skill's folder rather than inlining it in `SKILL.md`.
