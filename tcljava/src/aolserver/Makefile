# main makefile for building the nsjava module.

include Makefile.global

DIRS = tcl/lang nsjava src

.PHONY: doc all clean install distclean _distclean cleanmake

all: 
	@for i in $(DIRS); do \
		echo "building \"$$i\""; \
		(cd $$i && $(MAKE) all ) || exit 1; \
	done

doc:    all
	(cd java && $(MAKE) doc)


clean: 
	@for i in $(DIRS); do \
		echo "cleaning \"$$i\""; \
		( cd $$i && $(MAKE) $@) || exit 1; \
	done
	$(RM) `$(FIND) . -name '*~' -print -name '.*~' -print`

distclean: _distclean clean cleanmake

_distclean: 
	$(RM) config.cache config.log config.status

cleanmake:
	$(RM) Makefile.global

install: all
	cp src/$(NSJAVALIB) $(AOLSERVER_HOME)/bin
	cp tcl/lang/$(JARFILE) $(AOLSERVER_HOME)/bin
