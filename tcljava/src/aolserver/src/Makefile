# nsjava makefile for building the c-sources.

include ../Makefile.global

vpath %.c    ../../native

VERTCL = $(shell ls $(NS_HOME) | grep tcl8)

.SUFFIXES: .c .h .so

.PHONY: all clean

.c.o:
	$(CC) -c $(CFLAGS) -DTCL_MEM_DEBUG $< $(INC) -o $*.o


all: $(NSJAVALIB)

$(NSJAVALIB): $(OBJ)
	$(LDSO) $(OBJ) $(LIBS) $(NS_HOME)/$(VERTCL)/unix/libtcl8.3g.a $(NS_HOME)/thread/libnsthread.a -o $(NSJAVALIB)

$(OBJ): nsjava.h 

nsjava.h:  $(AUTOH)

clean:
	$(RM) $(OBJ) $(NSJAVALIB) 

nsjava.o:		nsjava.h
log.o:			nsjava_NsLog.h nsjava.h
javaevaltcl.o:		nsjava_NsTcl.h nsjava.h
set.o:			nsjava_NsSet.h nsjava.h
db.o:			nsjava_NsDb.h  nsjava.h
pg.o:			nsjava_NsPg.h  nsjava.h
methods.o:		nsjava.h
tclcalljava.o:		nsjava.h
