# -*- Makefile -*-
# copyright (c) 2004 IBM and others. all rights reserved.
#
# (re)generate ICU data out of CLDR
#
# to use this file:
#
## 0. have ICU installed and built normally (sorry, no out-of-source - yet.)
##
## 1. make sure LDML2ICUConverter is in your path (see elsewhere)
## 
## 2. Add a file 'reslocal.mk' in icu/source/data/locales to include this file:
##      include ../../../locale/tools/cldrres.mk
##
## 3. from icu/source/data  type:  'make cldr-clean-old' to get rid of the non-CLDR files
## now just type 'make' and you should be set.
##

# GENRB_ALIAS_SOURCE = \
# GENRB_SOURCE = 
L=../../../locale
#-include locales/cldrlist.mk

# if you want draft locales - uncomment this!  (hint: or, put it in reslocal.mk)
#CONVDEP=-f

# the tool. No invoke needed.
LDML_CONVERTER=LDML2ICUConverter

# command line options to the ldml conversion tool. Shouldn't need to edit
CONVOPTS+=     -s $(L)/common/main -d $(LOCSRCDIR) -p $(L)/icu/main $(CONVDEP)
CONVOPTS_COL+= -s $(L)/common/collation -d $(COLSRCDIR) -p $(L)/icu/collation $(CONVDEP)

# some aliases
GENRB_ALIAS_PATHS=$(GENRB_ALIAS_SOURCE:%.txt=$(LOCSRCDIR)/%.txt)
COLLATION_ALIAS_PATHS=$(COLLATION_ALIAS_SOURCE:%.txt=$(COLSRCDIR)/%.txt)
GENRB_PATHS=$(GENRB_SOURCE:%.txt=$(LOCSRCDIR)/%.txt)
COLLATION_PATHS=$(COLLATION_SOURCE:%.txt=$(COLSRCDIR)/%.txt)

# so make doesn't takes it from us (our carefully built .txt files..)
.PRECIOUS: $(GENRB_PATHS) $(LOCSRCDIR)/root.txt $(GENRB_ALIAS_PATHS) $(COLLATION_PATHS) $(COLSRCDIR)/root.txt $(COLLATION_ALIAS_PATHS)

#locales/cldrlist.mk:
#	echo -n 'GENRB_SOURCE=' > $@
#	( cd $L/common/main ; ls *.xml | grep -v root | fgrep -v supplemental | sed -e s%xml%txt% | tr '\012' ' ' ) >> $@

$(LOCSRCDIR)/%.txt: $(L)/common/main/%.xml
	$(LDML_CONVERTER) $(CONVOPTS) $(<F) || $(RMV) $@

$(COLSRCDIR)/%.txt: $(L)/common/collation/%.xml
	$(LDML_CONVERTER) $(CONVOPTS_COL) $(<F) || $(RMV) $@

# use the following two to clean up just the lists, or the whole deal.
cldr-clean-lists:
	-$(RMV) coll/colfiles.mk locales/resfiles.mk

cldr-clean-old: cldr-clean-lists
	-$(RMV) $(LOCSRCDIR)/*.txt $(COLSRCDIR)/*.txt


# the following two are S-L-O-W.  
# Don't run them if you're just cleaning up!

#ifneq ($(patsubst %clean,,$(MAKECMDGOALS)),)
#ifneq ($(patsubst %cldr-clean-old,,$(MAKECMDGOALS)),)
#ifneq ($(patsubst %cldr-clean-lists,,$(MAKECMDGOALS)),)

$(GENRB_ALIAS_PATHS) $(LOCSRCDIR)/resfiles.mk: $(L)/icu/deprecatedList.xml $(L)/common/main $(L)/icu/main
	echo $(MAKECMDGOALS)
	$(LDML_CONVERTER) $(CONVOPTS) -w $(L)/common/main

$(COLLATION_ALIAS_PATHS) $(COLSRCDIR)/colfiles.mk: $(L)/icu/deprecatedList.xml $(L)/common/collation $(L)/icu/collation
	echo $(MAKECMDGOALS)
	$(LDML_CONVERTER) $(CONVOPTS_COL) -w $(L)/common/collation
#endif
#endif
#endif
