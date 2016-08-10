package com.optimaltaint.experiments.no;

import com.optimaltaint.experiments.Experiment;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

/*
  Loop propagates no taint.

  All variables are generated using phosphor API calls, corresponds to naive case
  where all variables have an associated shadow variable for tracking taint.

  In all cases, x is the intended source of taint and y the "query" variable.

  Calls to wrap(_, false)
  are used to generate an int that is not tracked by phosphor. We on occasion
  do this for x, despite being the source of taint, to delay instrumenting operations.

  The different variants (noTaint/taintBefore/taintAfter) refer to the various
  locations (relative to the main loop) where taint tracking starts.
*/
public class Naive extends Experiment {
    /**
     * No taint in loop, but naive, so track all variables
     * @param maxI
     * @param maxJ
     * @param maxK
     */
    public static int loop(int maxI, int maxJ, int maxK) {
        int i = wrap(0, true, EMPTY_LABEL);
        int j = wrap(0, true, EMPTY_LABEL);
        int k = wrap(0, true, EMPTY_LABEL);
        int totalIters = wrap(0, true, EMPTY_LABEL);

        for( ; i < maxI; i++) {
            for (j = wrap(0, true, EMPTY_LABEL) ; j < maxJ; j++) {
                for(k = wrap(0, true, EMPTY_LABEL) ; k < maxK; k++) {
                    totalIters += 1;
                }
            }
        }
        return totalIters;
    }

    /**
     * No taint whatsoever, so no instrumentation should really be taking place.
     */
    public static void noTaint() {
        // all variables generated with shadow state
        int x = wrap(100, true, TAINT_LABEL);
        int y = wrap(100, true, EMPTY_LABEL);
        int z = wrap(200, true, EMPTY_LABEL);

        if (x < 50) {
            z = x + z * y;
        } else {
            x = 0;
        }

        int added = z + x;
        printDevNull("added:" + added);


        int iters = loop(50, 50, 50);
        if (iters > 100) {
            y = y * 2;
        } else {
            y = 100;
        }

        iters *= y;
        // query result: no taint 
        y = iters + z;
        printDevNull("iter comp: " + iters);
        printDevNull("y tainted: " + isTainted(y));
        assert(!isTainted(y));
    }


    /**
     * Taint before the loop, should start tracking at true branch of if/else.
     */
    public static void taintBefore() {
        // tainted
        int x = wrap(100, true, TAINT_LABEL);
        int y = wrap(100, true, EMPTY_LABEL);
        int z = wrap(200, true, EMPTY_LABEL);

        // z becomes tainted
        if (x > 50) {
            z = x + z * y;
        } else {
            x = 0;
        }

        int added = z + x;
        printDevNull("added:" + added);

        int iters = loop(50, 50, 50);
        if (iters > 1000) {
            y = y * 2;
        } else {
            y = 100;
        }

        iters *= y;
        // query result: tainted
        y = iters + z;
        printDevNull("iter comp: " + iters);
        printDevNull("y tainted: " + isTainted(y));
        assert(isTainted(y));
    }

    /**
     * Taint after the loop, starts tracking after loop call returns
     */
    public static void taintAfter() {
        int x = wrap(100, true, TAINT_LABEL);
        int y = wrap(100, true, EMPTY_LABEL);
        int z = wrap(200, true, EMPTY_LABEL);

        // false branch now preserves "taint"
        if (x < 50) {
            z = x + z * y;
        } else {
            x = x + 1;
        }

        int added = z + x;
        printDevNull("added:" + added);

        // iters becomes tainted after loop returns
        int iters = loop(50, 50, 50) * x;

        if (iters > 1000) {
            y = y * 2;
        } else {
            y = 100;
        }

        iters *= y;
        // query result: tainted
        y = iters + z;
        printDevNull("iter comp: " + iters);
        printDevNull("y tainted: " + isTainted(y));
        assert(isTainted(y));
    }


    public void timeNoTaint(int reps) {
        for (int i = 0; i < reps; i++) {
            noTaint();
        }
    }

    public void timeTaintBefore(int reps) {
        for (int i = 0; i < reps; i++) {
            taintBefore();
        }
    }

    public void timeTaintAfter(int reps) {
        for (int i = 0; i < reps; i++) {
            taintAfter();
        }
    }

    public static void main(String[] args) {
        noTaint();
    }
}
