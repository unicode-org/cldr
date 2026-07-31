## <a name="POSIX_Elements" id="POSIX_Elements" href="#POSIX_Elements">POSIX Elements</a>

```dtd
<!ELEMENT posix (alias | (messages*, special*)) >
<!ELEMENT messages (alias | ( yesstr*, nostr*)) >
```

The following are included for compatibility with POSIX.

```xml
<posix>
    <posix:messages>
        <posix:yesstr>ja</posix:yesstr>
        <posix:nostr>nein</posix:nostr>
    </posix:messages>
</posix>
```

1. The values for yesstr and nostr contain a colon-separated list of strings that would normally be recognized as "yes" and "no" responses. For cased languages, this shall include only the lower case version. POSIX locale generation tools must generate the upper case equivalents, and the abbreviated versions, and add the English words wherever they do not conflict. Examples:
    * ja → ja:Ja:j:J:yes:Yes:y:Y
    * ja → ja:Ja:j:J:yes:Yes // exclude y:Y if it conflicts with the native "no".
2. The older elements `yesexpr` and `noexpr` are deprecated. They should instead be generated from `yesstr` and `nostr` so that they match all the responses.

So for English, the appropriate strings and expressions would be as follows:

```text
yesstr "yes:y"
nostr "no:n"
```

The generated yesexpr and noexpr would be:

```text
yesexpr "^([yY]([eE][sS])?)"
```

This would match y,Y,yes,yeS,yEs,yES,Yes,YeS,YEs,YES.

```text
noexpr "^([nN][oO]?)"
```

This would match n,N,no,nO,No,NO.

