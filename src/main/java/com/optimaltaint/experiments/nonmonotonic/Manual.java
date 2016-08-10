package com.optimaltaint.experiments.nonmonotonic;


import com.optimaltaint.experiments.Experiment;

/*
  Loop propagates taint non-monotonically (i.e. the sources/sinks of taint DO NOT remain
  constant after every iteration)

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
     * Loop creates non-constant source/sink of taint for every iter > 1. In this loop we know
     * that despite the non-monotonic behavior, we iterate in such a way that only totalIters escapes
     * the loop as tainted, as long as i iterates past the "restartTaint" point, if not, it would
     * exit as non-tainted
     * @param maxI
     * @param maxJ
     * @param maxK
     */
    public static int loop(int maxI, int maxJ, int maxK) {
        int i = 0;
        // i should taint j at start (sources i, j)
        int j = i;
        int jCounter = 0;
        int k;
        int totalIters = 0;

        // for first 50% of loop, taint totalIters with j (sources i, j, totalIters)
        // at 50% of loop remove taint from j and use this to remove from totalIters (sources i)
        // between 50% and 80% just increase totalIters with constant (sources i)
        // at >= 80% of loop taint totalIters with i again (sources i, totalIters)
        int initTaint = (int)(maxI * 0.50);
        int restartTaint = (int)(maxI * 0.80);
        // use this to make sure we only set totalIters/j to non-tainted constant once
        int firstTime = 1;

        for( ; i < maxI; i++) {
            // use jcounter to keep alternate count of j, to maintain taint info
            // at start of each loop (we want to avoid j - j, as compiler will likely optimize
            // that to 0 (and thus remove taint info). This might still get JITed away though...
            for (j = j - jCounter, jCounter = j; j < maxJ; j++, jCounter++) {
                for(k = 0; k < maxK; k++) {
                    if (i < initTaint) { // < 50%
                        // taint total iters
                        totalIters += j;
                    } else if (i == initTaint) { // == 50%
                        // totalIters and j are no longer tainted (similarly for jCounter)
                        if (firstTime == 1) {
                            // note that initTaint is a non-tainted "constant"
                            totalIters = j = jCounter = initTaint;
                            firstTime = 0;
                        } else {
                            totalIters += 1;
                        }
                    } else if (i > initTaint && i < restartTaint) { // between 50% and 80%
                        totalIters += 1;
                    } else { // >= 80%
                        totalIters += i;
                    }
                }
            }
        }

        // if we reach point of retaint (which we inevitably will...so this check here is just
        // to illustrate the general case): we tag totalIters as tainted
        if (i >= restartTaint) {
            totalIters = wrap(totalIters, true, TAINT_LABEL);
        }

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
