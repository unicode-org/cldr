## <a name="Grammatical_Features" id="Grammatical_Features" href="#Grammatical_Features">Grammatical Features</a>

* LDML supplies grammatical information that can be used to distinguish localized forms on a per-locale basis. The current data is part of an initial phase; the longer term plan is to add structure to permit localized forms based on these features, starting with measurement units such as the dative form in Serbian of “kilometer”. That will allow unit values to be inserted as placeholders into messages and adopt the right forms for grammatical agreement.


The current data includes the following:

*   There are currently 3 grammatical features found in the [DTD](https://github.com/unicode-org/cldr/blob/main/common/dtd/ldmlSupplemental.dtd#1254): Gender, Case, Definiteness
*   There are mappings from supported locales to grammatical features they exhibit in the file [grammaticalFeatures.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/grammaticalFeatures.xml). Note that this is supplemental data, so the inheritance to the available locales needs to be done by the client.

Note that the CLDR plural categories overlap some of these features, since some languages use case and other devices to change words based on the numeric values.

