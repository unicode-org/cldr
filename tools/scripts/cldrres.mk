# copyright (c) 2004 IBM
#
# regenerate ICU data out of CLDR
#
# Note: doesn't handle alias locales, etc yet.
#
# to use this file:
## Save this file in icu/source/data/locales/cldrres.mk
## make sure LDML2ICUConverter is in your path (see elsewhere)
## Add a file 'reslocal.mk' in icu/source/data/locales  with this line:
##    -include locales/cldrres.mk
## from icu/source/data/locales  type:  'make cldr-relist' to get the locale list going
## now just type 'make' and you should be set.

# GENRB_ALIAS_SOURCE = \
# GENRB_SOURCE = 
L=../../../locale
#-include locales/cldrlist.mk

# for deprecates
CONVDEP=-f

CONVOPTS+=     -s $(L)/common/main -d $(LOCSRCDIR) -p $(L)/icu/main $(CONVDEP)
CONVOPTS_COL+= -s $(L)/common/collation -d $(COLSRCDIR) -p $(L)/icu/collation $(CONVDEP)

GENRB_ALIAS_PATHS=$(GENRB_ALIAS_SOURCE:%.txt=$(LOCSRCDIR)/%.txt)
COLLATION_ALIAS_PATHS=$(COLLATION_ALIAS_SOURCE:%.txt=$(COLSRCDIR)/%.txt)
GENRB_PATHS=$(GENRB_SOURCE:%.txt=$(LOCSRCDIR)/%.txt)
COLLATION_PATHS=$(COLLATION_SOURCE:%.txt=$(COLSRCDIR)/%.txt)

.PRECIOUS: $(GENRB_PATHS) $(LOCSRCDIR)/root.txt $(GENRB_ALIAS_PATHS) $(COLLATION_PATHS) $(COLSRCDIR)/root.txt $(COLLATION_ALIAS_PATHS)

#locales/cldrlist.mk:
#	echo -n 'GENRB_SOURCE=' > $@
#	( cd $L/common/main ; ls *.xml | grep -v root | fgrep -v supplemental | sed -e s%xml%txt% | tr '\012' ' ' ) >> $@

$(LOCSRCDIR)/%.txt: $(L)/common/main/%.xml
	LDML2ICUConverter $(CONVOPTS) $(<F) || $(RMV) $@

$(COLSRCDIR)/%.txt: $(L)/common/collation/%.xml
	LDML2ICUConverter $(CONVOPTS_COL) $(<F) || $(RMV) $@

#cldr-relist:
#	-$(RMV) locales/cldrlist.mk
#	$(MAKE) locales/cldrlist.mk

cldr-resfiles:
	LDML2ICUConverter $(CONVOPTS) -w $(L)/common/main

$(GENRB_ALIAS_PATHS) $(LOCSRCDIR)/resfiles.mk: $(L)/icu/deprecatedList.xml $(L)/common/main $(L)/icu/main
	LDML2ICUConverter $(CONVOPTS) -w $(L)/common/main


$(COLLATION_ALIAS_PATHS) $(COLSRCDIR)/colfiles.mk: $(L)/icu/deprecatedList.xml $(L)/common/collation $(L)/icu/collation
	LDML2ICUConverter $(CONVOPTS_COL) -w $(L)/common/collation


