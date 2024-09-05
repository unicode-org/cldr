---
title: New BCP47 Extension T Fields
---

# New BCP47 Extension T Fields

## Proposed Additions

BCP47 language tags can use Extension T for identifying transformed content, or indicating requests for transformed content, as described in [*rfc6497*](http://tools.ietf.org/html/rfc6497). If you have any comments on proposals, please circulate them on the cldr-users mailing list. Instructions for joining are at [cldr list](http://www.unicode.org/consortium/distlist.html#cldr_list). 

*There are no proposed additions at this time.*

## Approved Proposals

The following proposals have been approved for the next version of the BCP47 T Extension registry, after being distributed for public review.

### Approved May 9, 2012

The following proposal was distributed for public review on April 26, 2012. The longer descriptions in \<!-- … --> can be used as a basis for enhancing the XML description or for the LDML spec.

key = i0 (IME)

\<type name="handwrit" description="Handwriting input" since="21.0.2"/>

&emsp;\<!-- For LDML spec: Used when the only information known (or requested) is that the text was (or is to be) converted using an handwriting input.-->

\<type name="pinyin" description="Pinyin input" since="21.0.2"/>

&emsp;\<!-- For LDML spec: Pinyin input, an input method to input simplified Chinese characters. For background information, see http://en.wikipedia.org/wiki/Pinyin\_method-->

\<type name="und" description="The choice of input method is not specified." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Used when the only information known (or requested) is that the text was (or is to be) converted using an input method engine.-->

\<type name="wubi" description="Wubi input" since="21.0.2"/>

&emsp;\<!-- For LDML spec: Wubi input, an input method to input simplified Chinese characters. For background information, see http://en.wikipedia.org/wiki/Wubi\_method-->

key = k0 (keyboard)

\<type name="osx" description="Mac OSX keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout from the Mac OSX operating system.-->

\<type name="windows" description="Windows keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout from the Windows operating system.-->

\<type name="chromeos" description="ChromeOS keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout from the ChromeOS operating system.-->

\<type name="android" description="Android keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout from the Android operating system.-->

\<type name="googlevk" description="Google virtual keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout from the Google cloud virtual keyboard system.-->

\<type name="101key" description="101 key layout." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout with 101 keys where the default has a different number of keys.-->

\<type name="102key" description="102 key layout." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout with 102 keys where the default has a different number of keys.-->

\<type name="dvorak" description="Dvorak keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A Dvorak keyboard layout. See http://en.wikipedia.org/wiki/Dvorak\_Simplified\_Keyboard-->

\<type name="dvorakl" description="Dvorak left-hand keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A Dvorak left-handed keyboard layout. See http://en.wikipedia.org/wiki/File:KB\_Dvorak\_Left.svg-->

\<type name="dvorakr" description="Dvorak right-hand keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A Dvorak right-handed keyboard layout. See http://en.wikipedia.org/wiki/File:KB\_Dvorak\_Right.svg-->

\<type name="el220" description="Greek 220 keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: The Greek 220 keyboard layout. See http://www.microsoft.com/resources/msdn/goglobal/keyboards/kbdhela2.html-->

\<type name="el319" description="Greek 319 keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: The Greek 319 keyboard layout. See ftp://ftp.software.ibm.com/software/globalization/keyboards/KBD319.pdf-->

\<type name="extended" description="Extended keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard that has been enhanced with a large number of extra characters.-->

\<type name="isiri" description="Persian ISIRI keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Persian ISIRI keyboard. Based on ISIRI 2901:1994 standard. See http://behdad.org/download/Publications/persiancomputing/a007.pdf-->

\<type name="nutaaq" description="Inuktitut Nutaaq keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Inuktitut Nutaaq keyboard. See http://www.pirurvik.ca/en/webfm\_send/15-->

\<type name="legacy" description="Legacy keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard that has been replaced with a newer standard but is kept for legacy purposes.-->

\<type name="lt1205" description="Lithuanian standard keyboard (LST 1205:1992)." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Lithuanian standard keyboard. Based on the LST 1205:1992 standard. See http://www.kada.lt/litwin/-->

\<type name="lt1582" description="Lithuanian standard keyboard (LST 1582:2000)." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Lithuanian standard keyboard. Based on the LST 1582:2000 standard. See http://www.kada.lt/litwin/-->

\<type name="patta" description="Thai Pattachote keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A Thai Pattachote keyboard layout. This is a less frequently used layout in Thai (Kedmanee layout is more popular). See http://www.nectec.or.th/it-standards/keyboard\_layout/thai-key.htm-->

\<type name="qwerty" description="Qwerty keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A QWERTY-based keyboard or one that approximates QWERTY in a different script.-->

\<type name="qwertz" description="Qwertz keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A QWERTZ-based keyboard or one that approximates QWERTZ in a different script.-->

\<type name="var" description="Variant keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A keyboard layout with small variations from the default.-->

\<type name="viqr" description="Vietnamese VIQR layout." since="21.0.2"/>

&emsp;\<!-- For LDML spec: The VIQR layout for Vietnamese, based on http://tools.ietf.org/html/rfc1456.-->

\<type name="ta99" description="Tamil 99 keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: The Tamil99 layout for Tamil. See http://www.tamilvu.org/Tamilnet99/annex1.htm-->

\<type name="colemak" description="Colemak keyboard layout." since="21.0.2"/>

&emsp;\<!-- For LDML spec: The Colemak keyboard layout. The Colemak keyboard is an alternative to the QWERTY and dvorak keyboards. http://colemak.com/-->

\<type name="600dpi" description="Keyboard for a 600 dpi device." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Keyboard for a 600 dpi device.-->

\<type name="768dpi" description="Keyboard for a 768 dpi device." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Keyboard for a 768 dpi device.-->

\<type name="azerty" description="Azerty keyboard." since="21.0.2"/>

&emsp;\<!-- For LDML spec: A AZERTY-based keyboard or one that approximates AZERTY in a different script.-->

\<type name="und" description="The vender for the keyboard is not specified." since="21.0.2"/>

&emsp;\<!-- For LDML spec: Used when the only information known (or requested) is that the text was (or is to be) converted using an keyboard.-->

### Approved April 4, 2012

The following proposal was distributed for public review on March 26, 2012. The CLDR committee concluded that the best way of representing different kinds of identifiers for use in requesting input transforms was to have separate fields, and not subfields of the existing m0. Using different 'namespaces' allows users of the T extensions to ignore types of subfields that are not relevant, and to group related subfields in an organized fashion. On that basis, the following additions to the BCP47 T Extension registry (see [bcp47/transform.xml](http://unicode.org/repos/cldr/trunk/common/bcp47/transform.xml)) were approved. The longer descriptions in bullets can be used as a basis for enhancing the XML description or for the LDML spec.

\<key extension="t" name="k0" description="Keyboard transform" since="21.0.2">

- *Used to indicate a keyboard transformation, such as one used by a client-side virtual keyboard. The first subfield in a sequence would typically be a 'platform' or vendor designation.*

\<key extension="t" name="i0" description="Input Method Engine transform" since="21.0.2">

- *Used to indicate an input method transformation, such as one used by a client-side input method. The first subfield in a sequence would typically be a 'platform' or vendor designation.*

\<key extension="t" name="t0" description="Machine Translation" since="21.0.2">

- *Used to indicate content that has been machine translated, or a request for a particular type of machine translation of content. The first subfield in a sequence would typically be a 'platform' or vendor designation.*

\<type name="und" description="The choice of machine translation is not specified." since="21.0.2"/>

- *Used when the only information known (or requested) is that the text was machine translated.*

\<key extension="t" name="x0" description="Private Use" since="21.0.2">

- *Used for implementation-specific transforms. All subfields consistent with* [*rfc6497*](http://tools.ietf.org/html/rfc6497) *(that is, subtags of 3-8 alphanum characters) are valid, and do not require registration.*

**Note:** RFC6497 interprets transforms that result in content broadly, including speech recognition and other instances where the source is not simply text. For the case of keyboards, the source content can be viewed as keystrokes, but may also be text—for the case of virtual web-based keyboards. For example, such a keyboard may translate the text in the following way. Suppose the user types a key that produces a "W" on a qwerty keyboard. A web-based tool using an azerty virtual keyboard can map that text ("W") to the text that would have resulted from typing a key on an azerty keyboard, by transforming "W" to "Z". Such transforms are in fact performed in existing web applications. The standardized extension can be used to communicate, internally or externally, a request for a particular keyboard mapping that is to be used to transform either text or keystrokes, and then use that data to perform the requested actions.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)