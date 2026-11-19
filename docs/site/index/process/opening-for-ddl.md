---
title: "Opening for DDL"
---

This page describes the _process_ of opening the Survey Tool for a DDL submission phase.
A DDL submission phase is a form of [Extended Submission](/translation/getting-started/survey-tool-phases#extended-submission).
In this example, the `main` branch of CLDR is on version **50** (that is, development towards version 50), while the Survey Tool will be opening for version **51**.

During the period between DDL opening and the normal shakedown for v51, the Survey Tool is open only for DDL (non-TC) locales.

The Survey Tool is set to v51 during this time so that there is a seamless transition when the regular v61 phase opens.

## New DDL Branch

- Copy `main` to a new branch, `ddl/v51`

This new branch doesn't need the normal SBRS steps for a "new version" yet.
That is, don't update `GEN_VERSION`, or the DTD versions, or `ToolConstants.java`.
Version 50 hasn't shipped (yet), so our tooling would be confused if the code was set to version 51.
For example, the tooling would attempt to checkout version 50 from git.

### What goes where

- `main` remains the branch for all v50 work.
  - General development, code improvements
  - Time zone / Currency / Supplemental periodic updates
- `ddl/v51` is _only_ for changes targetting the DDL opening, that is:
  - New locale additions
  - Locale specific supplemental data changes, that is, supplemental data to support a new locale
  - onboarding changes for organization (`Organization.java`, `Locales.txt`
  - minimal code changes:
	- bug fixes and test fixes to support the locale intake
	- DDL-focussed UI improvements
	
Backporting and cherry picking should be minimal:
- Generally, do **not** backport new locales (or associated supplemental) from ddl/v51 to main v50.
They aren't needed, they won't be part of the v50 shipment, and they will cause coverage and other disruptions.
- If business-as-usual supplemental data updates are made in v50, such as time zone / currencies / etc, these can be forward-ported with care to the ddl branch, but must be done cautiously to avoid disruption. (See [Testing](#testing).)

## Survey Tool: Configuring and Opening

### Configuration

- Update `cldr.properties` as below:

```properties
# Note that all versions are MAJOR NUMBERS ONLY.
# This is important, because it marks which version last had votes.
CLDR_LASTVOTEVERSION=49
# CLDR_OLDVERSION does not seem to be used productively. May revisit it. It does signal
# the situation accurately, and could be used for documentation or instructions.
CLDR_OLDVERSION=50
# The new version: which table the votes end up in
CLDR_NEWVERSION=51
# The general phase of the Survey Tool will remain READONLY.
# Only DDL locales will be open, others remain closed.
CLDR_PHASE=READONLY
# DDL locales (Non-TC) enter SUBMIT phase.
CLDR_EXTENDED_PHASE=SUBMIT
# Inform users
CLDR_HEADER=CLDR v61 is open for DDL only
```

### Testing

Smoketest cannot be used to test the ddl branch, because smoketest only builds from `main`.
To test, use the cldr-staging server with appropriate configuration changes, from a ddl/v50 commit.

### Push to Production

Following a successful test on cldr-staging, that same commit may be pushed directly to production.

## DDL-WG Delegation

- DDL-WG is allowed to triage ddl-v51 tickets, new locales/orgs/submitters and to authorize them
- DDL-WG may make code changes as well on this branch. Noting that they will be reviewed again before they go back to main.

## From v50 ship to v51 shakedown

v50 can ship without any dependencies on work done in v51.

### Mergeback

- Preparation:  Cherry pick or merge from main forward to the `ddl/v51` branch.
  The purpose here is to minimize conflicts later.
  
- Merge back any locale and supplemental changes to main, and any trivial code changes, with usual 
  PR review process by TC.
  
- Code changes will need to be a separate PR.  This might be better done BEFORE merging back the ddl branch.

- No vetting will get committed. There's no vxml process at this point.

### Temporary closure

Survey tool will need to be closed temporarily before shakedown

## v51 shakedown

In v51 shakedown, ALL locales, including DDL are opened, and the process continues as normal.
