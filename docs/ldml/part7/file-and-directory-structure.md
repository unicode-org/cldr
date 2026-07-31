## <a name="File_and_Directory_Structure" id="File_and_Directory_Structure" href="#File_and_Directory_Structure">File and Directory Structure</a>

* In the future, new layouts will be included in the CLDR repository, as a way for new layouts to be distributed in a cross-platorm manner. The process for this repository of layouts has not yet been defined, see the [CLDR Keyboard Workgroup Page][keyboard-workgroup] for up-to-date information.

* Layouts have version metadata to indicate their specification compliance versi​​on number, such as `45`. See [`cldrVersion`](tr35-info.md#version-information).

```xml
<keyboard3 xmlns="https://schemas.unicode.org/cldr/45/keyboard3" conformsTo="45"/>
```

> _Note_: Unlike other LDML files, layouts are designed to be used outside of the CLDR source tree.  As such, they do not contain DOCTYPE entries.
>
> DTD and Schema (.xsd) files are available for use in validating keyboard files.

* The filename of a keyboard .xml file does not have to match the BCP47 primary locale ID, but it is recommended to do so. The CLDR repository may enforce filename consistency.

### <a name="Extensibility" id="Extensibility" href="#Extensibility">Extensibility</a>

For extensibility, the `<special>` element will be allowed at nearly every level.

See [Element special](tr35.md#special) in Part 1.

