# nsjava submake for building the java classes

include ../../Makefile.global

PKG = tcl.lang

TCLBLENDCLASSPATH = ../../../tclblend
TCLJAVACLASSPATH = ../../../tcljava
TCLBLEND_JNI_HEADER_CLASS_NAMES = \
	tcl.lang.CObject \
	tcl.lang.IdleHandler \
	tcl.lang.Interp \
	tcl.lang.Notifier \
	tcl.lang.TclList \
	tcl.lang.TimerHandler \
	tcl.lang.Util

vpath %.h ../../include
vpath %.java ../../../tclblend/tcl/lang/
vpath %.java ../../../tcljava/tcl/lang/
vpath %.java ../../../tcljava/tcl/lang/reflect
vpath %.class reflect


.SUFFIXES: .class .java .h

.PHONY: all jnih doc clean jfiles

.java.class:
	$(JAVAC) -classpath $(JAVA_CLASSPATH):$(NSJAVA_CLASSPATH):$(TCLBLENDCLASSPATH):$(TCLJAVACLASSPATH) -d ../../ $<

tcl_lang_%.h : %.class
	$(JAVAH) -classpath $(JAVA_CLASSPATH):$(NSJAVA_CLASSPATH):$(TCLBLENDCLASSPATH):$(TCLJAVACLASSPATH) -d ../../include $(PKG).$*


all:	$(JOBJ) jnih


$(JARFILE): $(JOBJ) $(JTEST)
	cd ../../ ; $(JAR) -c0f tcl/lang/$@ tcl

jnih:	javaNative.h

javaNative.h:
	CLASSPATH=$(JAVA_CLASSPATH):$(NSJAVA_CLASSPATH):$(TCLBLENDCLASSPATH):$(TCLJAVACLASSPATH) \
	$(JAVAH) -o ../../include/javaNative.h $(TCLBLEND_JNI_HEADER_CLASS_NAMES)


doc:   
	$(JAVADOC) -d ../doc $(patsubst %, $(DIR)/%, $(JSRC))

clean:
	$(RM) $(patsubst %, %, $(JOBJ)) $(JTEST) 
	$(RM) *.class
	$(RM) reflect/*.class
	$(RM) $(patsubst %, ../../include/%, $(TCLBLEND_JNI_HEADER_CLASS_NAMES))
	$(RM) $(JARFILE)
	$(RM) ../../include/javaNative.h
