# this is a test suite written for the jar program

source defs

#set JAVA /tmp/mo/jdk1.2/bin/java
set JAVA java

set JAR "exec $JAVA -Djava.compiler= kaffe.jar.Jar "

proc writeToFile { fname str {times 1} } {
    set fd [open $fname w]
    for {set i 0} {$i < $times} {incr i} {
	puts $fd $str
    }
    close $fd
}

file delete -force test

if {! [file isdirectory test] } {
    #puts "creating test directory"
    file mkdir test
    cd test
    writeToFile file1 "file1 contents."
    writeToFile file2 "file2 contents."
    file mkdir dir1
    writeToFile dir1/file3 "dir1/file3 contents."
    file mkdir dir1/dir2
    writeToFile dir1/dir2/file4 "dir1/dir2/file4 contents."
    
    # make a big file filled with 0's
    # it must be at least 1 meg so jar size wraps

    # create string of 1000 0's

    set str ""
    for {set i 0} {$i < 1000} {incr i} {
	append str 0
    }

    # write 1K string of 0's to file 1000 times
    writeToFile big.data $str 1000
} else {
    error "test directory exists"
}


set USAGE "Usage: jar {ctxu}\[vfm0M\] \[jar-file\] \[manifest-file\] \[-C dir\] files ...
Options:
\t-c  create new archive
\t-t  list table of contents for archive
\t-x  extract named (or all) files from archive
\t-u  update existing archive
\t-v  generate verbose output on standard output
\t-f  specify archive file name
\t-m  include manifest information from specified manifest file
\t-0  store only; use no ZIP compression
\t-M  Do not create a manifest file for the entries
\t-C  change to the specified directory and include the following files
If any file is a directory then it is processed recursively.
The manifest file name and the archive file name needs to be specified
in the same order the 'm' and 'f' flags are specified.

Example 1: to archive two class files into an archive called classes.jar:
\tjar cvf classes.jar Foo.class Bar.class
Example 2: use an existing manifest file 'mymanifest' and archive all the
\t\tfiles in the foo/ directory into 'classes.jar':
\tjar cvfm classes.jar mymanifest -C foo/ ."

set trash {"}

# error appended by Tcl's catch command
set TERR "child process exited abnormally"



# test series 1 : check usage errors

test 1.1 {usage error} {
    list [catch {eval $JAR} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.2 {usage error, bad option} {
    list [catch {eval $JAR Z} err] $err
} [list 1 "Illegal option: Z\n${USAGE}\n${TERR}"]

test 1.3 {usage error, ignore - in options} {
    list [catch {eval $JAR -Z} err] $err
} [list 1 "Illegal option: Z\n${USAGE}\n${TERR}"]

test 1.4 {usage error, no -ctxu option given} {
    list [catch {eval $JAR -M} err] $err
} [list 1 "One of the options -{ctxu} must be specified!\n${USAGE}\n${TERR}"]

test 1.5 {usage error, more than one of -ctxu given} {
    list [catch {eval $JAR -cx} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.6 {usage error, more than one of -ctxu given} {
    list [catch {eval $JAR -tx} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.7 {usage error, more than one of -ctxu given} {
    list [catch {eval $JAR -tu} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.8 {usage error, -c with no file arguments} {
    list [catch {eval $JAR -c} err] $err
} [list 1 "'c' flag requires that input files be specified!\n${USAGE}\n${TERR}"]

test 1.9 {usage error, -u with no file arguments} {
    list [catch {eval $JAR -uf t.jar} err] $err
} [list 1 "'u' flag requires that manifest or archive or input files be specified!\n${USAGE}\n${TERR}"]

test 1.10 {usage error, -u with no archive} {
    list [catch {eval $JAR -u file1} err] $err
} [list 1 "'u' flag requires that manifest or archive or input files be specified!\n${USAGE}\n${TERR}"]

test 1.11 {usage error, -m with no manifest argument} {
    list [catch {eval $JAR -tm} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.12 {usage error, -f with no file argument} {
    list [catch {eval $JAR -f} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.13 {usage error, -m option given more than once} {
    list [catch {eval $JAR -mm man1 man2} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.14 {usage error, -f option given more than once} {
    list [catch {eval $JAR -ff file1 file2} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.15 {usage error, both -m and -M arguments given} {
    list [catch {eval $JAR -mM file.manifest} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.16 {usage error, both -m and -M arguments given} {
    list [catch {eval $JAR -Mm file.manifest} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.17 {usage error, -C and no directory} {
    list [catch {eval $JAR -cf f.jar -C} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.18 {usage error, -C and no file} {
    list [catch {eval $JAR -cf f.jar -C dir} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.19 {usage error, -C after -C} {
    list [catch {eval $JAR -cf f.jar -C -C file} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.20 {usage error, -C dir -C} {
    list [catch {eval $JAR -cf f.jar -C dir -C file} err] $err
} [list 1 "${USAGE}\n${TERR}"]

test 1.21 {usage error, -t and -C} {
    list [catch {eval $JAR -tf f.jar -C dir file} err] $err
} [list 1 "'t' flag can not be used with the -C flag!\n${USAGE}\n${TERR}"]





# test series 2 : check for -f option with file that does not exist

set nonexistent nonexistent.jar

test 2.1 {archive does not exist} {
    list [catch {eval $JAR -xf $nonexistent} err] $err
} [list 1 "archive \"$nonexistent\" does not exist\n${TERR}"]

test 2.2 {archive does not exist} {
    list [catch {eval $JAR -tf $nonexistent} err] $err
} [list 1 "archive \"$nonexistent\" does not exist\n${TERR}"]

test 2.3 {archive does not exist} {
    list [catch {eval $JAR -uf $nonexistent file1} err] $err
} [list 1 "archive \"$nonexistent\" does not exist\n${TERR}"]






# test series 3 : check creation of jar archives

set testj test.jar
set testm test.manifest
writeToFile $testm "Name: none\nJava-Bean: False"

proc sortLines { lines } {
    return [join [lsort [split $lines \n]] \n]
}

test 3.1 {jar create, to stdout, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -c file1 file2 > $testj} err] \
	[file exists $testj] $err
} {0 1 {}}

test 3.2 {jar create, to stdout, verbose to stderr, no manifest, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cvM file1 file2 > $testj} err] \
	[file exists $testj] $err
} [list 1 1 "adding: file1 (in=16) (out=18) (deflated -12%)
adding: file2 (in=16) (out=18) (deflated -12%)"]

test 3.3 {jar create, to stdout, verbose to stderr, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cv file1 file2 > $testj} err] \
	[file exists $testj] $err
} [list 1 1 "added manifest
adding: file1 (in=16) (out=18) (deflated -12%)
adding: file2 (in=16) (out=18) (deflated -12%)"]

test 3.4 {jar create, to file, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cf $testj file1 file2} err] \
	[file exists $testj] $err
} {0 1 {}}

test 3.5 {jar create, to file, verbose to stdout, no manifest, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cvfM $testj file1 file2} err] \
	[file exists $testj] $err
} [list 0 1 "adding: file1 (in=16) (out=18) (deflated -12%)
adding: file2 (in=16) (out=18) (deflated -12%)"]

test 3.6 {jar create, to file, verbose to stdout, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cvf $testj file1 file2} err] \
	[file exists $testj] $err
} [list 0 1 "added manifest
adding: file1 (in=16) (out=18) (deflated -12%)
adding: file2 (in=16) (out=18) (deflated -12%)"]

test 3.7 {jar create, to file, not compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cf0 $testj file1 file2} err] \
	[file exists $testj] $err
} {0 1 {}}

test 3.8 {jar create, to file, verbose to stdout, no manifest, not compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cvf0M $testj file1 file2} err] \
	[file exists $testj] $err
} [list 0 1 "adding: file1 (in=16) (out=16) (stored 0%)
adding: file2 (in=16) (out=16) (stored 0%)"]

test 3.9 {jar create, to file, with manifest, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cvfm $testj $testm file1 file2} err] \
	[file exists $testj] $err
} [list 0 1 "added manifest
adding: file1 (in=16) (out=18) (deflated -12%)
adding: file2 (in=16) (out=18) (deflated -12%)"]

test 3.10 {jar create, with manifest, to file, compressed} {
    file delete -force $testj
    list [catch {eval $JAR -cvmf $testm $testj file1 file2} err] \
	[file exists $testj] $err
} [list 0 1 "added manifest
adding: file1 (in=16) (out=18) (deflated -12%)
adding: file2 (in=16) (out=18) (deflated -12%)"]

test 3.11 {jar create, no manifest, to file, compressed, with -C} {
    file delete -force $testj
    list [catch {eval $JAR -cvfM $testj file2 -C dir1 file3} err] \
	[file exists $testj] $err
} [list 0 1 "adding: file2 (in=16) (out=18) (deflated -12%)
adding: file3 (in=21) (out=23) (deflated -10%)"]

test 3.12 {jar create, no manifest, to file, compressed, with -C twice} {
    file delete -force $testj
    list [catch {eval $JAR -cvfM $testj file1 file2 \
		     -C dir1 file3 -C dir1/dir2 file4} err] \
	[file exists $testj] $err
} [list 0 1 "adding: file1 (in=16) (out=18) (deflated -12%)
adding: file2 (in=16) (out=18) (deflated -12%)
adding: file3 (in=21) (out=23) (deflated -10%)
adding: file4 (in=26) (out=28) (deflated -8%)"]

test 3.13 {jar create, no manifest, to file, compressed, read named dir} {
    file delete -force $testj
    list [catch {eval $JAR -cvfM $testj dir1} err] \
	[file exists $testj] [sortLines $err]
} [list 0 1 [sortLines "adding: dir1/ (in=0) (out=0) (stored 0%)
adding: dir1/file3 (in=21) (out=23) (deflated -10%)
adding: dir1/dir2/ (in=0) (out=0) (stored 0%)
adding: dir1/dir2/file4 (in=26) (out=28) (deflated -8%)"]]

test 3.14 {jar create, no manifest, to file, compressed, read . dir} {
    file delete -force $testj
    list [catch {eval $JAR -cvfM $testj .} err] \
	[file exists $testj] [sortLines $err]
} [list 0 1 [sortLines "adding: test/ (in=0) (out=0) (stored 0%)
adding: test/file1 (in=16) (out=18) (deflated -12%)
adding: test/file2 (in=16) (out=18) (deflated -12%)
adding: test/big.data (in=1001000) (out=2900) (deflated 99%)
adding: test/test.manifest (in=28) (out=30) (deflated -7%)
adding: test/dir1/ (in=0) (out=0) (stored 0%)
adding: test/dir1/file3 (in=21) (out=23) (deflated -10%)
adding: test/dir1/dir2/ (in=0) (out=0) (stored 0%)
adding: test/dir1/dir2/file4 (in=26) (out=28) (deflated -8%)"]]

test 3.15 {jar create, no manifest, to file, compressed, read .. dir} {
    file delete -force $testj
    cd dir1
    list [catch {eval $JAR -cvfM ../$testj ..} err] \
	[file exists ../$testj] [cd ..] [sortLines $err]
} [list 0 1 {} [sortLines "adding: test/ (in=0) (out=0) (stored 0%)
adding: test/file1 (in=16) (out=18) (deflated -12%)
adding: test/file2 (in=16) (out=18) (deflated -12%)
adding: test/big.data (in=1001000) (out=2900) (deflated 99%)
adding: test/test.manifest (in=28) (out=30) (deflated -7%)
adding: test/dir1/ (in=0) (out=0) (stored 0%)
adding: test/dir1/file3 (in=21) (out=23) (deflated -10%)
adding: test/dir1/dir2/ (in=0) (out=0) (stored 0%)
adding: test/dir1/dir2/file4 (in=26) (out=28) (deflated -8%)"]]

test 3.16 {jar create, no manifest, to file, compressed, read .. + -C} {
    file delete -force $testj
    list [catch {eval $JAR -cvfM $testj -C dir1 ..} err] \
	[file exists $testj] [sortLines $err]
} [list 0 1 [sortLines "adding: test/ (in=0) (out=0) (stored 0%)
adding: test/file1 (in=16) (out=18) (deflated -12%)
adding: test/file2 (in=16) (out=18) (deflated -12%)
adding: test/big.data (in=1001000) (out=2900) (deflated 99%)
adding: test/test.manifest (in=28) (out=30) (deflated -7%)
adding: test/dir1/ (in=0) (out=0) (stored 0%)
adding: test/dir1/file3 (in=21) (out=23) (deflated -10%)
adding: test/dir1/dir2/ (in=0) (out=0) (stored 0%)
adding: test/dir1/dir2/file4 (in=26) (out=28) (deflated -8%)"]]

# Get the current dir, trim first / off it
set pre  [file dirname [pwd]]
set list [file split $pre]
set list [lrange $list 1 end]
set pre  [join $list /]

test 3.17 {jar create, no manifest, to file, compressed, read absolute dir} {
    file delete -force $testj
    list [catch {eval $JAR -cvfM $testj -C dir1 [pwd]} err] \
	[file exists $testj] [sortLines $err]
} [list 0 1 [sortLines "adding: ${pre}/test/ (in=0) (out=0) (stored 0%)
adding: ${pre}/test/file1 (in=16) (out=18) (deflated -12%)
adding: ${pre}/test/file2 (in=16) (out=18) (deflated -12%)
adding: ${pre}/test/big.data (in=1001000) (out=2900) (deflated 99%)
adding: ${pre}/test/test.manifest (in=28) (out=30) (deflated -7%)
adding: ${pre}/test/dir1/ (in=0) (out=0) (stored 0%)
adding: ${pre}/test/dir1/file3 (in=21) (out=23) (deflated -10%)
adding: ${pre}/test/dir1/dir2/ (in=0) (out=0) (stored 0%)
adding: ${pre}/test/dir1/dir2/file4 (in=26) (out=28) (deflated -8%)"]]

catch {unset pre list}









# test series 4 : listing of jar archives

# first create a test.jar archive the will be used for listing

file delete -force $testj

# if the files are changed at the top of this file we need to change
# the sorted files here too

set sorted_files [list big.data dir1/dir2/file4 dir1/file3 \
		      file1 file2 test.manifest]

if {[catch {eval $JAR -cfM $testj $sorted_files} err]} {
  puts $err
  puts "archive create error, can not test archive listing"
  exit -1
}

test 4.1 {jar list, from stdin} {
    list [catch {eval $JAR -t < $testj} err] $err
} [list 0 [join $sorted_files \n]]


test 4.2 {jar list, verbose output, from stdin} {
    if {[catch {eval $JAR -tv < $testj} err]} {
        error $err
    }
    set result {}
    foreach line [split $err \n] {
	#puts "line is \"$line\""
	if {! [regexp {([ |0-9|-]*) .* .* .* .* .* .* (.*)} \
		   $line whole sub1 sub2]} {
	    error "regexp for line \"$line\" failed"
	}
	lappend result [list [string trim $sub1] $sub2]
    }
    set result
} [list {1001000 big.data} {26 dir1/dir2/file4} \
       {21 dir1/file3} {16 file1} {16 file2} {28 test.manifest}]


test 4.3 {jar list, from file} {
    list [catch {eval $JAR -tf $testj} err] $err
} [list 0 [join $sorted_files \n]]

test 4.4 {jar list, from file, with file arguments} {
    list [catch {eval $JAR -tf $testj file1 big.data} err] $err
} [list 0 "big.data\nfile1"]

test 4.5 {jar list, from file, with nonexistent file arguments} {
    list [catch {eval $JAR -tf $testj file1 bad1 bad2} err] $err
} {0 file1}





# test series 5 : extracting files from jar archive


# proc that works like the rm -rf * command in unix to remove
# all files and directories in the current directory

proc rm-rf { } {
    set files [glob -nocomplain *]
    foreach file $files {file delete -force $file}
}

# proc that checks that all the given files exist

proc files_exist { files } {
    foreach file $files {
	if {! [file exists $file]} {
	    return 0
	}
    }
    return 1
}


file delete -force $testj

# use the sorted_files variable set at the top of test series 4!


# compressed and uncompressed archive names
set com_testj com_test.jar
set un_testj  un_test.jar

if {[catch {eval $JAR -cfM $com_testj $sorted_files} err]} {
  puts $err
  puts "archive create error, can not test compressed archive extraction"
  exit -1
}

if {[catch {eval $JAR -cf0M $un_testj $sorted_files} err]} {
  puts $err
  puts "archive create error, can not test uncompressed archive extraction"
  exit -1
}


file mkdir tmp
cd tmp

# reset name to inlcude ../name
set com_testj ../$com_testj
set un_testj ../$un_testj





test 5.1 {jar extract, from stdin, compressed} {
    rm-rf
    list [catch {eval $JAR -x file1 file2 < $com_testj} err] \
	[files_exist {file1 file2}] $err
} {0 1 {}}

test 5.2 {jar extract, from stdin, not compressed} {
    rm-rf
    list [catch {eval $JAR -x file1 file2 < $un_testj} err] \
	[files_exist {file1 file2}] $err
} {0 1 {}}

test 5.3 {jar extract, from stdin, verbose, compressed} {
    rm-rf
    list [catch {eval $JAR -xv file1 file2 < $com_testj} err] \
	[files_exist {file1 file2}] $err
} {0 1 {  inflated: file1
  inflated: file2}}

test 5.4 {jar extract, from stdin, verbose, not compressed} {
    rm-rf
    list [catch {eval $JAR -xv file1 file2 < $un_testj} err] \
	[files_exist {file1 file2}] $err
} {0 1 { extracted: file1
 extracted: file2}}

test 5.5 {jar extract, from file, compressed} {
    rm-rf
    list [catch {eval $JAR -xf $com_testj file1 file2} err] \
	[files_exist {file1 file2}] $err
} {0 1 {}}

test 5.6 {jar extract, from file, not compressed} {
    rm-rf
    list [catch {eval $JAR -xf $un_testj file1 file2} err] \
	[files_exist {file1 file2}] $err
} {0 1 {}}

test 5.7 {jar extract, from file, verbose, compressed} {
    rm-rf
    list [catch {eval $JAR -xvf $com_testj file1 file2} err] \
	[files_exist {file1 file2}] $err
} {0 1 {  inflated: file1
  inflated: file2}}

test 5.8 {jar extract, from file, verbose, not compressed} {
    rm-rf
    list [catch {eval $JAR -xvf $un_testj file1 file2} err] \
	[files_exist {file1 file2}] $err
} {0 1 { extracted: file1
 extracted: file2}}

test 5.9 {jar extract, from file, compressed, all files} {
    rm-rf
    list [catch {eval $JAR -xf $com_testj} err] \
	[files_exist $sorted_files] $err
} {0 1 {}}

test 5.10 {jar extract, from file, not compressed, all files} {
    rm-rf
    list [catch {eval $JAR -xf $un_testj} err] \
	[files_exist $sorted_files] $err
} {0 1 {}}

test 5.11 {jar extract, from file, verbose, compressed, all files} {
    rm-rf
    list [catch {eval $JAR -xvf $com_testj} err] \
	[files_exist $sorted_files] $err
} {0 1 {  inflated: big.data
  inflated: dir1/dir2/file4
  inflated: dir1/file3
  inflated: file1
  inflated: file2
  inflated: test.manifest}}

test 5.12 {jar extract, from file, verbose, not compressed, all files} {
    rm-rf
    list [catch {eval $JAR -xvf $un_testj} err] \
	[files_exist $sorted_files] $err
} {0 1 { extracted: big.data
 extracted: dir1/dir2/file4
 extracted: dir1/file3
 extracted: file1
 extracted: file2
 extracted: test.manifest}}

test 5.13 {jar extract, from file, compressed, with -C} {
    rm-rf
    file mkdir tmp2
    list [catch {eval $JAR -xf $com_testj -C tmp2 file1 file2} err] \
	[files_exist {tmp2/file1 tmp2/file2}] $err
} {0 1 {}}

test 5.14 {jar extract, from file, compressed, with -C} {
    rm-rf
    file mkdir tmp2
    list [catch {eval $JAR -xf $com_testj file1 -C tmp2 file2} err] \
	[files_exist {file1 tmp2/file2}] $err
} {0 1 {}}

test 5.15 {jar extract, from file, compressed, multiple dirs in name} {
    rm-rf
    list [catch {eval $JAR -xf $com_testj dir1/dir2/file4} err] \
	[files_exist {dir1/dir2/file4}] $err
} {0 1 {}}


#clean up stuff used in test series 5

cd ..
file delete -force $com_testj $un_testj tmp













if {0} {




# extract uncompressed archive

eval $JAR -xvf $un_testj


# extract compressed archive

eval $JAR -xvf $com_testj





eval $JAR -cvf0 $testj $sorted_files

eval jar -cvf0 $testj $sorted_files

eval $JAR -cvf $testj $sorted_files

eval jar -cvf $testj $sorted_files




eval $JAR -xvf $testj
  inflated: big.data
  inflated: dir1/dir2/file4
  inflated: dir1/file3
  inflated: file1
  inflated: file2
  inflated: test.manifest


jar -xvf $testj
 extracted: big.data
 extracted: dir1/dir2/file4
 extracted: dir1/file3
 extracted: file1
 extracted: file2
 extracted: test.manifest




# with no compression the extract message is

   created: META-INF/
  inflated: META-INF/MANIFEST.MF


# with compression the extract message is

   created: META-INF/
 extracted: META-INF/MANIFEST.MF







> /dev/null
>& /dev/null
2> /dev/null


writeToFile out1 $err

writeToFile out2 $USAGE

exec diff -u out1 out2



#failing in JDK 1.2 ???

exec java kaffe.jar.Jar -tf test.jar

exec java kaffe.jar.Jar -t < test.jar


exec /soft/java/JDK-1.1.6/bin/java -verbose kaffe.jar.Jar -tf test.jar


exec /soft/java/JDK-1.1.6/bin/java_g -tm kaffe.jar.Jar -tf test.jar


javac -d /tmp/mo/build /tmp/mo/tcljava/bugs/jar/Jar.java


}

