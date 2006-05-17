/*
 * JaclSetInterrupted.java --
 *
 *	This file contains loads classes needed by the Jacl
 *	test suite.
 *
 * Copyright (c) 2006 Mo DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JaclSetInterrupted.java,v 1.2 2006/05/17 01:59:11 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.*;

public class JaclSetInterrupted {

// Test method that will create an Interp in a new thread
// and eval some code in the interp.

public static
boolean ThreadInterrupt1() throws Exception {
    ThreadInterrupt1Runner r = new ThreadInterrupt1Runner();
    Thread thr = new Thread(r);
    thr.start();
    thr.join(); // Wait for thread to die
    return r.passed;
}

static class ThreadInterrupt1Runner implements Runnable {
    Interp interp;
    boolean passed = false;

    public void run() {
        try {
            interp = new Interp();

            // Load util Tcl commands and setup interp.

            interp.eval("source SetInterrupted.tcl", 0);
            interp.eval("setup", 0);
        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        }

        // Invoke command, this invocation should be interrupted
        // at loop 50.

        try {
            interp.eval("set loop 0", 0);
            interp.eval("ti1_cmd2", 0);
        } catch (TclException te) {
            // Should never get here
            te.printStackTrace(System.err);
        } catch (TclInterruptedException te) {
            passed = true;
        } finally {
            interp.dispose();
        }
        return;
    }
}


// Test method that will create an Interp in a new thread
// and start a loop that should be interrupted.

public static
boolean ThreadInterrupt2() throws Exception {
    ThreadInterrupt2Runner r = new ThreadInterrupt2Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter while loop, then set
    // interrupt in the interp.

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp.setInterrupted();

    thr.join(); // Wait for other thread to die
    return r.passed;
}

static class ThreadInterrupt2Runner implements Runnable {
    Interp interp;
    boolean passed = false;
    boolean running = false;

    public void run() {
        try {
            interp = new Interp();

            // Load util Tcl commands and setup interp.

            interp.eval("source SetInterrupted.tcl", 0);
            interp.eval("setup", 0);
        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        }

        // Invoke command, this invocation should be interrupted
        // at some point during the while loop. The other thread
        // will set the interrupt flag by invoking interp.setInterrupt().

        try {
            interp.eval("set loop 0", 0);
            running = true;
            interp.eval("ti2_cmd2", 0);
        } catch (TclException te) {
            // Should never get here
            te.printStackTrace(System.err);
        } catch (TclInterruptedException te) {
            passed = true;
        } finally {
            interp.dispose();
        }
        return;
    }

    // Return true when Tcl code has entered while loop

    public boolean isRunning() {
        return running;
    }
}

// Test method that will create an Interp in a new thread
// and then process events from the Tcl event queue.

public static
boolean ThreadInterrupt3() throws Exception {
    ThreadInterrupt3Runner r = new ThreadInterrupt3Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp.setInterrupted();

    thr.join(); // Wait for thread to die
    return r.passed;
}

static class ThreadInterrupt3Runner implements Runnable {
    Interp interp;
    boolean passed = false;
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp = new Interp();

            // Load util Tcl commands and setup interp.

            interp.eval("source SetInterrupted.tcl", 0);
            interp.eval("setup", 0);
        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        }
        if (debug) {
            System.out.println("interp was setup");
        }

        // Process events from the Tcl event queue, if execution
        // is interrupted then stop processing events. Since
        // there should be no events to process, this loop
        // should just wait in the doOneEvent call.

        Notifier notifier = interp.getNotifier();
        try {
            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;
                notifier.doOneEvent(TCL.ALL_EVENTS);
            }
        } catch (TclInterruptedException te) {
            if (debug) {
                System.out.println("caught TclInterruptedException");
                te.printStackTrace(System.out);
            }

            passed = true;

            // Note that this method does not attempt to remove
            // events from the event queue.

        } finally {
            if (debug) {
                System.out.println("Interp.dispose()");
            }

            interp.dispose();
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create an Interp in a new thread
// and then process events from the Tcl event queue.

public static
boolean ThreadInterrupt4() throws Exception {
    ThreadInterrupt4Runner r = new ThreadInterrupt4Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp.setInterrupted();

    thr.join(); // Wait for thread to die
    return r.passed;
}

static class ThreadInterrupt4Runner implements Runnable {
    Interp interp;
    boolean passed = false;
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp = new Interp();

            // Load util Tcl commands and setup interp.

            interp.eval("source SetInterrupted.tcl", 0);
            interp.eval("setup", 0);
            interp.eval("ti4_cmd3", 0);
        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        }
        if (debug) {
            System.out.println("interp was setup");
        }

        // Process events from the Tcl event queue, if execution
        // is interrupted then stop processing events. This
        // method should process an event that was added to
        // the event queue by ti4_cmd3.

        Notifier notifier = interp.getNotifier();
        if (debug) {
            if (notifier == null) {
                System.out.println("interp.getNotifier() returned null");
            } else {
                System.out.println("interp.getNotifier() returned non-null");
            }
        }
        try {
            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;
                notifier.doOneEvent(TCL.ALL_EVENTS);
            }
        } catch (TclInterruptedException te) {
            if (debug) {
                System.out.println("caught TclInterruptedException");
                te.printStackTrace(System.out);
            }

            passed = true;

            // Note that this method does not attempt to remove
            // events from the event queue.

        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            if (debug) {
                System.out.println("Interp.dispose()");
            }

            interp.dispose();
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create an Interp in a new thread
// and then process events from the Tcl event queue.
// This test will enter a loop and then invoke vwait.

public static
boolean ThreadInterrupt5() throws Exception {
    ThreadInterrupt5Runner r = new ThreadInterrupt5Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp.setInterrupted();

    thr.join(); // Wait for thread to die
    return r.passed;
}

static class ThreadInterrupt5Runner implements Runnable {
    Interp interp;
    boolean passed = false;
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp = new Interp();

            // Load util Tcl commands and setup interp.

            interp.eval("source SetInterrupted.tcl", 0);
            interp.eval("setup", 0);
            interp.eval("ti5_cmd3", 0);
        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        }
        if (debug) {
            System.out.println("interp was setup");
        }

        // Process events from the Tcl event queue, if execution
        // is interrupted then stop processing events. This
        // method should process an event that was added to
        // the event queue by ti5_cmd3.

        Notifier notifier = interp.getNotifier();
        try {
            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;
                notifier.doOneEvent(TCL.ALL_EVENTS);
            }
        } catch (TclInterruptedException te) {
            if (debug) {
                System.out.println("caught TclInterruptedException");
                te.printStackTrace(System.out);
            }

            passed = true;

            // Note that this method does not attempt to remove
            // events from the event queue.

        } finally {
            if (debug) {
                System.out.println("Interp.dispose()");
            }

            interp.dispose();
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create an Interp in a new thread
// and then process events from the Tcl event queue.
// The event that will be processed will also process
// events in a vwait. One of the events it will process
// should interrupt the interp, so the vwait will never
// finish.

public static
boolean ThreadInterrupt6() throws Exception {
    ThreadInterrupt6Runner r = new ThreadInterrupt6Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {}

    while (! r.isRunning()) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }
    r.interp.setInterrupted();

    thr.join(); // Wait for thread to die
    return r.passed;
}

static class ThreadInterrupt6Runner implements Runnable {
    Interp interp;
    boolean passed = false;
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp = new Interp();

            // Load util Tcl commands and setup interp.

            interp.eval("source SetInterrupted.tcl", 0);
            interp.eval("setup", 0);
            interp.eval("ti6_cmd2", 0);
        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        }
        if (debug) {
            System.out.println("interp was setup");
        }

        // Process events from the Tcl event queue, if execution
        // is interrupted then stop processing events. This
        // method should process an event that was added to
        // the event queue by ti6_cmd2.

        Notifier notifier = interp.getNotifier();
        try {
            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;
                notifier.doOneEvent(TCL.ALL_EVENTS);
            }
        } catch (TclInterruptedException te) {
            if (debug) {
                System.out.println("caught TclInterruptedException");
                te.printStackTrace(System.out);
            }

            passed = true;

            // Note that this method does not attempt to remove
            // events from the event queue.

        } finally {
            if (debug) {
                System.out.println("Interp.dispose()");
            }

            interp.dispose();
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create an Interp in a new thread
// and then process events from the Tcl event queue.
// The event will enter a loop and then be interrupted
// from the other thread. This method tests the
// TclInterruptedException class because it gets the
// interp that generated the interrupted exception
// and it then invokes the Interp.disposeInterrupted()
// method to cleanup and dispose of the interp.

public static
boolean ThreadInterrupt7() throws Exception {
    ThreadInterrupt7Runner r = null;

    try {
    r = new ThreadInterrupt7Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp.setInterrupted();

    thr.join(); // Wait for thread to die
    } catch (NullPointerException e) {
        // Don't know why this would be generated
        e.printStackTrace(System.err);
    }

    return r.passed;
}

static class ThreadInterrupt7Runner implements Runnable {
    Interp interp;
    boolean passed = false;
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp = new Interp();

            // Load util Tcl commands and setup interp.

            interp.eval("source SetInterrupted.tcl", 0);
            interp.eval("setup", 0);
            interp.eval("ti7_cmd3", 0);
        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }

        if (debug) {
            System.out.println("interp was setup");
        }

        // Process events from the Tcl event queue, if execution
        // is interrupted then stop processing events. This
        // method should process an event that was added to
        // the event queue by ti7_cmd3. This implementation
        // will query the Interp object that was interrupted
        // from the TclInterruptedException.

        Notifier notifier = interp.getNotifier();
        while (true) {
            if (debug) {
                System.out.println("invoking notifier.doOneEvent()");
            }

            running = true;

            try {
                notifier.doOneEvent(TCL.ALL_EVENTS);
            } catch (TclInterruptedException te) {
                if (debug) {
                    System.out.println("caught TclInterruptedException");
                    te.printStackTrace(System.out);
                }

                passed = true;

                te.disposeInterruptedInterp();

                // Break out of the event processing loop
                // since this thread has only one interp.
                break;
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return;
            }

        }

        // Note that there is no way to exit the loop above
        // without catching a TclInterruptedException and
        // disposing of the interp.

        return;
    }

    public boolean isRunning() {
        return running;
    }

}


// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This will test interruption of one of the Interps
// while the other continues to run.

public static
boolean ThreadInterrupt20() throws Exception {
    ThreadInterrupt20Runner r = new ThreadInterrupt20Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    try {
        Thread.sleep(50);
    } catch (InterruptedException e) {}

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp1.setInterrupted();

    thr.join(); // Wait for thread to die
    return r.passed;
}

static class ThreadInterrupt20Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean passed = false;
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti20_cmd3 " + "ONE", 0);

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti20_cmd3 " + "TWO", 0);

        } catch (TclException te) {
            // Should never be reached
            te.printStackTrace(System.err);
            return;
        }
        if (debug) {
            System.out.println("interps were setup");
        }

        // Both interps use the same Notifier since there
        // is one Notifier for each thread.

        Notifier notifier = interp1.getNotifier();
        if (debug) {
            boolean nmatch = (notifier == interp2.getNotifier());
            System.out.println("Notifier match is " + nmatch);
        }

        while (true) {
            if (debug) {
                System.out.println("invoking notifier.doOneEvent()");
            }

            running = true;

            try {
                notifier.doOneEvent(TCL.ALL_EVENTS);
            } catch (TclInterruptedException te) {
                if (debug) {
                    System.out.println("caught TclInterruptedException");
                    te.printStackTrace(System.out);
                }

                // Double check that the interrupted interp is
                // the correct one.

                if (te.interp == interp1) {
                    if (debug) {
                        System.out.println("interp1 interrupted");
                    }

                    if (passed == true) {
                        // Interp should not be interrupted a second time.
                        // This might happen if events were not removed
                        // from the event queue.

                        throw new TclRuntimeError("TclInterruptedException was " +
                            "raised more than once for the same interp");
                    }
                    passed = true;
                } else if (te.interp == interp2) {
                    if (debug) {
                        System.out.println("interp2 interrupted");
                    }
                }

                // Cleanup and then dispose of Interp object.
                // This method is invoked after the stack
                // has been fully unwound and any cleanup
                // logic in each stack frame has been run.

                te.disposeInterruptedInterp();
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return;
            }

            // Break out of the event processing loop
            // when the last Interp object in this
            // thread has been disposed of. This call
            // should really be done in the while
            // condition, it is only here so that
            // debug output can print the condition.

            if (debug) {
                System.out.println("notifier.hasActiveInterps() is " +
                    notifier.hasActiveInterps());
                System.out.println("notifier.refCount is " + notifier.refCount);
            }

            if (!notifier.hasActiveInterps()) {
                break;
            }
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This will test interruption of one of the Interps
// while the other continues to run.

public static
String ThreadInterrupt21() throws Exception {
    ThreadInterrupt21Runner r = new ThreadInterrupt21Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    try {
        Thread.sleep(150);
    } catch (InterruptedException e) {}

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp1.setInterrupted();

    thr.join(); // Wait for thread to die
    
    return r.results.toString();
}

static class ThreadInterrupt21Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean interrupted_one = false;
    boolean interrupted_two = false;
    StringBuffer results = new StringBuffer();
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti21_cmd3 " + "ONE", 0);
            results.append("SETUP_ONE ");

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti21_cmd3 " + "TWO", 0);
            results.append("SETUP_TWO ");

            if (debug) {
                System.out.println("interps were setup");
            }

            // Both interps use the same Notifier since there
            // is one Notifier for each thread.

            Notifier notifier = interp1.getNotifier();

            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;

                try {
                    notifier.doOneEvent(TCL.ALL_EVENTS);
                } catch (TclInterruptedException te) {
                    if (debug) {
                        System.out.println("caught TclInterruptedException");
                        te.printStackTrace(System.out);
                    }

                    // Double check that the interrupted interp is
                    // the correct one.

                    if (te.interp == interp1) {
                        if (debug) {
                            System.out.println("interp1 interrupted");
                        }

                        if (interrupted_one == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_one = true;
                        results.append("INTERRUPTED_ONE ");
                    } else if (te.interp == interp2) {
                        if (debug) {
                            System.out.println("interp2 interrupted");
                        }

                        if (interrupted_two == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_two = true;
                        results.append("INTERRUPTED_TWO ");
                    }

                    // Cleanup and then dispose of Interp object.
                    // This method is invoked after the stack
                    // has been fully unwound and any cleanup
                    // logic in each stack frame has been run.

                    te.disposeInterruptedInterp();
                }

                // Break out of the event processing loop
                // when the last Interp object in this
                // thread has been disposed of.

                if (debug) {
                    System.out.println("notifier.hasActiveInterps() is " +
                        notifier.hasActiveInterps());
                }

                if (!notifier.hasActiveInterps()) {
                    results.append("END_EVENT_LOOP");
                    break;
                }
            }

        } catch (Exception e) {
            // Should never be reached
            e.printStackTrace(System.err);
        }

        if (debug) {
            System.out.println("end results are : " + results.toString());
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This will test interruption of one of the Interps
// while the other continues to run. While the
// first stack is being unwound, additional Tcl
// code will be executed.

public static
String ThreadInterrupt22() throws Exception {
    ThreadInterrupt22Runner r = new ThreadInterrupt22Runner();
    Thread thr = new Thread(r);
    thr.start();

    // Wait for other thread to enter event processing loop
    // and then set the interrupt in the interp.

    try {
        Thread.sleep(500);
    } catch (InterruptedException e) {}

    while (! r.isRunning()) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}
    }
    r.interp1.setInterrupted();

    thr.join(); // Wait for thread to die

    return r.results.toString();
}

static class ThreadInterrupt22Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean interrupted_one = false;
    boolean interrupted_two = false;
    StringBuffer results = new StringBuffer();
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti22_cmd3 " + "ONE", 0);
            results.append("SETUP_ONE ");

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti22_cmd3 " + "TWO", 0);
            results.append("SETUP_TWO ");

            if (debug) {
                System.out.println("interps were setup");
            }

            // Both interps use the same Notifier since there
            // is one Notifier for each thread.

            Notifier notifier = interp1.getNotifier();

            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;

                try {
                    notifier.doOneEvent(TCL.ALL_EVENTS);
                } catch (TclInterruptedException te) {
                    if (debug) {
                        System.out.println("caught TclInterruptedException");
                        te.printStackTrace(System.out);
                    }

                    // Double check that the interrupted interp is
                    // the correct one.

                    if (te.interp == interp1) {
                        if (debug) {
                            System.out.println("interp1 interrupted");
                        }

                        if (interrupted_one == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_one = true;
                        results.append("INTERRUPTED_ONE ");

                        // Query the "results" variable.

                        TclObject tobj = interp1.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    } else if (te.interp == interp2) {
                        if (debug) {
                            System.out.println("interp2 interrupted");
                        }

                        if (interrupted_two == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_two = true;
                        results.append("INTERRUPTED_TWO ");

                        // Query the "results" variable.

                        TclObject tobj = interp2.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    }

                    // Cleanup and then dispose of Interp object.
                    // This method is invoked after the stack
                    // has been fully unwound and any cleanup
                    // logic in each stack frame has been run.

                    te.disposeInterruptedInterp();
                }

                // Break out of the event processing loop
                // when the last Interp object in this
                // thread has been disposed of.

                if (debug) {
                    System.out.println("notifier.hasActiveInterps() is " +
                        notifier.hasActiveInterps());
                }

                if (!notifier.hasActiveInterps()) {
                    results.append("END_EVENT_LOOP");
                    break;
                }
            }

        } catch (Exception e) {
            // Should never be reached
            e.printStackTrace(System.err);
        }

        if (debug) {
            System.out.println("end results are : " + results.toString());
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This will test interruption of one of the Interps
// while the other continues to run. While the
// first stack is being unwound, additional Tcl
// code will be executed.

public static
String ThreadInterrupt23() throws Exception {
    ThreadInterrupt23Runner r = new ThreadInterrupt23Runner();
    Thread thr = new Thread(r);
    thr.start();

    // No need to interrupt here since both interps
    // will interrupt themselves.

    thr.join(); // Wait for thread to die

    return r.results.toString();
}

static class ThreadInterrupt23Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean interrupted_one = false;
    boolean interrupted_two = false;
    StringBuffer results = new StringBuffer();
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti23_cmd3 " + "ONE", 0);
            results.append("SETUP_ONE ");

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti23_cmd3 " + "TWO", 0);
            results.append("SETUP_TWO ");

            if (debug) {
                System.out.println("interps were setup");
            }

            // Both interps use the same Notifier since there
            // is one Notifier for each thread.

            Notifier notifier = interp1.getNotifier();

            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;

                try {
                    notifier.doOneEvent(TCL.ALL_EVENTS);
                } catch (TclInterruptedException te) {
                    if (debug) {
                        System.out.println("caught TclInterruptedException");
                        te.printStackTrace(System.out);
                    }

                    // Double check that the interrupted interp is
                    // the correct one.

                    if (te.interp == interp1) {
                        if (debug) {
                            System.out.println("interp1 interrupted");
                        }

                        if (interrupted_one == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_one = true;
                        results.append("INTERRUPTED_ONE ");

                        // Query the "results" variable.

                        TclObject tobj = interp1.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    } else if (te.interp == interp2) {
                        if (debug) {
                            System.out.println("interp2 interrupted");
                        }

                        if (interrupted_two == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_two = true;
                        results.append("INTERRUPTED_TWO ");

                        // Query the "results" variable.

                        TclObject tobj = interp2.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    }

                    // Cleanup and then dispose of Interp object.
                    // This method is invoked after the stack
                    // has been fully unwound and any cleanup
                    // logic in each stack frame has been run.

                    te.disposeInterruptedInterp();
                }

                // Break out of the event processing loop
                // when the last Interp object in this
                // thread has been disposed of.

                if (debug) {
                    System.out.println("notifier.hasActiveInterps() is " +
                        notifier.hasActiveInterps());
                }

                if (!notifier.hasActiveInterps()) {
                    results.append("END_EVENT_LOOP");
                    break;
                }
            }

        } catch (Exception e) {
            // Should never be reached
            e.printStackTrace(System.err);
        }

        if (debug) {
            System.out.println("end results are : " + results.toString());
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This will test interruption of one of the Interps
// while the other continues to run. While the
// first stack is being unwound multiple finally
// blocks should be executed.

public static
String ThreadInterrupt24() throws Exception {
    ThreadInterrupt24Runner r = new ThreadInterrupt24Runner();
    Thread thr = new Thread(r);
    thr.start();

    // No need to interrupt here since both interps
    // will interrupt themselves.

    thr.join(); // Wait for thread to die

    return r.results.toString();
}

static class ThreadInterrupt24Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean interrupted_one = false;
    boolean interrupted_two = false;
    StringBuffer results = new StringBuffer();
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti24_cmd5 " + "ONE", 0);
            results.append("SETUP_ONE ");

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti24_cmd5 " + "TWO", 0);
            results.append("SETUP_TWO ");

            if (debug) {
                System.out.println("interps were setup");
            }

            // Both interps use the same Notifier since there
            // is one Notifier for each thread.

            Notifier notifier = interp1.getNotifier();

            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;

                try {
                    notifier.doOneEvent(TCL.ALL_EVENTS);
                } catch (TclInterruptedException te) {
                    if (debug) {
                        System.out.println("caught TclInterruptedException");
                        te.printStackTrace(System.out);
                    }

                    // Double check that the interrupted interp is
                    // the correct one.

                    if (te.interp == interp1) {
                        if (debug) {
                            System.out.println("interp1 interrupted");
                        }

                        if (interrupted_one == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_one = true;
                        results.append("INTERRUPTED_ONE ");

                        // Query the "results" variable.

                        TclObject tobj = interp1.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    } else if (te.interp == interp2) {
                        if (debug) {
                            System.out.println("interp2 interrupted");
                        }

                        if (interrupted_two == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_two = true;
                        results.append("INTERRUPTED_TWO ");

                        // Query the "results" variable.

                        TclObject tobj = interp2.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    }

                    // Cleanup and then dispose of Interp object.
                    // This method is invoked after the stack
                    // has been fully unwound and any cleanup
                    // logic in each stack frame has been run.

                    te.disposeInterruptedInterp();

                } catch (Throwable te) {
                    if (debug) {
                        System.out.println("caught Throwable");
                        te.printStackTrace(System.out);
                    }
                }

                // Break out of the event processing loop
                // when the last Interp object in this
                // thread has been disposed of.

                if (debug) {
                    System.out.println("notifier.hasActiveInterps() is " +
                        notifier.hasActiveInterps());
                }

                if (!notifier.hasActiveInterps()) {
                    results.append("END_EVENT_LOOP");
                    break;
                }
            }

        } catch (Exception e) {
            // Should never be reached
            e.printStackTrace(System.err);
        }

        if (debug) {
            System.out.println("end results are : " + results.toString());
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This test checks that a catch can't catch a
// TclInterrupted exception and it adds a couple
// of after commands that should be canceled.

public static
String ThreadInterrupt25() throws Exception {
    ThreadInterrupt25Runner r = new ThreadInterrupt25Runner();
    Thread thr = new Thread(r);
    thr.start();

    // No need to interrupt here since both interps
    // will interrupt themselves.

    thr.join(); // Wait for thread to die

    return r.results.toString();
}

static class ThreadInterrupt25Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean interrupted_one = false;
    boolean interrupted_two = false;
    StringBuffer results = new StringBuffer();
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti25_cmd3 " + "ONE", 0);
            results.append("SETUP_ONE ");

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti25_cmd3 " + "TWO", 0);
            results.append("SETUP_TWO ");

            if (debug) {
                System.out.println("interps were setup");
            }

            // Both interps use the same Notifier since there
            // is one Notifier for each thread.

            Notifier notifier = interp1.getNotifier();

            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;

                try {
                    notifier.doOneEvent(TCL.ALL_EVENTS);
                } catch (TclInterruptedException te) {
                    if (debug) {
                        System.out.println("caught TclInterruptedException");
                        te.printStackTrace(System.out);
                    }

                    // Double check that the interrupted interp is
                    // the correct one.

                    if (te.interp == interp1) {
                        if (debug) {
                            System.out.println("interp1 interrupted");
                        }

                        if (interrupted_one == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_one = true;
                        results.append("INTERRUPTED_ONE ");

                        // Query the "results" variable.

                        TclObject tobj = interp1.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    } else if (te.interp == interp2) {
                        if (debug) {
                            System.out.println("interp2 interrupted");
                        }

                        if (interrupted_two == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_two = true;
                        results.append("INTERRUPTED_TWO ");

                        // Query the "results" variable.

                        TclObject tobj = interp2.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    }

                    // Cleanup and then dispose of Interp object.
                    // This method is invoked after the stack
                    // has been fully unwound and any cleanup
                    // logic in each stack frame has been run.

                    te.disposeInterruptedInterp();

                } catch (Throwable te) {
                    if (debug) {
                        System.out.println("caught Throwable");
                        te.printStackTrace(System.out);
                    }
                }

                // Break out of the event processing loop
                // when the last Interp object in this
                // thread has been disposed of.

                if (debug) {
                    System.out.println("notifier.hasActiveInterps() is " +
                        notifier.hasActiveInterps());
                }

                if (!notifier.hasActiveInterps()) {
                    results.append("END_EVENT_LOOP");
                    break;
                }
            }

        } catch (Exception e) {
            // Should never be reached
            e.printStackTrace(System.err);
        }

        if (debug) {
            System.out.println("end results are : " + results.toString());
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}

// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This test checks that a catch can't catch a
// TclInterrupted exception and it adds a couple
// of after commands that should be canceled.

public static
String ThreadInterrupt26() throws Exception {
    ThreadInterrupt26Runner r = new ThreadInterrupt26Runner();
    Thread thr = new Thread(r);
    thr.start();

    // No need to interrupt here since both interps
    // will interrupt themselves.

    thr.join(); // Wait for thread to die

    return r.results.toString();
}

static class ThreadInterrupt26Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean interrupted_one = false;
    boolean interrupted_two = false;
    StringBuffer results = new StringBuffer();
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti26_cmd3 " + "ONE", 0);
            results.append("SETUP_ONE ");

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti26_cmd3 " + "TWO", 0);
            results.append("SETUP_TWO ");

            if (debug) {
                System.out.println("interps were setup");
            }

            // Both interps use the same Notifier since there
            // is one Notifier for each thread.

            Notifier notifier = interp1.getNotifier();

            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;

                try {
                    notifier.doOneEvent(TCL.ALL_EVENTS);
                } catch (TclInterruptedException te) {
                    if (debug) {
                        System.out.println("caught TclInterruptedException");
                        te.printStackTrace(System.out);
                    }

                    // Double check that the interrupted interp is
                    // the correct one.

                    if (te.interp == interp1) {
                        if (debug) {
                            System.out.println("interp1 interrupted");
                        }

                        if (interrupted_one == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_one = true;
                        results.append("INTERRUPTED_ONE ");

                        // Query the "results" variable.

                        TclObject tobj = interp1.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    } else if (te.interp == interp2) {
                        if (debug) {
                            System.out.println("interp2 interrupted");
                        }

                        if (interrupted_two == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_two = true;
                        results.append("INTERRUPTED_TWO ");

                        // Query the "results" variable.

                        TclObject tobj = interp2.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    }

                    // Cleanup and then dispose of Interp object.
                    // This method is invoked after the stack
                    // has been fully unwound and any cleanup
                    // logic in each stack frame has been run.

                    te.disposeInterruptedInterp();

                } catch (Throwable te) {
                    if (debug) {
                        System.out.println("caught Throwable");
                        te.printStackTrace(System.out);
                    }
                }

                // Break out of the event processing loop
                // when the last Interp object in this
                // thread has been disposed of.

                if (debug) {
                    System.out.println("notifier.hasActiveInterps() is " +
                        notifier.hasActiveInterps());
                }

                if (!notifier.hasActiveInterps()) {
                    results.append("END_EVENT_LOOP");
                    break;
                }
            }

        } catch (Exception e) {
            // Should never be reached
            e.printStackTrace(System.err);
        }

        if (debug) {
            System.out.println("end results are : " + results.toString());
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}


// Test method that will create 2 interps in a thread
// and then process events in the Tcl event queue.
// This test checks that a TJC compiled proc can
// be interrupted.

public static
String ThreadInterrupt30() throws Exception {
    ThreadInterrupt30Runner r = new ThreadInterrupt30Runner();
    Thread thr = new Thread(r);
    thr.start();

    // No need to interrupt here since both interps
    // will interrupt themselves.

    thr.join(); // Wait for thread to die

    return r.results.toString();
}

static class ThreadInterrupt30Runner implements Runnable {
    Interp interp1;
    Interp interp2;
    boolean interrupted_one = false;
    boolean interrupted_two = false;
    StringBuffer results = new StringBuffer();
    boolean running = false;
    final boolean debug = false;

    public void run() {
        try {
            interp1 = new Interp();
            interp2 = new Interp();

            // Load util Tcl commands and setup interps.

            if (debug) {
                System.out.println("starting interp setup");
            }

            interp1.eval("source SetInterrupted.tcl", 0);
            interp1.eval("setup", 0);
            interp1.eval("ti30_setup " + "ONE", 0);
            results.append("SETUP_ONE ");

            if (debug) {
                System.out.println("interp ONE was setup");
            }

            interp2.eval("source SetInterrupted.tcl", 0);
            interp2.eval("setup", 0);
            interp2.eval("ti30_setup " + "TWO", 0);
            results.append("SETUP_TWO ");

            if (debug) {
                System.out.println("interps were setup");
            }

            interp1.eval("ti30_start " + "ONE", 0);
            interp2.eval("ti30_start " + "TWO", 0);

            if (debug) {
                System.out.println("interps were started");
            }

            // Both interps use the same Notifier since there
            // is one Notifier for each thread.

            Notifier notifier = interp1.getNotifier();

            while (true) {
                if (debug) {
                    System.out.println("invoking notifier.doOneEvent()");
                }

                running = true;

                try {
                    notifier.doOneEvent(TCL.ALL_EVENTS);
                } catch (TclInterruptedException te) {
                    if (debug) {
                        System.out.println("caught TclInterruptedException");
                        te.printStackTrace(System.out);
                    }

                    // Double check that the interrupted interp is
                    // the correct one.

                    if (te.interp == interp1) {
                        if (debug) {
                            System.out.println("interp1 interrupted");
                        }

                        if (interrupted_one == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_one = true;
                        results.append("INTERRUPTED_ONE ");

                        // Query the "results" variable.

                        TclObject tobj = interp1.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    } else if (te.interp == interp2) {
                        if (debug) {
                            System.out.println("interp2 interrupted");
                        }

                        if (interrupted_two == true) {
                            // Interp should not be interrupted a second time.
                            // This might happen if events were not removed
                            // from the event queue.

                            throw new TclRuntimeError("TclInterruptedException was " +
                                "raised more than once for the same interp");
                        }

                        interrupted_two = true;
                        results.append("INTERRUPTED_TWO ");

                        // Query the "results" variable.

                        TclObject tobj = interp2.getVar("results", null, 0);
                        results.append("{");
                        results.append(tobj.toString());
                        results.append("} ");
                    }

                    // Cleanup and then dispose of Interp object.
                    // This method is invoked after the stack
                    // has been fully unwound and any cleanup
                    // logic in each stack frame has been run.

                    te.disposeInterruptedInterp();

                } catch (Throwable te) {
                    if (debug) {
                        System.out.println("caught Throwable");
                        te.printStackTrace(System.out);
                    }
                }

                // Break out of the event processing loop
                // when the last Interp object in this
                // thread has been disposed of.

                if (debug) {
                    System.out.println("notifier.hasActiveInterps() is " +
                        notifier.hasActiveInterps());
                }

                if (!notifier.hasActiveInterps()) {
                    results.append("END_EVENT_LOOP");
                    break;
                }
            }

        } catch (Exception e) {
            // Should never be reached
            e.printStackTrace(System.err);
        }

        if (debug) {
            System.out.println("end results are : " + results.toString());
        }

        return;
    }

    public boolean isRunning() {
        return running;
    }

}


} // end class JaclSetInterrupted

