@echo off
rem this bat file is used to start jacl
rem Copyright (c) 1997-1998 by Moses DeJong

set PREFIX=G:\mo\software\test_tcl

set JAVA=G:\mo\software\java\jdk1.1.6\bin\java

rem Some JDK's might want -native in the line below
set JAVA_FLAGS="-ms5m -mx20m"

set CLASSPATH=%PREFIX%\lib\jacl.jar;%PREFIX%\lib\tcljava.jar;%CLASSPATH%

%JAVA% %JAVA_FLAGS% tcl.lang.Shell %1 %2 %3 %4 %5 %6 %7 %8 %9
