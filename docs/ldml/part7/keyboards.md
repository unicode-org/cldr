## <a name="Keyboards" id="Keyboards" href="#Keyboards">Keyboards</a>

* The Unicode Standard and related technologies such as CLDR have dramatically improved the path to language support. However, keyboard support remains platform and vendor specific, causing inconsistencies in implementation as well as timeline.


* More and more language communities are determining that digitization is vital to their approach to language preservation and that engagement with Unicode is essential to becoming fully digitized. For many of these communities, however, getting new characters or a new script added to The Unicode Standard is not the end of their journey. The next, often more challenging stage is to get device makers, operating systems, apps and services to implement the script requirements that Unicode has just added to support their language.


However, commensurate improvements to streamline new language support on the input side have been lacking. CLDR’s Keyboard specification has been updated in an attempt to address this gap.

* This document specifies an interchange format for the communication of keyboard mapping data independent of vendors and platforms. Keyboard authors can then create a single mapping file for their language, which implementations can use to provide that language’s keyboard mapping on their own platform.


* Additionally, the standardized identifier for keyboards can be used to communicate, internally or externally, a request for a particular keyboard mapping that is to be used to transform either text or keystrokes. The corresponding data can then be used to perform the requested actions.  For example, a remote screen-access application (such as used for customer service or server management) would be able to communicate and choose the same keyboard layout on the remote device as is used in front of the user, even if the two systems used different platforms.


* The data can also be used in analysis of the capabilities of different keyboards. It also allows better interoperability by making it easier for keyboard designers to see which characters are generally supported on keyboards for given languages.


<!-- To illustrate this specification, here is an abridged layout representing the English US 101 keyboard on the macOS operating system (with an inserted long-press example). -->

For complete examples, see the XML files in the CLDR source repository.

Attribute values should be evaluated considering the DTD and [DTD Annotations](tr35.md#dtd-annotations).

* * *

