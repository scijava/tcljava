# Primitive tests of the tarfiles under Unix.
# This file need not be shipped with Jacl or Tcl Blend, it should
# only be in the tcljava tar file that includes both Jacl and Tcl Blend.

# RCS: @(#) $Id: testbuild.mk,v 1.6 1998/11/16 06:10:24 hylands Exp $

# To run these tests, do
# cd ../unix 
# configure --with-tcl=/users/cxh/pt/obj.sol2.5/tcltk/tcl8.0.3/unix/ `/users/cxh/pt/src/tcltk/java_studio`
# make dists
# make -f ../tests/testbuild.mk buildtest


# Build everything and run all the tests
buildtest: srcBuild srcTest binBuild demoBinBuild

# Build from sources
srcBuild: jaclSrcBuild blendSrcBuild 

# Test the build from sources
srcTest: jaclSrcTest jaclSrcSmokeTest blendSrcTest blendSrcSmokeTest

# Test the binary tar files
binBuild: jaclBinBuild blendBinBuild

# Test the demos in the binary tar files
demoBinBuild: demo_jaclBinBuild demo_blendBinBuild


# Include the Unix Makefile
include ../unix/Makefile

# Edit these to suit yourself.

GTAR =		gtar
WITH_TCL = 	/users/cxh/pt/obj.sol2.5/tcltk/tcl8.0.3/unix
TCLSH = 	/users/cxh/pt/bin.sol2.5/tclsh
WISH =		/users/cxh/pt/bin.sol2.5/wish

# Values for JDK1.1
JDK =		/opt/jdk1.1.6
JDK_LD_LIBRARY_PATH =	$(JDK1.1)/lib/sparc
JDK_VERSION =	1.1.6

# Values for JDK1.2
#JDK =		/opt/jdk1.2fcs
#JDK_LD_LIBRARY_PATH =	$(JDK1.2)/jre/lib/sparc:$(JDK1.2)/jre/lib/sparc/native_threads
#JDK_VERSION =	1.2fcs

TMPPATH =	$(JDK)/bin:/usr/bin:/usr/local/bin:/usr/ccs/bin:.

# Cleanup
testbuild_clean: clean_jaclSrcBuild clean_blendSrcBuild \
			clean_jaclBinBuild clean_blendBinBuild


######################################################################
# Smoke test variables
#
# A smoke test is a test where we plug it in and see if it explodes or not.
# To smoke test Jacl and Tcl Blend, we start up and run a few commands
# and check the results.

# Command to run to start up Jacl or Blend and see if it runs at all
SMOKETEST = 	echo "puts \"java package: [package require java], jdkVersion: [set java::jdkVersion], patchLevel: [set java::patchLevel] \"; exit" 

# File that we put the output of the smoke test into
SMOKETEST_RESULTS_FILE =	$(DISTDIR)/smoketest_results

# File that contains the ok results we compare against
OK_SMOKETEST_RESULTS_FILE =	$(DISTDIR)/ok_smoketest_results

# Correct output for Jacl
JACL_GOOD_SMOKETEST_RESULTS =	"% java package: 1.1, jdkVersion: $(JDK_VERSION), patchLevel: 1.1b1 "

# Correct output for Tcl Blend
BLEND_GOOD_SMOKETEST_RESULTS =	"java package: 1.1, jdkVersion: $(JDK_VERSION), patchLevel: 1.1b1 "


######################################################################
# Untar the Jacl source and build it
JACLSRCTEST_DIR = $(DISTDIR)/jaclsrctest
JACLSRCTEST_JACLSH = $(JACLSRCTEST_DIR)/jacltest.exec/bin/jaclsh
jaclSrcBuild: $(JACLSRCTEST_JACLSH)
$(JACLSRCTEST_JACLSH): $(JACLSRCTEST_DIR)/$(JACL_DISTNAME)
	@echo "#"
	@echo "# Building from Jacl Src tar file"
	@echo "#"
	mkdir -p $(JACLSRCTEST_DIR)/jacltest $(JACLSRCTEST_DIR)/jacltest.exec
	cd $(JACLSRCTEST_DIR)/$(JACL_DISTNAME)/unix; \
		PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		configure \
		--prefix=$(JACLSRCTEST_DIR)/jacltest \
		--exec-prefix=$(JACLSRCTEST_DIR)/jacltest.exec \
		--with-studio=$(STUDIO_LIB_DIR); \
		$(MAKE) all install

# Untar the Jacl Source file
$(JACLSRCTEST_DIR)/$(JACL_DISTNAME): $(DISTDIR)/$(JACL_SRC_DISTNAME).tar.gz
	@echo "#"
	@echo "# Untarring $^ to create $@"
	@echo "#"
	ls -ldg $@ $^
	-mkdir  $(JACLSRCTEST_DIR)
	cd  $(JACLSRCTEST_DIR); \
		$(GTAR) -zxf $^
	touch $@

jaclSrcTest:  $(JACLSRCTEST_JACLSH)
	@echo "#"
	@echo "# Running the Jacl Test suite built from sources"
	@echo "#"
	cd $(JACLSRCTEST_DIR)/$(JACL_DISTNAME)/unix; \
		PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		$(MAKE) test

jaclSrcSmokeTest:  $(JACLSRCTEST_JACLSH)
	@echo "#"
	@echo "# Smoke testing jaclsh built from sources"
	@echo "#"
	rm -f $(SMOKETEST_RESULTS_FILE) $(OK_SMOKETEST_RESULTS_FILE)
	echo $(JACL_GOOD_SMOKETEST_RESULTS) > $(OK_SMOKETEST_RESULTS_FILE)
	PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		$(SMOKETEST) | \
		 $(JACLSRCTEST_JACLSH) > $(SMOKETEST_RESULTS_FILE)
	diff $(OK_SMOKETEST_RESULTS_FILE) $(SMOKETEST_RESULTS_FILE)

clean_jaclSrcBuild:
	rm -rf $(JACLSRCTEST_DIR)


######################################################################
# Untar the Blend source and build it
BLENDSRCTEST_DIR = $(DISTDIR)/blendsrctest
BLENDSRCTEST_JTCLSH = $(BLENDSRCTEST_DIR)/blendtest.exec/bin/jtclsh 
blendSrcBuild: $(BLENDSRCTEST_JTCLSH)
$(BLENDSRCTEST_JTCLSH): $(BLENDSRCTEST_DIR)/$(BLEND_DISTNAME)
	@echo "#"
	@echo "# Building from Blend Src tar file"
	@echo "#"
	mkdir -p $(BLENDSRCTEST_DIR)/blendtest \
		$(BLENDSRCTEST_DIR)/blendtest.exec
	cd $(BLENDSRCTEST_DIR)/$(BLEND_DISTNAME)/unix; \
		PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		configure \
		--prefix=$(BLENDSRCTEST_DIR)/blendtest \
		--exec-prefix=$(BLENDSRCTEST_DIR)/blendtest.exec \
		--with-studio=$(STUDIO_LIB_DIR) \
		--with-tcl=$(WITH_TCL); \
		$(MAKE) all install

# Untar the Blend Source file
$(BLENDSRCTEST_DIR)/$(BLEND_DISTNAME): $(DISTDIR)/$(BLEND_SRC_DISTNAME).tar.gz
	@echo "#"
	@echo "# Untarring $@"
	@echo "#"
	-mkdir  $(BLENDSRCTEST_DIR)
	cd  $(BLENDSRCTEST_DIR); \
		$(GTAR) -zxf $(DISTDIR)/$(BLEND_SRC_DISTNAME).tar.gz
	touch $@

blendSrcTest: $(BLENDSRCTEST_JTCLSH)
	@echo "#"
	@echo "# Running the Blend test suite built from sources"
	@echo "#"
	cd $(BLENDSRCTEST_DIR)/$(BLEND_DISTNAME)/unix; \
		PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		$(MAKE) test


blendSrcSmokeTest: $(BLENDSRCTEST_DIR)/blendtest.exec/bin/jtclsh
	@echo "#"
	@echo "# Smoke testing jtclsh built from sources"
	@echo "#"
	rm -f $(SMOKETEST_RESULTS_FILE) $(OK_SMOKETEST_RESULTS_FILE)
	echo $(BLEND_GOOD_SMOKETEST_RESULTS) > $(OK_SMOKETEST_RESULTS_FILE)
	PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		$(SMOKETEST) | \
		$(BLENDSRCTEST_JTCLSH) > $(SMOKETEST_RESULTS_FILE)
	diff $(OK_SMOKETEST_RESULTS_FILE) $(SMOKETEST_RESULTS_FILE)

clean_blendSrcBuild:
	rm -rf $(BLENDSRCTEST_DIR)


######################################################################
# Smoke test the Jacl Binary distribution
JACLBINTEST_DIR = $(DISTDIR)/jaclbintest/$(JACL_DISTNAME)

RUN_JACL= PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=/tmp/foo \
		CLASSPATH=$(JACLBINTEST_DIR)/jacl.jar:$(JACLBINTEST_DIR)/tcljava.jar \
		java tcl.lang.Shell

jaclBinBuild: $(JACLBINTEST_DIR)
	@echo "#"
	@echo "# Smoke testing from Jacl Bin tar file"
	@echo "#"
	rm -f $(SMOKETEST_RESULTS_FILE) $(OK_SMOKETEST_RESULTS_FILE)
	echo $(JACL_GOOD_SMOKETEST_RESULTS) > $(OK_SMOKETEST_RESULTS_FILE)
	cd $(JACLBINTEST_DIR); \
		echo "puts \"java package: [package require java], jdkVersion: [set java::jdkVersion], patchLevel: [set java::patchLevel] \"; exit" | \
		$(RUN_JACL) > $(SMOKETEST_RESULTS_FILE)
	diff $(OK_SMOKETEST_RESULTS_FILE) $(SMOKETEST_RESULTS_FILE)

# Untar the Jacl Binary file
$(JACLBINTEST_DIR): $(DISTDIR)/$(JACL_DISTNAME).tar.gz
	@echo "#"
	@echo "# Untarring $@"
	@echo "#"
	-mkdir  -p $(JACLBINTEST_DIR)
	cd  $(JACLBINTEST_DIR)/..; \
		$(GTAR) -zxf $(DISTDIR)/$(JACL_DISTNAME).tar.gz
	touch $@

clean_jaclBinBuild:
	rm -rf $(JACLBINTEST_DIR)

######################################################################
# Rules to run Jacl from the Binary file demos
demo_jaclBinBuild: gluepkg_jaclBinBuild guiDemo_jaclBinBuild \
	simplepkg_jaclBinBuild watchpkg_jaclBinBuild pyramidpkg_jaclBinBuild

gluepkg_jaclBinBuild:
	cd $(JACLBINTEST_DIR)/demos/gluepkg; \
		echo "source glue.tcl; exit" | \
		$(RUN_JACL)

guiDemo_jaclBinBuild:
	cd $(JACLBINTEST_DIR)/demos/guiDemo; \
		echo "source guiDemo.tcl; exit" | \
		$(RUN_JACL)


simplepkg_jaclBinBuild:
	cd $(JACLBINTEST_DIR)/demos/simplepkg; \
		echo "java::load -classpath . SimpleExtension; \
				puts [sayhello]; exit" | \
		$(RUN_JACL)

watchpkg_jaclBinBuild:
	cd $(JACLBINTEST_DIR)/demos/watchpkg; \
		echo "java::load -classpath . StopWatchExtension; \
			sw new; sw set 10; \
			source swCmd.tcl; set s [swNew]; \
			swSet \$$s 10; exit" | \
		$(RUN_JACL)

pyramidpkg_jaclBinBuild:
	cd $(JACLBINTEST_DIR)/demos/pyramidpkg; \
		RUN_JACL= PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=/tmp/foo \
		CLASSPATH=.:$(JACLBINTEST_DIR)/jacl.jar:$(JACLBINTEST_DIR)/tcljava.jar \
		appletviewer pyramid.html

######################################################################
# Smoke test the Blend Binary distribution
BLENDBINTEST_DIR = $(DISTDIR)/blendbintest/$(BLEND_DISTNAME)

RUN_BLEND= PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH):$(BLENDBINTEST_DIR) \
		TCLLIBPATH=$(BLENDBINTEST_DIR) \
		$(TCLSH)

blendBinBuild: $(BLENDBINTEST_DIR)
	@echo "#"
	@echo "# Smoke testing from Blend Bin tar file"
	@echo "#"
	rm -f $(SMOKETEST_RESULTS_FILE) $(OK_SMOKETEST_RESULTS_FILE)
	echo $(BLEND_GOOD_SMOKETEST_RESULTS) > $(OK_SMOKETEST_RESULTS_FILE)
	cd $(BLENDBINTEST_DIR); \
		echo "puts \"java package: [package require java], jdkVersion: [set java::jdkVersion], patchLevel: [set java::patchLevel] \"; exit" | \
		$(RUN_BLEND) > $(SMOKETEST_RESULTS_FILE)
	diff $(OK_SMOKETEST_RESULTS_FILE) $(SMOKETEST_RESULTS_FILE)

# Untar the Blend Source file
$(BLENDBINTEST_DIR): $(DISTDIR)/$(BLEND_DISTNAME).solaris.tar.gz
	@echo "#"
	@echo "# Untarring $@"
	@echo "#"
	-mkdir  -p $(BLENDBINTEST_DIR)
	cd  $(BLENDBINTEST_DIR)/..; \
		$(GTAR) -zxf $(DISTDIR)/$(BLEND_DISTNAME).solaris.tar.gz
	touch $@

clean_blendBinBuild:
	rm -rf $(BLENDBINTEST_DIR)

######################################################################
# Rules to run Tcl Blend from the Binary file demos
demo_blendBinBuild: gluepkg_blendBinBuild guiDemo_blendBinBuild \
	simplepkg_blendBinBuild watchpkg_blendBinBuild

gluepkg_blendBinBuild:
	cd $(BLENDBINTEST_DIR)/demos/gluepkg; \
		echo "source glue.tcl" | \
		$(RUN_BLEND)

guiDemo_blendBinBuild:
	cd $(BLENDBINTEST_DIR)/demos/guiDemo; \
		echo "source guiDemo.tcl" | \
		$(RUN_BLEND)

simplepkg_blendBinBuild:
	cd $(BLENDBINTEST_DIR)/demos/simplepkg; \
		echo "package require java; \
			java::load -classpath . SimpleExtension; \
				puts [sayhello]" | \
		$(RUN_BLEND)

watchpkg_blendBinBuild:
	cd $(BLENDBINTEST_DIR)/demos/watchpkg; \
		echo "package require java; \
			java::load -classpath . StopWatchExtension; \
			sw new; sw set 10; \
			source swCmd.tcl; set s [swNew]; swSet \$$s 10  " | \
		$(RUN_BLEND)

run_blendBinBuild:
		$(RUN_BLEND)

##############################################


# Test out jaclBinDist rule.  Run 'make jaclSrcDist jaclSrcBuild' first
jaclBinTest:
	rm -rf $(DISTDIR)/jaclbintest
	mkdir -p $(DISTDIR)/jaclbintest
	cd $(JACL_DISTDIR)/unix; \
		$(MAKE) jaclBinDist DISTDIR=$(DISTDIR)/jaclbintest



# Sanity check by building, installing, running tests
tclblendSrcBuild:
	mkdir -p $(DISTDIR)/tbtest
	cd $(BLEND_DISTDIR)/unix; \
		configure -with-tcl=$(TCL_BIN_DIR) \
			 --prefix=$(DISTDIR)/tbtest; \
		$(MAKE) install; \
		$(MAKE) test_tclblend.build; \
		$(MAKE) test






test_tclblend.jar:
	@echo "This test builds tclblend, then jacl, then tries"
	@echo "the tclblend test suite.  This "
	$(MAKE) TCLJAVA=tclblend
	$(MAKE) TCLJAVA=tclblend test
	$(MAKE) TCLJAVA=jacl
	$(MAKE) TCLJAVA=jacl test
	$(MAKE) TCLJAVA=tclblend test


