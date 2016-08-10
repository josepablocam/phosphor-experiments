package com.optimaltaint.experiments;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import com.google.caliper.SimpleBenchmark;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.PrintStream;


/**
 * Simple abstract class that generalize our experiments. We create some common labels
 * and some printing utilities
 */
public abstract class Experiment extends SimpleBenchmark {
    public final static String TAINT_LABEL = "x";
    public final static String EMPTY_LABEL = null;
    public final static PrintStream DEV_NULL = new PrintStream(new NullOutputStream());
    public final static PrintStream STD_OUT = System.out;

    // Taint if appropriate
    public static int wrap(int i, boolean taint, String label) {
        if (taint) {
            // use constant label for taint, to avoid penalizing
            return MultiTainter.taintedInt(i, label);
        } else {
            return i;
        }
    }

    // check if a number is tainted. Since we have EMPTY_LABEL as null we also
    // check that the dependencies are non-empty before saying it is tainted
    public static boolean isTainted(int i) {
        Taint t = MultiTainter.getTaint(i);
      return t != null && !t.hasNoDependencies();
    }

    public static void printDevNull(String msg) {
        System.setOut(DEV_NULL);
        System.out.println(msg);
    }

    public static void printStdOut(String msg) {
        System.setOut(STD_OUT);
        System.out.println(msg);
    }
}
