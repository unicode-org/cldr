## <a name="Reference_Elements" id="Reference_Elements" href="#Reference_Elements">Reference Element</a>

(Use only in supplemental data; deprecated for ldml.dtd and locale data)

```dtd
<!ELEMENT references ( reference* ) >
<!ELEMENT reference ( #PCDATA ) >
<!ATTLIST reference type NMTOKEN #REQUIRED>
<!ATTLIST reference standard ( true | false ) #IMPLIED >
<!ATTLIST reference uri CDATA #IMPLIED >
```

* The references section supplies a central location for specifying references and standards. The uri should be supplied if at all possible. If not online, then an ISBN number should be supplied, such as in the following example:


```xml
<reference type="R2" uri="https://www.ur.se/nyhetsjournalistik/3lan.html">Landskoder på Internet</reference>
<reference type="R3" uri="URN:ISBN:91-47-04974-X">Svenska skrivregler</reference>
```

