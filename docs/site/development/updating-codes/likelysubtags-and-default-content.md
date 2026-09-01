---
title: LikelySubtags and Default Content
---

1. First make sure that you do [Update Language/Script/Region Subtags](/development/updating-codes/update-languagescriptregion-subtags) first
2. Run GenerateLikelySubtags with VM argument ```-DCLDR_DIR``` set to your cldr directory to generate the likely subtag data **AND** the default content locales.
	1. If you are trying to debug, add the VM argument ```-DGenerateLikelySubtagsDebug```
3. Input data:
	1. Data comes from territory/language information in supplemental data.
		1. However, it is supplemented by **LANGUAGE\_OVERRIDES** in GenerateLikelySubtags.java
			1. If there is no territory/language information in supplemental data for a language, add it to **LANGUAGE\_OVERRIDES**.
			2. If the mapping changes when it shouldn't (there are some special cases), add to **LANGUAGE\_OVERRIDES.**
4. Output:
	1. Creates {CLDR\_DIR}/../Generated/cldr/supplemental/likelySubtags.xml and {CLDR\_DIR}/../Generated/cldr/supplemental/supplementalMetadata.xml
	2. Diff with {CLDR\_DIR}/common/supplemental/likelySubtags.xml and {CLDR\_DIR}/common/supplemental/supplementalMetadata.xml
	3. Be very careful to diff everything and check for errors.
		1. Watch especially for backwards incompatible changes; that is, changes rather than just additions.
		2. Look at the above to handle that with **LANGUAGE\_OVERRIDES.**
	4. Run tests, fix input data, and iterate as necessary.
		1. Copy into the svn workspace and commit.

## Updating `langtags.json`

[`langtags.json`](https://github.com/silnrsi/langtags) is used as an additional source for likely subtag information.

To update:

- Go to <https://github.com/silnrsi/langtags/releases/latest> and find the latest release
- Download `langtags.json` and copy it to `./tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/external/langtags.json`
- Check git diff - if there's no change, you're done.
- Re-run `GenerateLikelySubtags` (just as in the above section)
- If there are no changes to `likelySubtags.xml`, then just check in `langtags.json` and note that.
- diff the results, run tests, check in, etc. (see above)
- Note: You can use the `--json` command line option to `GenerateLikelySubtags` to get detailed warnings about `langtags.json`

Example update to `likelySubtags.xml`: (note the `origin=` attribute)

```diff
+               <likelySubtag from="aci" to="aci_Latn_IN" origin="sil1"/>               <!--Aka-Cari‧?‧?        ➡ Aka-Cari‧Latin‧India-->
+               <likelySubtag from="ack" to="ack_Latn_IN" origin="sil1"/>               <!--Aka-Kora‧?‧?        ➡ Aka-Kora‧Latin‧India-->
+               <likelySubtag from="acl" to="acl_Latn_IN" origin="sil1"/>               <!--Akar-Bale‧?‧?       ➡ Akar-Bale‧Latin‧India-->
```
