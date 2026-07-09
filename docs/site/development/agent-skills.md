---
title: CLDR Agent Skills
---

# CLDR Agent Skills

CLDR keeps reusable AI agent instructions ("skills") in [`tools/skills/`](https://github.com/unicode-org/cldr/tree/main/tools/skills)
under the open [Agent Skills](https://github.com/agentskills/agentskills) format. A skill is a
folder containing a `SKILL.md` file with `name` and `description` metadata plus instructions for
a specific CLDR task, and optionally bundled scripts or reference material. Because the format is
open and supported by multiple AI coding agents, a skill written once works across any
skills-compatible agent — it isn't tied to a single vendor's tool.

## Why skills instead of ad-hoc prompts

- **Repeatable**: multi-step CLDR tasks (e.g. authoring RBNF rules for a new language) become a
  consistent, auditable procedure instead of a one-off prompt.
- **Portable**: the instructions live in plain Markdown in the repository, so they work the same
  way regardless of which agent or tool a contributor uses.
- **Reviewable**: since skills are checked into `tools/skills/`, changes to them go through the
  same PR review as any other code or documentation change.

## Using a skill

No agent — including Claude Code — automatically discovers skills under `tools/skills/` just
because they're checked into the repository. Each agent product looks for skills in its own
conventional location (for example, a personal `~/.claude/skills/` folder), not in an arbitrary
repo-relative directory. So `tools/skills/` works as a shared, version-controlled source of truth,
but you generally need to point your agent at it explicitly:

- Agents with native Agent Skills support can be configured to also load skills from
  `tools/skills/`, or you can copy/symlink a skill into that agent's own skills folder.
- For agents with their own custom-command mechanism (for example, Claude Code's
  `.claude/commands/`), this repository may keep a thin pointer file that just tells the agent to
  follow the corresponding `tools/skills/<skill-name>/SKILL.md` instructions, so existing
  shortcuts keep working without duplicating content.
- Otherwise, you can simply ask your agent to read and follow `tools/skills/<skill-name>/SKILL.md`
  directly.

## Available skills

| Skill | Purpose |
|---|---|
| [`add-rbnf`](https://github.com/unicode-org/cldr/tree/main/tools/skills/add-rbnf) | Add or expand Rule Based Number Format (RBNF) rules so numbers can be spelled out as words for a language. |

## Adding a new skill

See the [README in `tools/skills/`](https://github.com/unicode-org/cldr/tree/main/tools/skills)
for the folder layout and conventions to follow when adding a new skill.
