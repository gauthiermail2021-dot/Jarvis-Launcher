package ca.jarvis.bridge;

/** Pure ordering policy: a visible EasySet panel takes priority over a timer. */
final class EasySetScanHandoff {
    enum Decision { DEFER_UNTIL_CURRENT_FINISHES, REPLACE_EXISTING_TIMER, START }

    private EasySetScanHandoff() { }

    static Decision decide(boolean capturing, boolean analyzing,
                           boolean olderTimerWaiting) {
        if (capturing || analyzing) return Decision.DEFER_UNTIL_CURRENT_FINISHES;
        if (olderTimerWaiting) return Decision.REPLACE_EXISTING_TIMER;
        return Decision.START;
    }
}
