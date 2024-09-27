---
title: Running CLDR Tools
---

# Running CLDR Tools

You will need to include some options to run various programs. Here are some samples, but the directories may vary depending on your configuration.

**Standard Gorp**

\-Dfile.encoding\=UTF\-8

\-Xmx3000m

\-DCLDR\_DIR\=${workspace\_loc}/cldr

\-DOTHER\_WORKSPACE\=${workspace\_loc}/"../Google Drive/workspace/"

\-DCLDR\_GEN\_DIR\=${workspace\_loc}/"../Google Drive/workspace/Generated/cldr/"

\-Dregistry\=language\-subtag\-registry

\-DSHOW\_FILES

The xmx is to increase memory so that you don't blow up. If you only do a few dozen locales, you don't need to set it that high.

**Optional**

\-f regex // to only check locales matching the regex, like ".\*" or "en\|fr\|de" ...

\-DSHOW\_FILES // shows files being opened and created


