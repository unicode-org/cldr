---
title: Supported NumberingSystems
---

# Supported NumberingSystems

Per ticket #3516 and 4097 - we need a way to specify which numbering systems are supported in a particular locale.

We currently only have a single field, that defines the default numbering system for a locale, as follows:

\<defaultNumberingSystem>latn\</defaultNumberingSystem>

There are other categories of numbering systems that should be defined on a per-locale basis, so that programmers can access a certain type of numbering system without necessarily knowing the specific numbering system in place.

This proposal replaces the current "defaultNumberingSystem" field with a series of fields that denotes the different categories of numbering systems that might be desired. Although numbering systems could be categorized in a number of ways, the most common groupings would be as follows:

\<default> - The default numbering system to be used for formatting numbers in the locale.

\<native> - Numbering system using native digits. The "native" numbering system can only be a numeric numbering system, containing the native digits used in the locale.

\<traditional> - The traditional or historic numbering system. Algorithmic systems are allowed in the "traditional" system. 

- May be the same as "native" for some locales, but it may be different for others, such as Tamil or Chinese.
- If "traditional" is not explicitly specified, fall back to "native".

\<finance> - Special numbering system used for financial quantities. If "financial" is not explicitly specified, fall back to "default".

**BCP 47 - Locale keywords**

\<default> - No keyword is required

\<native> - native ( Example: ar-MA-u-nu-native is Arabic locale for Morocco, but using native digits ).

\<traditional> - traditio ( Example: ta-IN-u-nu-trad is Tamil locale for India, using traditional numerals ).

\<finance> - finance ( Example: zh-Hant-TW-u-nu-finance would be Chinese locale in Tradtional Han script for Taiwan, using financial numbers ).

Proposed seed data for numbering systems

----------------
**root.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

    </numberingSystems>

</numbers>
```

**am.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>ethi</traditional>

    </numberingSystems>

</numbers>
```

**ar.xml:**

```
<numbers>

    <numberingSystems>

        <default>arab</default>

        <native>arab</native>

    </numberingSystems>

</numbers>
```

**ar\_DZ.xml:** ( native="arab" would be inherited from the "ar" locale )

```
<numbers>

    <numberingSystems>

        <default>latn</default>

    </numberingSystems>

</numbers>
```

**ar\_MA.xml:**( native="arab" would be inherited from the "ar" locale )

```
<numbers>

    <numberingSystems>

        <default>latn</default>

    </numberingSystems>

</numbers>
```

**ar\_TN.xml:**( native="arab" would be inherited from the "ar" locale )

```
<numbers>

    <numberingSystems>

        <default>latn</default>

    </numberingSystems>

</numbers>
```

**as.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>beng</native>

    </numberingSystems>

</numbers>
```

**bn.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>beng</native>

    </numberingSystems>

</numbers>
```

**bo.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>tibt</native>

    </numberingSystems>

</numbers>
```

**brx.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>deva</native>

    </numberingSystems>

</numbers>
```

**byn.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>ethi</traditional>

    </numberingSystems>

</numbers>
```

**el.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>grek</traditional>

    </numberingSystems>

</numbers>
```

**fa.xml:**

```
<numbers>

    <numberingSystems>

        <default>arabext</default>

        <native>arabext</native>

    </numberingSystems>

</numbers>
```

**gu.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>gujr</native>

    </numberingSystems>

</numbers>
```

**he.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>hebr</traditional>

    </numberingSystems>

</numbers>
```

**hi.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>deva</native>

    </numberingSystems>

</numbers>
```

**hy.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>armn</traditional>

    </numberingSystems>

</numbers>
```

**ja.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>hanidec</native>

        <traditional>jpan</traditional>

        <finance>jpanfin</finance>

    </numberingSystems>

</numbers
```

**ka.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>geor</traditional>

    </numberingSystems>

</numbers>
```

**km.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>khmr</native>

    </numberingSystems>

</numbers>
```

**kn.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>knda</native>

    </numberingSystems>

</numbers>
```

**kok.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>deva</native>

    </numberingSystems>

</numbers>
```

**ku.xml:**

```
<numbers>

    <numberingSystems>

        <default>arab</default>

        <native>arab</native>

    </numberingSystems>

</numbers>
```

**lo.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>laoo</native>

    </numberingSystems>

</numbers>
```

**ml.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>mlym</native>

    </numberingSystems>

</numbers>
```

**mr.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>deva</native>

    </numberingSystems>

</numbers>
```

**mn\_Mong.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>mong</native>

    </numberingSystems>

</numbers>
```

**my.xml:**

```
<numbers>

    <numberingSystems>

        <default>mymr</default>

        <native>mymr</native>

    </numberingSystems>

</numbers>
```

**ne.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>deva</native>

    </numberingSystems>

</numbers>
```

**om.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>ethi</traditional>

    </numberingSystems>

</numbers>
```

**or.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>orya</native>

    </numberingSystems>

</numbers>
```

**pa.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>guru</native>

    </numberingSystems>

</numbers>
```

**pa\_Arab.xml:**

```
<numbers>

    <numberingSystems>

        <default>arabext</default>

        <native>arabext</native>

    </numberingSystems>

</numbers>
```

**ta.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>tamldec</native>

        <traditional>taml</traditional>

    </numberingSystems>

</numbers>
```

**te.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>telu</native>

    </numberingSystems>

</numbers>
```

**th.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>thai</native>

    </numberingSystems>

</numbers>
```

**ti.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>ethi</traditional>

    </numberingSystems>

</numbers>
```

**tig.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>ethi</traditional>

    </numberingSystems>

</numbers>
```

**ur.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>arabext</native>

    </numberingSystems>

</numbers
```

**uz\_Arab.xml:**

```
<numbers>

    <numberingSystems>

        <default>arabext</default>

        <native>arabext</native>

    </numberingSystems>

</numbers>
```

**wal.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>latn</native>

        <traditional>ethi</traditional>

    </numberingSystems>

</numbers>
```

**zh.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>hanidec</native>

        <traditional>hans</traditional>

        <finance>hansfin</finance>

    </numberingSystems>

</numbers
```

**zh\_Hant.xml:**

```
<numbers>

    <numberingSystems>

        <default>latn</default>

        <native>hanidec</native>

        <traditional>hant</traditional>

        <finance>hantfin</finance>

    </numberingSystems>

</numbers
```

The plan is that these fields would NOT be exposed to survey tool, and would only be changeable via ticket submissions in trac.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)