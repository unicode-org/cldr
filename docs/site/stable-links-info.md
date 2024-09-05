---
title: Stable Links Info
---

# Stable Links Info

This page contains information about updating the various stable links to part of CLDR and "latest" versions, etc. Most of the redirects are in the ".htaccess" file of http://www.unicode.org/cldr/ and some are in the /etc/apache2/httpd-rewrite.conf file on www.unicode.org. (Note: at this point, you should not have to touch cldr/.htaccess which should be stable, just the rewrite conf file.)

|   | Visible link for users - this is to remain stable | Redirects or is re-written to this link (examples): | How to update for release |
|---|---|---|---|
|   |  |  |  |
|   | **Versions of CLDR** |  |  |
| development version | http://cldr.unicode.org/index/downloads/dev | http://cldr.unicode.org/index/downloads/cldr-24 | Modify Google sites page |
| latest version | http://cldr.unicode.org/index/downloads/latest | http://cldr.unicode.org/index/downloads/cldr-23-1 | Modify Google sites page |
| specific (eg 22) | http://cldr.unicode.org/index/downloads/cldr-22 | http://cldr.unicode.org/index/downloads/cldr-22 | nothing needed |
|   |  |  |  |
|   | **Links to CLDR data (zip files)** |  |  |
| development version | http://unicode.org/Public/cldr/dev | http://unicode.org/Public/cldr/dev | nothing needed (permanent redirect in cldr/.htaccess if ever needs to be touched) |
| latest version | http://unicode.org/Public/cldr/latest | http://unicode.org/Public/cldr/23.1 | Change redirect to specific released version (in Apache conf file) |
| specific (eg 22) | http://unicode.org/Public/cldr/22 | http://unicode.org/Public/cldr/22 | nothing needed |
|   |  |  |  |
|  | **Chart links** |  |  |
| development version | http://unicode.org/cldr/charts/dev | http://www.unicode.org/repos/cldr-aux/charts/25/index.html | Obsolete |
| latest version | http://unicode.org/cldr/charts/latest | https://unicode-org.github.io/cldr-staging/charts/latest/ | nothing needed |
| specific (eg 41) | http://unicode.org/cldr/charts/41 | https://unicode-org.github.io/cldr-staging/charts/41/ | nothing needed |
|   |  |  |  |
|  | **Specification links** |  |  |
| development version | (will have specific links) | http://www.unicode.org/reports/tr35/proposed.html | nothing needed (1) |
| latest version | (will have specific links) | http://www.unicode.org/reports/tr35/ | nothing needed (1) |
| specific (eg 22) | (will have specific links) | http://www.unicode.org/reports/tr35/tr35-27.html | nothing needed |
|   |  |  |  |
|  | ~~**Delta / Changes**~~ |  |  |
| ~~development version~~ | ~~http://www.unicode.org/cldr/changes/dev~~ | ~~http://unicode.org/cldr/trac/report/63~~ | ~~Update Trac report #63~~ |
| ~~latest version~~ | ~~http://www.unicode.org/cldr/changes/latest~~ | ~~http://unicode.org/cldr/trac/report/62~~ | ~~Update Trac report #62~~ |
|   |  |  |  |
|  | **~~SVN links to the repository~~ (THIS SECTION IS OBSOLETE AND WILL BE REMOVED LATER)** |  |  |
| ~~development version~~ | ~~http://unicode.org/cldr/dev~~ | ~~http://unicode.org/repos/cldr/trunk~~ | ~~nothing needed~~ |
| ~~latest version~~ | ~~http://unicode.org/cldr/latest~~ | ~~http://unicode.org/repos/cldr/tags/latest~~ | ~~Need to " svn delete latest " and then " svn copy 24 latest "~~ |

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)