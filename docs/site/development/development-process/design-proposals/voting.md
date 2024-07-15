---
title: Voting
---

# Voting

We have been trying to tune the voting process, see [2260](http://www.unicode.org/cldr/bugs-private/locale-bugs-private?findid=2260). The goal is to balance stability with flexibility.

## Issues

1. Occurrence of "draft" in country locales: http://www.unicode.org/cldr/data/dropbox/misc/draft\_in\_country\_locales.txt
2. And non-country locales: http://www.unicode.org/cldr/data/dropbox/misc/draft\_in\_noncountry\_locales.txt
3. Gratuitious changes in country locales: pt\_PT has lots of such from pt (= pt\_BR)
4. We want people to be able to make fixes where needed (it is frustrating to request a fix, but not have it confirmed because people don't look at it)
5. But we don't want to have "wiki-battles": how do we "protect" against bogus data, while allowing needed changes?

## Suggestions

1. Set a higher bar
	1. on changes to "critical" locales (union of principal's main tiers, intersected with those that are fleshed out well)
2. http://www.unicode.org/cldr/data/charts/supplemental/coverage\_goals.html
3. on country locales.
4. Allow multiple separate votes for TC organizations for non-critical locales. For Vetter status, two sets of eyes should be sufficient. Downside is "deck-stacking".
5. Vote on "logical groups" (eg sets of Months) as a whole.
6. Show country locale differences as "alts".
7. Save votes for "active members" across releases. See [2095](http://www.unicode.org/cldr/bugs-private/locale-bugs-private/design?id=2095;_=).
8. not feasible for this release.

## Background

Our current voting process is at http://cldr.unicode.org/index/process#TOC-Draft-Status-of-Optimal-Field-Value

The key points are:

- For each value, each organization gets a vote based on the *maximum* (not cumulative) strength of the votes of its users who voted on that item.
- If there is a dispute (votes for different values) within an organization, then the majority vote for that organization is chosen. If there is a tie, then no vote is counted for the organization.
- Let **O** be the optimal value's vote, **N** be the vote of the next best value, and **G** be the number of organizations that voted for the optimal value.
- Assign the draft status according to the first of the conditions below that applies:

| Resulting Draft Status | Condition |
|---|:---:|
| *approved - critical loc* | **O ≥ 8** and O &gt; N |
| *approved - non-critical loc* |   **O ≥ 4** and O &gt; N |
| *contributed* | O ≥ 2 and O &gt; N and G ≥ 2 |
| *provisional* | O ≥ 2 and O ≥ N |
| *unconfirmed* | otherwise |

- If the draft status of the previously released value is better than the new draft status, then no change is made. Otherwise, the optimal value and its draft status are made part of the new release.

In our previous version, *approved* required O ≥ 8. 

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)