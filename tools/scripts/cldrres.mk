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
-include locales/cldrlist.mk

CONVOPTS+= -s $(L)/common/main
CONVOPTS+= -d $(LOCSRCDIR)
CONVOPTS+= -p $(L)/icu/main
CONVOPTS+=-w
CONVOPTS+=-f
## do we want supp??
#CONVOPTS+=-l ~/W/locale/common/main

.PRECIOUS: $(GENRB_SOURCE:%.txt=$(LOCSRCDIR)/%.txt) $(LOCSRCDIR)/root.txt

locales/cldrlist.mk:
	echo -n 'GENRB_SOURCE=' > $@
	( cd $L/common/main ; ls *.xml | grep -v root | fgrep -v supplemental | sed -e s%xml%txt% | tr '\012' ' ' ) >> $@

$(LOCSRCDIR)/%.txt: $(L)/common/main/%.xml
	LDML2ICUConverter $(CONVOPTS) $(<F) || $(RMV) $@

cldr-relist:
	-$(RMV) locales/cldrlist.mk
	$(MAKE) locales/cldrlist.mk
