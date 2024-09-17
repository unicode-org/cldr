# UN Literacy Data (CLDR BRS)



1. Goto <https://data.un.org/Data.aspx?d=POP&f=tableCode:31>
2. On the left tab under filters:
    - under **Area** choose **Total**
    - under **Sex** choose **Both Sexes**
3. Click the **Download** button and choose **XML**
4. Save the resultant XML file as `tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/external/un_literacy.xml`
5. Now you can run `AddPopulationData`

> Note: If the format changes, you'll have to modify the `AddPopulationData.loadUnLiteracy()` method.
