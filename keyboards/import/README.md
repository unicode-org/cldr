# Keyboard imports

The XML files in this directory are importable using the keyboard syntax as follows:

```xml
<import base="cldr" path="45/somefile.xml"/>
```

This is the naming convention being used:

- _type_`-`_script_`-`_description_`.xml`

Where _type_ is the top level element such as `<keys>`.

So,

- `keys-Zyyy-punctuation.xml` is of [script](https://www.unicode.org/iso15924/iso15924-codes.html) `Zyyy`, aka "Common".

See [tr35-keyboard.md](../../docs/ldml/tr35-keyboards.md#Element_import)

## Copyright and License

See the top level [README.md](../../README.md#copyright--licenses)
