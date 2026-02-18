---
title: JSON Format Data
---

# JSON Format Data

**This page is out of date, see** [**CLDR Releases/Downloads: JSON Data**](https://cldr.unicode.org/index/downloads#json-data) **for details of the JSON data.**

The CLDR Technical Committee is pleased to announce a preliminary publishing of the CLDR 22.1 main and supplemental data in JSON format.

In 2011, the committee approved a specification for a standardized JSON format that can used to represent LDML data. Our hope is that by standardizing the format, it will be much easier for Javascript based applications to share and interchange data in this format, rather than having to invent a JSON format that is known only to the specific application.

In addition, we now have a conversion utility that can be used to convert LDML into the JSON format, and this utility ( Ldml2JsonConverter ) will be published as part of the standard CLDR tools collection in the next release. We intend to enhance this utility to provide consumers a way to produce small subsets ( based on XPaths ) that are likely more useful than the full data set.

The CLDR committee welcomes comments on this specification and the data itself. Our hope is that as more people realize that there is a standard format for CLDR data in JSON, it will become the best practice way to use CLDR data in the Javascript environment.

The JSON specification for CLDR data can be found [here](/index/cldr-spec/cldr-json-bindings).

To access the CLDR 22.1 data in the JSON format, click [HERE](https://home.unicode.org/basic-info/projects/#!/repos/cldr-aux/json/22.1/)

