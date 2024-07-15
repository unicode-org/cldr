---
title: Secular/neutral eras
---

# Secular/neutral eras

|   |   |
|---|---|
| Author | Peter Edberg |
| Date | 2013-04-23,24 |
| Status | Proposal |
| Feedback to | pedberg (at) apple (dot) com |
| Bugs | - [#649](http://unicode.org/cldr/trac/ticket/649) Write up PRI on secular forms of Eras (eg CE)<br /> - [#1574](http://unicode.org/cldr/trac/ticket/1574) Calendar-gregorian era : Add "CE" and "BCE" as additional alternate era terms<br /> - [#4656](http://unicode.org/cldr/trac/ticket/4656) Calendar-gregorian era : Add "CE" and "BCE" as additional alternate era terms  |

Currently for gregorian we have the following in "root":

```
<eras>
    <eraNames>
        <alias source="locale" path="../eraAbbr"/>
    </eraNames>
    <eraAbbr>
        <era type="0">BE</era>
        <!-- = 544 BC gregorian. -->
    </eraAbbr>
    <eraNarrow>
        <alias source="locale" path="../eraAbbr"/>
    </eraNarrow>
\</eras>
```

and we have the following in "en", for example:

```
<eras>
    <eraNames>
        <era type="0">Before Christ</era>
        <era type="1">Anno Domini</era>
    </eraNames>
    <eraAbbr>
        <era type="0">BC</era>
        <era type="1">AD</era>
    </eraAbbr>
    <eraNarrow>
        <era type="0">B</era>
        <era type="1">A</era>
    </eraNarrow>
</eras>
```

We need a way that locales can provide data for an alternate secular/neutral era name if their default era name has a religious basis (as with the current en/gregorian/era naming).

Since many locales already use a secular/neutral name as the default for gregorian, this would need to fall back to the default name in the same locale.

The easiest way to do this is just provide \<eras alt="variant">l in locales that need it. A request for the "variant" form will fall back to the default form in the same locale if no "variant" form is present. So in "en" we would add this:

```
<eras alt="variant">
    <eraNames>
        <era type="0">Before Common Era</era>
        <era type="1">Common Era</era>
    </eraNames>
    <eraAbbr>
        <era type="0">BCE</era>
        <era type="1">CE</era>
    </eraAbbr>
</eras>
```

(We could use other names for the alt form such as "secular" or "neutral" but "variant" is more general and already widely supported.)


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)