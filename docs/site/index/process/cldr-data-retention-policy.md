---
title: CLDR Data Retention Policy
---

# CLDR Data Retention Policy

Certain types of CLDR data can become obsolete, often due to political reorganization or changes in policy within the various countries. When such changes occur, we leave the obsolete data in CLDR for a certain period of time in order to make it easier for applications to migrate to the newer codes. However, eventually it becomes necessary to remove obsolete data from the CLDR in order to keep the data from growing uncontrollably.

The following guidelines have been discussed by the CLDR technical committee and serve as the basis for decision making about when obsolete codes and data are to be removed from the CLDR.

1. Territory Names ( //ldml/localeDisplayNames/territories/territory\[@type\="XX"] ) \- Data is to remain in the CLDR for a period of 5 years after the territory code for territory "XX" is deprecated in the IANA Subtag Registry.
2. Metazone Names ( //ldml/dates/timeZoneNames/metazone\[@type\="ZoneName"] \- Data is to remain in the CLDR for a period of 20 years after the metazone becomes "inactive" ( i.e. The zone name is not used in ANY country ). A spreadsheet listing the Inactive Metazones in CLDR and the dates when they became inactive can be found [here](https://docs.google.com/spreadsheets/d/1Oj1IVo2Vg6wtAhk0Xd3HcA04HKZmSPxksIpvduvSYw8/edit#gid=0).

