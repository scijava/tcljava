# nsjava submake for building the java classes

include ../Makefile.global

PKG = nsjava

TCLBLENDCLASSPATH = ../../tclblend
TCLJAVACLASSPATH = ../../tcljava

vpath %.h ../include
vpath %.java ../../tclblend/tcl/lang/
vpath %.java ../../tcljava/tcl/lang/
vpath %.java ../../tcljava/tcl/lang/reflect
vpath %.class ../tcl/lang/reflect
vpath %.jar ../tcl/lang


.SUFFIXES: .class .java .h

.PHONY: all jnih doc clean jfiles

.java.class:
	$(JAVAC) -classpath $(JAVA_CLASSPATH):$(NSJAVA_CLASSPATH):$(TCLBLENDCLASSPATH):$(TCLJAVACLASSPATH) -d ../ $<

nsjava_%.h : %.class
	$(JAVAH) -classpath $(JAVA_CLASSPATH):$(NSJAVA_CLASSPATH):$(TCLBLENDCLASSPATH):$(TCLJAVACLASSPATH) -d ../include $(PKG).$*


all:	$(NSJAVAJOBJ) $(JTEST) $(JARFILE) jnih nsjava.jar


$(JARFILE): $(JOBJ) $(NSJAVAJOBJ) $(JTEST)
	cd ../ ; $(JAR) -c0f tcl/lang/$@ tcl nsjava


jnih:	$(AUTOH)

doc:   
	$(JAVADOC) -d ../doc $(patsubst %, $(DIR)/%, $(JSRC))

clean:
	$(RM) $(patsubst %, %, $(NSJAVAJOBJ)) $(JTEST) 
	$(RM) *.class
	$(RM) $(patsubst %, ../include/%, $(AUTOH))
	$(RM) $(JARFILE)
