@echo off
rem this bat file is used to start jacl, it shoud be in your PATH
rem Copyright (c) 1997-1998 by Moses DeJong
rem See license.terms file for copyright info

set __PREFIX=G:\mo\software\test_tcl

set __JAVA_PREFIX=G:\mo\software\java\jdk1.1.7
set __JAVA_EXECUTABLE=%__JAVA_PREFIX%\bin\java
set __JAVA_CLASSPATH=%__JAVA_PREFIX%\lib\classes.zip;%__JAVA_PREFIX%\lib\jacl.jar;%__JAVA_PREFIX%\lib\tcljava.jar

%JAVA_EXECUTABLE% -ms5m -mx20m -classpath %__JAVA_CLASSPATH% tcl.lang.Shell %1 %2 %3 %4 %5 %6 %7 %8 %9
