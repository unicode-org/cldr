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
##      include ../../../locale/tools/scripts/cldrres.mk
##
## 3. from icu/source/data  type:  'make cldr-clean-old' to get rid of the non-CLDR files
## now just type 'make' and you should be set.
##

## Root of the CLDR directory (contains common, icu, ...)
CLDR_ROOT=../../../cldr
ICU_XML=$(srcdir)/xml

## Arguments to the LDML2ICUConverter program

## if you want draft locales - uncomment this (or, put it in reslocal.mk)
#LDML_CONVERTER_OPTS+=-f

## if you want verbose output - uncomment this (or, put it in reslocal.mk)
#LDML_CONVERTER_OPTS+=-v

## the tool. No $(INVOKE) needed.
LDML_CONVERTER=LDML2ICUConverter

## command line options to the ldml conversion tool. Shouldn't need to edit from here on down.
LDML_OPTS_RES += -s $(CLDR_ROOT)/common/main -d $(LOCSRCDIR) -p $(ICU_XML)/main -m $(CLDR_ROOT)/common/supplemental $(LDML_CONVERTER_OPTS)
LDML_OPTS_COL += -s $(CLDR_ROOT)/common/collation -d $(COLSRCDIR) -p $(ICU_XML)/collation $(LDML_CONVERTER_OPTS)

## some aliases
GENRB_ALIAS_PATHS=$(GENRB_ALIAS_SOURCE:%.txt=$(LOCSRCDIR)/%.txt)
GENRB_SYNTHETIC_PATHS=$(GENRB_SYNTHETIC_ALIAS:%.txt=$(LOCSRCDIR)/%.txt)
COLLATION_ALIAS_PATHS=$(COLLATION_ALIAS_SOURCE:%.txt=$(COLSRCDIR)/%.txt)
COLLATION_SYNTHETIC_PATHS=$(COLLATION_SYNTHETIC_ALIAS:%.txt=$(COLSRCDIR)/%.txt)
COLLATION_EMPTY_PATHS=$(COLLATION_EMPTY_SOURCE:%.txt=$(COLSRCDIR)/%.txt)
GENRB_PATHS=$(GENRB_SOURCE:%.txt=$(LOCSRCDIR)/%.txt)
COLLATION_PATHS=$(COLLATION_SOURCE:%.txt=$(COLSRCDIR)/%.txt)

## so make doesn't takes it from us (our carefully built .txt files..)
.PRECIOUS: $(GENRB_PATHS) $(LOCSRCDIR)/root.txt $(GENRB_ALIAS_PATHS) $(COLLATION_PATHS) $(COLSRCDIR)/root.txt $(COLLATION_ALIAS_PATHS)

## Rebuild ICU resource .txt from XML source
$(LOCSRCDIR)/%.txt: $(CLDR_ROOT)/common/main/%.xml
	$(LDML_CONVERTER) $(LDML_OPTS_RES) $(<F) || ($(RMV) $@;false)

## Rebuild ICU collation .txt from XML source
$(COLSRCDIR)/%.txt: $(CLDR_ROOT)/common/collation/%.xml
	$(LDML_CONVERTER) $(LDML_OPTS_COL) $(<F) || ($(RMV) $@;false)

## Special target for cleaning up the resource lists
cldr-clean-lists:
	-$(RMV) coll/colfiles.mk locales/resfiles.mk

## Special target for cleaning up ALL generated files
cldr-clean-old: cldr-clean-lists
	-$(RMV) $(LOCSRCDIR)/*.txt $(COLSRCDIR)/*.txt

## Special target for building the resource lists
cldr-lists:  coll/colfiles.mk locales/resfiles.mk


## Alias generation. These are slow and shouldn't be rebuilt if  'make clean' is being run. 

## These don't work right, yet.
#ifneq ($(patsubst %clean,,$(MAKECMDGOALS)),)
#ifneq ($(patsubst %cldr-clean-old,,$(MAKECMDGOALS)),)
#ifneq ($(patsubst %cldr-clean-lists,,$(MAKECMDGOALS)),)

$(GENRB_SYNTHETIC_PATHS) $(LOCSRCDIR)/resfiles.mk: $(ICU_XML)/deprecatedList.xml 
	$(LDML_CONVERTER) $(LDML_OPTS_RES) -w $(CLDR_ROOT)/common/main  || ($(RMV) $@;false)

$(COLLATION_SYNTHETIC_PATHS) $(COLLATION_EMPTY_PATHS) $(COLSRCDIR)/colfiles.mk: $(ICU_XML)/deprecatedList.xml 
	$(LDML_CONVERTER) $(LDML_OPTS_COL) -w $(CLDR_ROOT)/common/collation || ($(RMV) $@;false)
#endif
#endif
#endif

## Special target for building a tarball of the locale data
locales.tgz:
	tar cvf - locales/resfiles.mk locales/*.txt coll/colfiles.mk coll/*.txt | gzip > $@

## Special target for building the resource .txts
cldr-locale-txt: $(GENRB_PATHS)

## Special target for building the collation .txt
cldr-collation-txt: $(COLLATION_PATHS)

cldr-all-files: cldr-lists cldr-locale-txt cldr-collation-txt
