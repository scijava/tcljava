
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import org.codehaus.janino.util.*;
import org.codehaus.janino.util.resource.*;
import org.codehaus.janino.util.enumerator.*;

import java.io.*;
import java.lang.reflect.*;

/**
 * A simplified version of {@link Compiler} that can compile only a single
 * compilation unit. (A "compilation unit" is the characters stored in a
 * ".java" file.)
 * <p>
 * Opposed to a normal ".java" file, you can declare multiple public classes
 * here.
 */
public class SimpleCompiler extends EvaluatorBase {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("    org.codehaus.janino.SimpleCompiler <class-name> <arg> [ ... ]");
            System.err.println("Reads a compilation unit from STDIN and invokes method \"public static void main(String[])\" of");
            System.err.println("class <class-name>.");
            System.exit(1);
        }

        String className = args[0];
        String[] mainArgs = new String[args.length - 1];
        System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

        ClassLoader cl = new SimpleCompiler("STDIN", System.in).getClassLoader();
        Class c = cl.loadClass(className);
        Method m = c.getMethod("main", new Class[] { String[].class });
        m.invoke(null, new Object[] { mainArgs });
    }

    public SimpleCompiler(
        String optionalFileName,
        Reader in
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this(
            new Scanner(optionalFileName, in), // scanner
            (ClassLoader) null                 // optionalParentClassLoader
        );
    }

    public SimpleCompiler(
        String      optionalFileName,
        InputStream is
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this(
            new Scanner(optionalFileName, is), // scanner
            (ClassLoader) null                 // optionalParentClassLoader
        );
    }

    public SimpleCompiler(
        String fileName
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this(
            new Scanner(fileName), // scanner
            (ClassLoader) null     // optionalParentClassLoader
        );
    }

    /**
     * Parse a compilation unit from the given {@link Scanner} object and
     * compile it to a set of Java<sup>TM</sup> classes.
     * 
     * @param scanner Source of tokens
     * @param optionalParentClassLoader Loads referenced classes
     */
    public SimpleCompiler(
        Scanner     scanner,
        ClassLoader optionalParentClassLoader
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        super(optionalParentClassLoader);

        // Parse the compilation unit.
        Java.CompilationUnit compilationUnit = new Parser(scanner).parseCompilationUnit();

        // Compile the classes and load them.
        this.classLoader = this.compileAndLoad(
            compilationUnit,
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION
        );
    }

    /**
     * This SimpleCompiler implementation is used when compiling Java
     * source code stored in a String value. The SimpleCompiler object
     * is created once and the CLASSPATH is setup. Then the compile()
     * method is invoked 1 or more times to compile Java class source
     * contained in a String object. Unlike the other implementations,
     * this version of the SimpleCompiler will not attempt to load the
     * compiled class data into the current thread using the class loader.
     * This implementation will just compile the source code into
     * an array of ClassFile objects.
     */

    boolean noloadSimpleCompiler = false;
    IClassLoader icloader = null;

    public SimpleCompiler()
    {
        super(null);
        this.classLoader = null;
        noloadSimpleCompiler = true;

        // Use context class loader unless explicit class loader is given
        //ClassLoader cloader = null;
        //if (cloader == null) {
        //    Thread cthread = Thread.currentThread();
        //    cloader = cthread.getContextClassLoader();
        //}

/*
        // Load classes from the CLASSPATH
        String classPath = System.getProperty("java.class.path");
        System.out.println("classPath is \"" + classPath + "\"");
        ResourceFinder classPathResourceFinder = new PathResourceFinder(
            PathResourceFinder.parsePath(classPath));

        ResourceFinder classLoaderResourceFinder = new ResourceFinderClassLoader(            
            classPathResourceFinder, cloader);

        IClassLoader icloader = new ResourceFinderIClassLoader(
            //classPathResourceFinder,
            classLoaderResourceFinder,
            null);

        //cloader = new ResourceFinderClassLoader(classPathResourceFinder, cloader);
        //ClassLoaderIClassLoader icloader = new ClassLoaderIClassLoader(cloader);
*/

/*
        // Load classes from the CLASSPATH

        String classPath = System.getProperty("java.class.path");
        System.out.println("classPath is \"" + classPath + "\"");
        ResourceFinder classPathResourceFinder = new PathResourceFinder(
            PathResourceFinder.parsePath(classPath));
        icloader = new ResourceFinderIClassLoader(
            classPathResourceFinder,
            null);
*/

        String classPath = System.getProperty("java.class.path");
        //System.out.println("CLASSPATH is \"" + classPath + "\"");

        icloader = Compiler.createJavacLikePathIClassLoader(
                null, // optionalBootClassPath
                null, // optionalExtDirs
                PathResourceFinder.parsePath(classPath)
        );
    }

    public
    ClassFile[] compile(String javasrc) {
        if (!noloadSimpleCompiler) {
            throw new RuntimeException("SimpleCompiler.compile() can only be used with " +
                "a SimpleCompiler() constructed with no arguments");
        }

        try {
            // FIXME: Not clear that this implementation is saving
            // the Class data read from the CLASSPATH, keeping
            // already read classes around would really speed things
            // on successive compiles. Look more into optimizing this.
            StringReader sreader = new StringReader(javasrc);
            Scanner scanner = new Scanner(null, sreader);
            Parser parser = new Parser(scanner);
            Java.CompilationUnit cunit = parser.parseCompilationUnit();

            UnitCompiler ucompiler = new UnitCompiler(cunit, icloader);
            EnumeratorSet defaultDebug =
                DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION;

            ClassFile[] cfiles = ucompiler.compileUnit(defaultDebug);
            return cfiles;
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Returns a {@link ClassLoader} object through which the previously compiled classes can
     * be accessed. This {@link ClassLoader} can be used for subsequent calls to
     * {@link #SimpleCompiler(Scanner, ClassLoader)} in order to compile compilation units that
     * use types (e.g. declare derived types) declared in the previous one.
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    private final ClassLoader classLoader;
}
