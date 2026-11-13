package ca.jarvis.bridge;

/** Conservative policy: no inferred app closure based solely on backgrounding. */
final class XtoolCloseDetectionPolicy {
    static final int ABSENT_SAMPLES_TO_CONFIRM = 3;
    private XtoolCloseDetectionPolicy() { }

    /** A null sample means unavailable/unknown; never treat it as closed. */
    static int nextAbsences(boolean previouslySeenRunning, int prior,
                            Boolean runningNow) {
        if (!previouslySeenRunning || runningNow == null
                || runningNow.booleanValue()) return 0;
        return Math.min(ABSENT_SAMPLES_TO_CONFIRM, Math.max(0, prior) + 1);
    }

    static boolean shouldFinish(boolean previouslySeenRunning, int absentSamples) {
        return previouslySeenRunning
                && absentSamples >= ABSENT_SAMPLES_TO_CONFIRM;
    }
}
