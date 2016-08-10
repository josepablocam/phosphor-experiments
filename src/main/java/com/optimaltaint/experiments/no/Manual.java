package com.optimaltaint.experiments.no;


import com.optimaltaint.experiments.Experiment;

/*
  Loop propagates no taint.

  Manually annotated so that we only instrument at the point necessary.

  In all cases, x is the intended source of taint and y the "query" variable.

  Calls to wrap(_, false)
  are used to generate an int that is not tracked by phosphor. We on occasion
  do this for x, despite being the source of taint, to delay instrumenting operations.

  The different variants (noTaint/taintBefore/taintAfter) refer to the various
  locations (relative to the main loop) where taint tracking starts.
*/
public class Manual extends Experiment {
    /**
     * No taint in loop
     * @param maxI
     * @param maxJ
     * @param maxK
     */
    public static int loop(int maxI, int maxJ, int maxK) {
        int i = 0;
        int j;
        int k;
        int totalIters = 0;

        for( ; i < maxI; i++) {
            for (j = 0 ; j < maxJ; j++) {
                for(k = 0 ; k < maxK; k++) {
                    totalIters += 1;
                }
            }
        }
        return totalIters;
    }

    /**
     * No taint whatsoever, so no instrumentation
     */
    public static void noTaint() {
        // don't just put the constants here as javac can optimize away branching below
        int x = wrap(100, false, EMPTY_LABEL);
        int y = wrap(100, false, EMPTY_LABEL);
        int z = wrap(200, false, EMPTY_LABEL);

        // x is overwritten with constant, so "taint" removed, no instrumentation
        // ever needed
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
    }


    /**
     * Taint before the loop, starts tracking at true branch of if/else
     */
    public static void taintBefore() {
        // tainted
        int x = wrap(100, false, EMPTY_LABEL);
        int y = wrap(100, false, EMPTY_LABEL);
        int z = wrap(200, false, EMPTY_LABEL);

        // z becomes tainted
        if (x > 50) {
            // first instrumentation point
            z = wrap(x + z * y, true, TAINT_LABEL);
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
        printDevNull("y taint: " + isTainted(y));
    }

    /**
     * Taint after the loop, starts tracking after loop call returns
     */
    public static void taintAfter() {
        int x = wrap(100, false, EMPTY_LABEL);
        int y = wrap(100, false, EMPTY_LABEL);
        int z = wrap(200, false, EMPTY_LABEL);

        // false branch now preserves "taint"
        if (x < 50) {
            z = x + z * y;
        } else {
            x = x + 1;
        }

        int added = z + x;
        printDevNull("added:" + added);

        // iters becomes tainted after loop returns
        int iters = wrap(loop(50, 50, 50) * x, true, TAINT_LABEL);
        if (iters > 1000) {
            y = y * 2;
        } else {
            iters = 100;
        }

        iters *= y;
        // query result: tainted
        y = iters + z;
        printDevNull("iter comp: " + iters);
        printDevNull("y taint: " + isTainted(y));
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
        taintBefore();
        taintAfter();
    }
}
