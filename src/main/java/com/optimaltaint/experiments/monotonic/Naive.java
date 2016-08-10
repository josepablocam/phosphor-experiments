package com.optimaltaint.experiments.monotonic;

import com.optimaltaint.experiments.Experiment;

/*
  Loop propagates taint monotonically (i.e. the sources/sinks of taint remain
  constant after every iteration > 1)

  All variables are generated using phosphor API calls, corresponds to naive case
  where all variables have an associated shadow variable for tracking taint.

  In all cases, x is the intended source of taint and y the "query" variable.

  Calls to wrap(_, false)
  are used to generate an int that is not tracked by phosphor. We on occasion
  do this for x, despite being the source of taint, to delay instrumenting operations.

  The different variants (taintBefore/taintAfter) refer to the various
  locations (relative to the main loop) where taint reaches variables other than
  original source.
*/
public class Naive extends Experiment {
    /**
     * Loop creates constant source/sink of taint (i, totalIters) for every iter > 1
     * @param maxI
     * @param maxJ
     * @param maxK
     */
    public static int loop(int maxI, int maxJ, int maxK) {
        int i = wrap(0, true, TAINT_LABEL);
        int j = wrap(0, true, EMPTY_LABEL);
        int k = wrap(0, true, EMPTY_LABEL);
        int totalIters = wrap(0, true, EMPTY_LABEL);


        for( ; i < maxI; i++) {
            for (j = wrap(0, true, EMPTY_LABEL); j < maxJ; j++) {
                for(k = wrap(0, true, EMPTY_LABEL) ; k < maxK; k++) {
                    totalIters += i;
                }
            }
        }
        return totalIters;
    }


    /**
     * Taint before the loop, should start tracking at true branch of if/else
     */
    public static void taintBefore() {
        // tainted
        int x = wrap(100, true, TAINT_LABEL);
        int y = wrap(100, true, EMPTY_LABEL);
        int z = wrap(200, true, EMPTY_LABEL);

        // z becomes tainted
        if (x > 50) {
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
        assert(isTainted(y));
    }

    /**
     * Taint after the loop, should start tracking after loop call returns
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

        // iters becomes tainted after loop returns, as loop creates taint for return val
        int iters = loop(50, 50, 50) * x;
        System.out.println(iters);
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
        assert(isTainted(y));
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
        taintAfter();
    }
}
