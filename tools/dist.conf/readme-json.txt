The JSON files published here are provided as a reference sample to show the recommended structure for JSON
converted from LDML.  The locales provided represent a sampling of the most widely used locales within CLDR,
those designated as "Established Locales" in the CLDR survey tool process, and in most cases will provide
more data than is actually needed for a given application.

Users who wish to use the standardized JSON format for CLDR should use the new Ldml2JsonConverter tool
provided as part of the tools.zip.  The tool allows you to filter CLDR's data based on draft status,
coverage, and also allows you to create a customized configuration file that you can use to "cherry pick"
only those data items that are needed for your application.

The default configuration file can be found in the tools.zip at tools/java/org/unicode/cldr/json/JSON_config.txt .
The configuration file allows you to specify a series of "sections" each of whose content can be controlled by providing a regular
expression that matches the paths you wish to include in your implementaion.  The default configuration is basically aligned to
the first level of structure under /ldml in the main directory, and the functional areas in the supplemental data.

------------

Usage: Ldml2JsonConverter [OPTIONS] [FILES]
This program converts CLDR data to the JSON format.
Please refer to the following options.
        example: org.unicode.cldr.json.Ldml2JsonConverter -c xxx -d yyy
Here are the options:
-h (help)       no-arg  Provide the list of possible options
-c (commondir)  .*      Common directory for CLDR files, defaults to CldrUtility.COMMON_DIRECTORY
-d (destdir)    .*      Destination directory for output files, defaults to CldrUtility.GEN_DIRECTORY
-m (match)      .*      Regular expression to define only specific locales or files to be generated
-t (type)       (main|supplemental|segments)     Type of CLDR data being generated, main or supplemental.
-r (resolved)   (true|false)    Whether the output JSON for the main directory should be based on resolved or unresolved data
-s (draftstatus)        (approved|contributed|provisional|unconfirmed)  The minimum draft status of the output data
-l (coverage)   (minimal|basic|moderate|modern|comprehensive|optional)  The maximum coverage level of the output data
-n (fullnumbers)        (true|false)    Whether the output JSON should output data for all numbering systems, even those not used in the locale
-o (other)      (true|false)    Whether to write out the 'other' section, which contains any unmatched paths
-i (identity)   (true|false)    Whether to copy the identity info into all sections containing data
-k (konfig)     .*      LDML to JSON configuration file

-----------------
