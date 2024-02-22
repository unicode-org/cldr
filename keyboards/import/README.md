# Keyboard imports

The XML files in this directory are importable using the keyboard syntax as follows:

```xml
<import base="cldr" path="techpreview/somefile.xml"/>
```

As an experiment, a naming convention is being attempted:

- _type_`-`_script_`-`_description_`.xml`

Where _type_ is the top level element such as `<keys>`.

So,

- `keys-Zyyy-punctuation.xml` is of [script](https://www.unicode.org/iso15924/iso15924-codes.html) `Zyyy`, aka "Common".


See [tr35-keyboard.md](../../docs/ldml/tr35-keyboards.md#Element_import)

## Copyright

Copyright &copy; 2022 Unicode, Inc.
All rights reserved.
[Terms of use](http://www.unicode.org/copyright.html)
