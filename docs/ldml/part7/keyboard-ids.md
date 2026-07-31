## <a name="Keyboard_IDs" id="Keyboard_IDs" href="#Keyboard_IDs">Keyboard IDs</a>

* There is a set of subtags that help identify the keyboards. Each of these are used after the `"t-k0"` subtags to help identify the keyboards. The first tag appended is a mandatory platform tag followed by zero or more tags that help differentiate the keyboard from others with the same locale code.


### <a name="Principles_for_Keyboard_IDs" id="Principles_for_Keyboard_IDs" href="#Principles_for_Keyboard_IDs">Principles for Keyboard IDs</a>

The following are the design principles for the IDs.

1. BCP47 compliant.
   1. Eg, `en`, `sr-Cyrl`, or `en-t-k0-extended`.
2. Use the minimal language id based on `likelySubtags` (see [Part 1: Likely Subtags](tr35.md#Likely_Subtags))
   1. Eg, instead of `fa-Arab`, use `fa`.
   2. The data is in <https://github.com/unicode-org/cldr/blob/main/common/supplemental/likelySubtags.xml>
3. Keyboard files should be platform-independent, however, if included, a platform id is the first subtag after `-t-k0-`. If a keyboard on the platform changes over time, both are dated, eg `bg-t-k0-chromeos-2011`. When selecting, if there is no date, it means the latest one.
4. Keyboards are only tagged that differ from the "standard for each language". That is, for each language on a platform, there will be a keyboard with no subtags. Subtags with common semantics across languages and platforms are used, such as `-extended`, `-phonetic`, `-qwerty`, `-qwertz`, `-azerty`, …
5. In order to get to 8 letters, abbreviations are reused that are already in [bcp47](https://github.com/unicode-org/cldr/blob/main/common/bcp47/) -u/-t extensions and in [language-subtag-registry](https://www.iana.org/assignments/language-subtag-registry) variants, eg for Traditional use `-trad` or `-traditio` (both exist in [bcp47](https://github.com/unicode-org/cldr/blob/main/common/bcp47/)).
6. Multiple languages cannot be indicated in the locale id, so the predominant target is used.
   1. For Finnish + Sami, use `fi-t-k0-smi` or `extended-smi`
   2. The [`<locales>`](#element-locales) element may be used to identify additional languages.
7. In some cases, there are multiple subtags, like `en-US-t-k0-chromeos-intl-altgr.xml`
8. Otherwise, platform names are used as a guide.

**Examples**

```xml
<!-- Serbian Latin -->
<keyboard3 locale="sr-Latn"/>
```

```xml
<!-- Serbian Cyrillic -->
<keyboard3 locale="sr-Cyrl"/>
```

```xml
<!-- Pan Nigerian Keyboard-->
<keyboard3 locale="mul-Latn-NG-t-k0-panng">
    <locales>
    <locale id="ha"/>
    <locale id="ig"/>
    <!-- others … -->
    </locales>
</keyboard3>
```

```xml
<!-- Finnish Keyboard including Skolt Sami -->
<keyboard3 locale="fi-t-k0-smi">
    <locales>
    <locale id="sms"/>
    </locales>
</keyboard3>
```

* * *

