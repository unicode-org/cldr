---
title: "Opening for DDL"
---

This page describes the _process_ of opening the Survey Tool for a DDL submission phase.
A DDL submission phase is a form of [Extended Submission](/translation/getting-started/survey-tool-phases#extended-submission).

In this example, the `main` branch of CLDR is on version **50** (that is, development towards version 50), while the Survey Tool will be opening for version **51**.

During the period between DDL opening and the normal shakedown for v51, the Survey Tool is open only for DDL (non-TC) locales.

The Survey Tool is set to v51 during this time so that there is a seamless transition when the regular v61 phase opens.

## When this process may begin

This process may begin as soon as v49 (in this example) has been released, and that `main`
is already in place for v50. It's important that `main` already be set for v50 so that the experience
will be as close as possible to v50 production.

## Pre-communication

- Send a note out when appropriate to DDL users, with an estimated timeline.

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

Note, smoketest will continue to reflect the v50 development in progress.

### Push to Production

Following a successful test on cldr-staging, that same commit may be pushed directly to production.

## Communication

- Send a note to DDL users noting the opening.
  Include a warning about needing to review votes after the switch to shakedown.

## DDL-WG Delegation

- DDL-WG is allowed to triage ddl-v51 tickets, new locales/orgs/submitters and to authorize them
- DDL-WG may make code changes as well on this branch. Noting that they will be reviewed again before they go back to main.

## From v50 ship to v51 shakedown

- v50 can ship without any dependencies on work done in v51.

### Mergeback

- Preparation:  Cherry pick or merge from main forward to the `ddl/v51` branch.
  The purpose here is to minimize conflicts later.

> Note: When to choose a cherry pick or merge?
> It depends on the kinds of changes that are made, and it is up to the maintainer of the
> ddl branch to determine which route is easiest.

- Major Code changes will need to be a separate PR.
  This might be best done BEFORE merging back the rest of the `ddl/v51` branch, so that those changes
  can receive separate review.

- Merge back any locale and supplemental changes to main, and any trivial code changes, with usual
  PR review process by TC.
  Ideally the entire `ddl/v51` branch would be merged back.

> Note: No vetted data will get committed.
> There's no vxml process at this point, because vetting has not closed.

### Pre-test of v51 shakedown

When everything has been merged back from the `ddl/v51` branch to `main`, it is time
to test again.

- Send a note to DDL users informing them of the upcoming transition and brief downtime.
- Push a test out of `main`'s commit to cldr-staging (as in [testing](#testing))
- Copy production data to cldr-staging
- Check the logs, look for deleted votes due to invalid values. 
  See [CLDR-19570](https://unicode-org.atlassian.net/browse/CLDR-19570) for an example of this log message.
  (The action to take varies. Perhaps users will need a notice sent to them.
   In an extreme case, votes could be altered in sql.)
- This would be a good time to mention keeping a backup of the database.

### Temporary closure

Survey tool will need to be closed temporarily before shakedown.
Having done the testing mentioned above, the closure need only be long enough
to change the overall phase to `SUBMIT`, update the message, and push a commit from `main` to production.

## v51 shakedown

Shakedown is effected by cldr.properties as follows, keeping the version settings the same:

```properties
CLDR_PHASE=SUBMIT
# CLDR_EXTENDED_PHASE is not used in this state, it may be commented out.
#CLDR_EXTENDED_PHASE=..
```

In v51 shakedown, ALL locales, including DDL are opened, and the process continues as normal.

### Post communication

- Send a note to DDL locales indicating that shakedown has commenced, and suggest re-reviewing errors
  and coverage level in case of any changes since the DDL opening.

