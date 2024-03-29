# Commit Metadata

### These lines are comments.
### The following list of commits is parsed by the ICU/CLDR commit checker to make corrections
### It has the following structure:
### `- <commit> <bug id or hyphen> <message>`
###
### Hint: To see full commit hashes use:
###   git log --format='%H %s'

- 00decafbad CLDR-0000 Bad commit message
- 50257a7a3eba7bbed634961a2aa74e77b12810bf - Early commit, no bug id
- 283dc84d81cfc47fb15cd664fce4b0151d070ea6 - Early commit, no bug id
- 02198373a591a15b804127acddd32582ec985b7e CLDR-15852 v42 merge commit
- 9e15f63e30cadf57b8eee0f6d6c3398263dfcdac CLDR-15470 v41 cherry pick
- 7b370d06c506200e31f9053416ce98cf23d1d1b6 CLDR-16058 v42 cherry pick into v43
- 5056ea26b4bcbee90652b487a88d79b7f506f432 CLDR-16098 v43 cherry pick
- 7c8c8bb83ca253f499d4d5c08f968327611a6a70 CLDR-16437 v44 dep fix
- f0324b82d27695306d1126c54a6de381cfbd9bcc CLDR-16564 v44 version number fix
- b0a6207ff224a6cd8ca7f888c8a4740bacb83124 CLDR-16598 v44 Monitor SurveyTool server disk space
- 2eef76b58182ee16e12897df33094eafd18a1833 CLDR-16883 v44 ST: hide sideways inheritance
- 99038356323f9f74ab577b1eacb3499a436c5d5d CLDR-17062 v44 CLDRModify
- f355414fb725b517df5281bafbad4cf704674cb2 CLDR-16969 v44 CLDRModify
- cf63211ac313545dcd153a74e4d19d0be593db41 CLDR-16712 v44 CLDRModify
- cbdd59767072c7ca154faae2d3b59ad6ad67dd1d CLDR-16650 v44 cherry-pick maint-43 to main
- f534e312f9c5b6984e5a941746adff08eff66c50 CLDR-16577 v44 merge maint-43 to main
- 09c8e1082808f97608c39062897a7b844d50c18e CLDR-17277 v45 accidental merge commit

### The following are items to skip for a certain CLDR version.
### Format: `# SKIP v00` followed by a list of commits to skip for that version (same structure as above)
### Note: if something is both to be skipped AND has a bad commit message, add it to this file twice: once in the skip list, and once in the corrections section at the top.

# SKIP v41

- 56ca5d5 CLDR-14877 split ticket
- 56ca5d563cf57990a7598f570cb9be51956cb9de - v40 commit
- 382f36702d763713f010a3bf87d34605c0226ebd - v40 commit
- bc8a79f9b1b71b00c716b8b4c8e0185515a6d0b8 - v40 commit
- 4b87f2d718dd9cab45517ebe7f996ac10b6b1fff - v40 commit

# SKIP v42

- f86c67b CLDR-15290 v41 spec
- 0eace83 CLDR-15115 v41 spec
- 5aca9fd CLDR-15398 v41 spec
- bbffc61 CLDR-15408 v41 spec
- 953c013 CLDR-15461 v41 BRS
- 1b84c52 CLDR-15461 v41 BRS

# SKIP v43

- b394834 CLDR-16086 v42 charts
- 5714fc0 CLDR-15956 was reverted in v43, landed in v42
- 51b4b64 CLDR-15956 was reverted in v43, landed in v42

# SKIP v44

- 404635d8b37c222c9233318ae161b6b61e3a4b50 CLDR-15970 v43 dependency fix
- ad04baabf773e8aae714c29fbdb5d9027c9e5c3d CLDR-15970 v43 dependency fix

# SKIP v45

- 1a52361654f5d9c58f084d1d919080423862975a CLDR-17141 kbd: drop transform=no (#3330) (cherry-picked to maint-44)
- 286a10b2b26fb4db33b4225a9f6d7a59eb1608d5 CLDR-17140 kbd: specify modifier matching (#3314) (cherry-picked to maint-44)
- 5f1d2981aa384660722d5788ad7e396f86af9f97 CLDR-17113 kbd: element/attr renaming (#3313) (cherry-picked to maint-44)
- 73e4014f703e78e7868abc218b3e8f60f912cb96 CLDR-17229 kbd: cherry pick FROM maint-44
- 13db39076da1999260ad5ad85b6fc2bf8050756f CLDR-17141 kbd: fixed in 44
- 09c8e1082808f97608c39062897a7b844d50c18e CLDR-17230 emoji: fixed in 44.1
- 657e6e996b88b891132db29cc4fc61c36f174689 CLDR-17141 kbd: fixed in 44
