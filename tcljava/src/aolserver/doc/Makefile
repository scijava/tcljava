# nsjava submake for building the html docs.

include ../Makefile.global


.SUFFIXES: .java .html

.PHONY: all clean

all:  
	(cd ../java && make doc) || exit 1

clean:
	rm -f $(patsubst %, nsjava.%, $(JDOC)) AllNames.html packages.html tree.html

