# Primitive tests of the build under Unix 

# RCS: @(#) $Id: testbuild.mk,v 1.2 1998/11/05 00:47:17 hylands Exp $

# To run these tests, do
# cd ../unix 
# make dists
# configure --with-tcl=/users/cxh/pt/obj.sol2.5/tcltk/tcl8.0.3/unix/ `/users/cxh/pt/src/tcltk/java_studio`
# make -f ../tests/testbuild.mk buildtest


buildtest: srcBuild binBuild

srcBuild: jaclSrcBuild blendSrcBuild 
binBuild: jaclBinBuild blendBinBuild



# Include the Unix Makefile
include ../unix/Makefile

# Edit these to suit yourself.

GTAR =		gtar
WITH_TCL = 	/users/cxh/pt/obj.sol2.5/tcltk/tcl8.0.3/unix
TCLSH = 	/users/cxh/pt/bin.sol2.5/tclsh
WISH =		/users/cxh/pt/bin.sol2.5/wish


JDK1.1 = 	/opt/jdk1.1.6
JDK1.1_LD_LIBRARY_PATH =	$(JDK1.1)/lib/sparc

JDK1.2 =	/opt/jdk1.2fcs
JDK1.2_LD_LIBRARY_PATH =	$(JDK1.2)/jre/lib/sparc:$(JDK1.2)/jre/lib/sparc/native_threads

JDK=$(JDK1.1)
TMP_LD_LIBRARY_PATH =	$(JDK1.1_LD_LIBRARY_PATH)
#JDK=$(JDK1.2)
#TMP_LD_LIBRARY_PATH =	$(JDK1.2_LD_LIBRARY_PATH)


TMPPATH =	$(JDK)/bin:/usr/bin:/usr/local/bin:/usr/ccs/bin:.


# Cleanup
testbuild_clean: clean_jaclSrcBuild clean_blendSrcBuild

######################################################################
# Untar the Jacl source and build it
JACLSRCTEST_DIR = $(DISTDIR)/jaclsrctest

jaclSrcBuild: $(JACLSRCTEST_DIR)/$(JACL_DISTNAME)
	@echo "#"
	@echo "# Building from Jacl Src tar file"
	@echo "#"
	mkdir -p $(JACLSRCTEST_DIR)/jacltest
	cd $(JACLSRCTEST_DIR)/$(JACL_DISTNAME)/unix; \
		PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		configure \
		--prefix=$(JACLSRCTEST_DIR)/jacltest \
		--with-studio=$(STUDIO_LIB_DIR); \
		$(MAKE) install; \
		$(MAKE) test

# Untar the Jacl Source file
$(JACLSRCTEST_DIR)/$(JACL_DISTNAME): $(DISTDIR)/$(JACL_SRC_DISTNAME).tar.gz
	@echo "#"
	@echo "# Untarring $@"
	@echo "#"
	-mkdir  $(JACLSRCTEST_DIR)
	cd  $(JACLSRCTEST_DIR); \
		$(GTAR) -zxf $(DISTDIR)/$(JACL_SRC_DISTNAME).tar.gz

clean_jaclSrcBuild:
	rm -rf $(JACLSRCTEST_DIR)


######################################################################
# Untar the Blend source and build it
BLENDSRCTEST_DIR = $(DISTDIR)/blendsrctest

blendSrcBuild: $(BLENDSRCTEST_DIR)/$(BLEND_DISTNAME)
	@echo "#"
	@echo "# Building from Blend Src tar file"
	@echo "#"
	mkdir -p $(BLENDSRCTEST_DIR)/blendtest
	cd $(BLENDSRCTEST_DIR)/$(BLEND_DISTNAME)/unix; \
		PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH) \
		configure \
		--prefix=$(BLENDSRCTEST_DIR)/blendtest \
		--with-studio=$(STUDIO_LIB_DIR) \
		--with-tcl=$(WITH_TCL); \
		$(MAKE) install; \
		$(MAKE) test

# Untar the Blend Source file
$(BLENDSRCTEST_DIR)/$(BLEND_DISTNAME): $(DISTDIR)/$(BLEND_SRC_DISTNAME).tar.gz
	@echo "#"
	@echo "# Untarring $@"
	@echo "#"
	-mkdir  $(BLENDSRCTEST_DIR)
	cd  $(BLENDSRCTEST_DIR); \
		$(GTAR) -zxf $(DISTDIR)/$(BLEND_SRC_DISTNAME).tar.gz

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
	cd $(JACLBINTEST_DIR); \
		echo "puts \"java package: [package require java], jdkVersion: [set java::jdkVersion], patchLevel: [set java::patchLevel] \"; exit" | \
		$(RUN_JACL)

# Untar the Jacl Source file
$(JACLBINTEST_DIR): $(DISTDIR)/$(JACL_DISTNAME).tar.gz
	@echo "#"
	@echo "# Untarring $@"
	@echo "#"
	-mkdir  -p $(JACLBINTEST_DIR)
	cd  $(JACLBINTEST_DIR)/..; \
		$(GTAR) -zxf $(DISTDIR)/$(JACL_DISTNAME).tar.gz

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

clean_jaclBinBuild:
	rm -rf $(JACLBINTEST_DIR)

######################################################################
# Smoke test the Blend Binary distribution
BLENDBINTEST_DIR = $(DISTDIR)/blendbintest/$(BLEND_DISTNAME)

RUN_BLEND= PATH=$(TMPPATH) \
		LD_LIBRARY_PATH=$(TMP_LD_LIBRARY_PATH):$(BLENDBINTEST_DIR) \
		CLASSPATH=$(BLENDBINTEST_DIR)/tclblend.jar:$(BLENDBINTEST_DIR)/tcljava.jar \
		$(TCLSH)

blendBinBuild: $(BLENDBINTEST_DIR)
	@echo "#"
	@echo "# Smoke testing from Blend Bin tar file"
	@echo "#"
	cd $(BLENDBINTEST_DIR); \
		echo "puts \"java package: [package require java], jdkVersion: [set java::jdkVersion], patchLevel: [set java::patchLevel] \"; exit" | \
		$(RUN_BLEND)

# Untar the Blend Source file
$(BLENDBINTEST_DIR): $(DISTDIR)/$(BLEND_DISTNAME).solaris.tar.gz
	@echo "#"
	@echo "# Untarring $@"
	@echo "#"
	-mkdir  -p $(BLENDBINTEST_DIR)
	cd  $(BLENDBINTEST_DIR)/..; \
		$(GTAR) -zxf $(DISTDIR)/$(BLEND_DISTNAME).solaris.tar.gz

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
		echo "java::load -classpath . SimpleExtension; \
				puts [sayhello]" | \
		$(RUN_BLEND)

watchpkg_blendBinBuild:
	cd $(BLENDBINTEST_DIR)/demos/watchpkg; \
		echo "java::load -classpath . StopWatchExtension; \
			sw new; sw set 10; \
			source swCmd.tcl; set s [swNew]; swSet \$$s 10  " | \
		$(RUN_BLEND)

run_blendBinBuild:
		$(RUN_BLEND)

clean_blendBinBuild:
	rm -rf $(BLENDBINTEST_DIR)

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


