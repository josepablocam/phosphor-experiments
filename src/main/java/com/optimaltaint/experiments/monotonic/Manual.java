package com.optimaltaint.experiments.monotonic;


import com.optimaltaint.experiments.Experiment;

/*
  Loop propagates taint monotonically (i.e. the sources/sinks of taint remain
  constant after every iteration > 1)

  Manually annotated so that we only instrument at the point necessary.

  In all cases, x is the intended source of taint and y the "query" variable.

  Calls to wrap(_, false)
  are used to generate an int that is not tracked by phosphor. We on occasion
  do this for x, despite being the source of taint, to delay instrumenting operations.

  The different variants (taintBefore/taintAfter) refer to the various
  locations (relative to the main loop) where taint reaches variables other than
  original source.
*/
public class Manual extends Experiment {
    /**
     * Loop creates constant source/sink of taint (i, totalIters) for every iter > 1.
     * We know that totalIters is the only one that escapes the loop as tainted.
     * @param maxI
     * @param maxJ
     * @param maxK
     */
    public static int loop(int maxI, int maxJ, int maxK) {
        int i, j, k, totalIters = 0;

        for(i = 0 ; i < maxI; i++) {
            for (j = 0 ; j < maxJ; j++) {
                for(k = 0 ; k < maxK; k++) {
                    totalIters += i;
                }
            }
        }
        totalIters = wrap(totalIters, true, TAINT_LABEL);
        return totalIters;
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

        // iters becomes tainted after loop returns, as loop creates taint for return val
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
