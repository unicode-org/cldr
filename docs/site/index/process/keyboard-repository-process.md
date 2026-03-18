---
title: 'Keyboard Intake Procedures in CLDR'
---

> Editorial note: Please post feedback on this document to [CLDR-17254]

The goal of this document is to clarify the CLDR TC procedures for onboarding keyboard layouts into the [Unicode Common Locale Data Repository](https://cldr.unicode.org/) (CLDR). The guide for contributing keyboards is currently in development.

The keyboard section of the repository provides data for keyboard layouts in a stable and machine-readable format. The most significant benefit of hosting the keyboard data in CLDR for vendors is that they already use CLDR as a trusted source of locale data and will now be able to also have a trusted source for keyboard layouts. Through CLDR’s policies that establish the credibility of its data content, and its standardization of formats to represent the data, including the new LDML format for keyboard layouts, CLDR lowers the barrier for vendors to include keyboards in their products.

This keyboard data will serve as a bridge between language communities and industry:

* Language communities can express their input method needs in a single file format by engaging with Unicode, rather than having to outreach to every system or device vendor individually.  
* Keyboard users will benefit from a unified and consistent typing experience across all platforms that consume the CLDR keyboard data.  
* Vendors and other implementers can utilize the repository to extend their language coverage with keyboards known to be relevant to language communities. The one-time up front investment to implement the LDML format will rapidly pay for itself in comparison to seeking resources and expertise for each language individually.

Which keyboards should be installed and enabled by default per given language is a part of locale data and out of scope for the keyboard repository itself.

## Relationship to Keyman Repository

The [Keyman repository](https://github.com/keymanapp/keyboards) currently hosts thousands of keyboards in its native Keyman formats, as well as a growing number of LDML files. The aim of the Keyman Repository is broader than CLDR. SIL’s goal with the Keyman repository is to encourage broad contributions of keyboard data from the community to Keyman. CLDR on the other hand is focused on ensuring that languages included in CLDR can define their input needs in a stable and machine-readable format for vendors to consume. As such, the Keyman repository of layouts is expected to be more open, and a good staging place for LDML keyboard layout files to be tested by users and readied for submission to CLDR.

While usage data from Keyman can be used to support a request for inclusion of a keyboard layout in the CLDR, it is not a requirement for a proposed layout to have been in the Keyman repository first.

## Contribution and Release cycle

For overview of the contribution and release cycle, see Figure 1\.

![Figure 1: Keyboard Process](/images/keyboard-process.png)

Figure 1\. Overview of the contribution and release cycle

### Submitting LDML

Contributions to the keyboard repository are accepted in the form of pull requests (PR) adding or modifying LDML files in the `keyboards/3.0` folder of the [CLDR](https://github.com/unicode-org/cldr) repository. The LDML specification for keyboard layouts is [Part 7 of UTS \#35](https://www.unicode.org/reports/tr35/tr35-keyboards.html).

Anyone is welcome to create or propose changes to a keyboard layout for any language and any script and submit it for consideration or propose changes to any existing layout in the repository, subject to stability and requirement policies described below.

Unicode CLDR is an open-source project. To contribute to a Unicode Consortium repository, a Contributor License Agreement (CLA) is required. See [CONTRIBUTING.md](https://github.com/unicode-org/cldr/blob/main/CONTRIBUTING.md#contributor-license-agreement) for more information.

### Group Review

The [Keyboard Working Group](/index/keyboard-workgroup) (KBD) will review the proposed keyboard layout data and work with the contributor to address any issues. The [minimum layout requirements](#requirements-on-keyboard-layouts-in-cldr) and policies governing submissions are outlined below.

Once the group is satisfied with the PR, it will recommend the [CLDR Technical Committee](/cldr-tc) to include the keyboard layout data in the repository. Normally, the [draft status](https://www.unicode.org/reports/tr35/#Attribute_draft) of a new keyboard will be *contributed*. The layout must be included in the repository prior to the data freeze deadline for the target CLDR release. This is to ensure that KBD and the TC have sufficient time to address any public feedback, which could include removing the layout altogether or postponing it to a later release.	

### Contributed vs Approved

Once released, layouts will normally stay in CLDR as *contributed* for at least two release cycles (i.e. one year). During this period, the stability of the keyboard layout is not guaranteed. CLDR will accept feedback from users and vendors on the released keyboard layout, which can result in changes to the layout, including removal of keys or other breaking changes. If no breaking changes are enacted during two release cycles, the keyboard layout may progress from *contributed* to *approved*. Once *approved*, stability of the layout is enforced with only additive or corrective changes made to the layout going forwards.

If a breaking change is introduced while the layout is *contributed*, it will remain *contributed* until a full release cycle passes without further breaking changes to the layout. Breaking changes to *approved* keyboard layouts will require a new layout to be created; see **Stability policies** for a definition of breaking changes.  
KBD will be responsible for determining what constitutes a breaking change, an additive change, and a corrective change. Keyboard proposals may include arguments for or against promoting a keyboard to *approved* status.

### Vendor Intake

Once a keyboard is part of the CLDR, it will become available to CLDR consumers. Vendors are recommended to include *contributed* keyboards in their systems and are encouraged to relay any feedback from users to KBD. This is critical for providing stability guarantees while ensuring keyboard layouts serve the needs of their user communities.

Vendors are not required to distinguish *contributed* and *approved* keyboard layouts in the user interface. The status is an indication to vendors on the expected stability of the layouts.

## Requirements on keyboard layouts in CLDR

Before a keyboard layout can be included in the CLDR, several requirements and quality checks need to be satisfied. Note that some of the checks for inclusion in CLDR are stricter than the requirements of the LDML specification itself.

1. The XML file must use UTF-8 encoding.  
2. The keyboard layout needs to conform to the XML schema.  
3. All locales listed in the keyboard layout must already exist in CLDR with [at least a “core” level](https://www.unicode.org/reports/tr35/tr35-info.html#coverage-levels).  
   1. The keyboard should cover the exemplars for those locales.  
4. All locale ids must be minimized per CLDR likely subtags rules.  
5. Only imports with base="cldr" are allowed.  
6. The keyboard layout cannot contain \<special\> elements, attributes in custom namespaces or custom namespace declarations.  
7. The keyboard layout cannot contain \<forms\> element. Use one of the implied forms.  
8. Identifiers must match UTS\#46  
9. All referenced ids must be defined in the file or any of the imported files.  
10. Unique and descriptive info/@name must be provided.  
11. info/@layout and info/@indicator must be provided.  
12. All codepoints must be assigned in a released version of Unicode, or expected to be released in Unicode by the time of the CLDR release.  
13. All codepoints must be in the normalization form C.  
14. settings/@normalization="disabled" is not allowed.  
15. Codepoints in private user areas (PUA) are not allowed. Use markers for internal states.  
16. Codepoints in DoNotEmit.txt are not allowed in output.  
17. The keyboard layout must include a hardware layout.  
18. No hardware row can contain more keys than defined in the form.

### Layout identification

The name of the LDML file itself is only an implementation detail of the CLDR repository and not intended to be shown to users. Vendors can choose to store the layout data in a different way, including one that does not require file names. Layout selection for users should rely solely on the metadata included in the layout files.

The working group can adjust the file names as it sees fit. The current convention is "{primary language id}-{info/@layout}\[-{further distinguishing label if needed}\].xml"

info/@layout should not conflict with BCP-47 subtag registry names for variants. 

The name in the metadata i.e. info/@name is guaranteed to be unique across all versions of the keyboard repository.

## Acceptance criteria

CLDR is not meant as an experimental platform for keyboard layouts. For experimentation, development, and testing, other repositories, such as Keyman, are recommended. Keyboard authors may also opt to self-publish their layouts on private websites or platforms such as GitHub.

Keyboard layouts submitted to CLDR are meant to have been community tested and intended to satisfy the text input needs of their target community as defined by the keyboard spec. Ways to demonstrate these criteria include but are not limited to:

1. a number of users of the layout on existing systems or in other repositories;  
2. a national or an international standard which the layout conforms to;  
3. a submission done by or endorsed by a government organization or a national body.

## Stability policies

Once a layout becomes *approved*, it will not be removed from future releases of the keyboard repository (except for legal reasons). Only additive or corrective changes can be made, such as:

* adding output to a previously unused key or sequence of keys (including adding new flicks, long press keys and appending to multitap keys)  
* adding new forms, transforms, display  
* adding new locales to the list of supported locales  
* renaming ids and variables  
* changing metadata i.e. info values, including the unique info/@name  
* changing the order of supported locales  
* changing the primary locale i.e. keyboard3/@locale as long as it stays in the list of supported locales  
* changing the order of long press keys  
* changing display values for keys  
* removing or correcting invalid codepoints or otherwise invalid output  
  * includes changes in transforms to update a new DoNotEmit.txt value with a preferred alternative

When such changes are accepted to a layout in CLDR, we will generally follow [semver.org](http://semver.org) naming practice within the constraint of breaking changes requiring a new name.

Breaking changes to approved keyboards requiring a new layout file with a new name include:

* removing a locale from the list of supported locales  
* adding new rows or columns to an already existing form  
* changing or removing existing output of a key or sequence of keys unless invalid  
* changing the order of multitap keys  
* changing the flick directions

When breaking changes are needed, a new keyboard layout with a new name has to be created.


[CLDR-17254]: https://unicode-org.atlassian.net/browse/CLDR-17254
