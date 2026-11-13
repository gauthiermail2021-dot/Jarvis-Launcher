package ca.jarvis.bridge;

import java.util.List;

/**
 * Conservative gate for one-shot EasySet inspection after a real xTool tap.
 * This class never reads an accessibility node/window, takes screenshots or
 * performs UI gestures. All OCR remains in the already-consented scanner.
 */
final class EasySetPanelTapPolicy {
    static final long NAMED_TAP_COOLDOWN_MS = 1_200L;
    static final long UNNAMED_TAP_COOLDOWN_MS = 8_000L;

    private EasySetPanelTapPolicy() { }

    /**
     * Android often supplies the clicked control's label directly on the
     * TYPE_VIEW_CLICKED record even when canRetrieveWindowContent=false.
     * Never match our own diagnostic or explanatory text.
     */
    static boolean namesEasySetRow(List<CharSequence> eventText,
                                   CharSequence contentDescription) {
        if (isRowLabel(contentDescription)) return true;
        if (eventText != null) {
            for (CharSequence text : eventText) {
                if (isRowLabel(text)) return true;
            }
        }
        return false;
    }

    static boolean hasAnyText(List<CharSequence> eventText,
                              CharSequence contentDescription) {
        if (contentDescription != null
                && contentDescription.toString().trim().length() > 0)
            return true;
        if (eventText != null) {
            for (CharSequence text : eventText) {
                if (text != null && text.toString().trim().length() > 0)
                    return true;
            }
        }
        return false;
    }

    private static boolean isRowLabel(CharSequence raw) {
        if (raw == null) return false;
        String normalized = EasySetLabels.normalize(raw.toString());
        // Typical xTool Android click label is exactly "Volet EasySet".
        // Do not mistake the "Cliquer sur..." helper instruction for a tap.
        return normalized.equals("volet easyset")
                || normalized.startsWith("volet easyset ");
    }

    /**
     * If xTool omits click text for a custom WebView, permit a bounded
     * fallback only until an EasySet panel has been recognized by OCR.
     * One event can lead to at most one consented, passive screenshot.
     */
    static boolean shouldRequest(String pkg, boolean authorizedProjection,
                                 boolean bubbleVisible, boolean namedTap,
                                 boolean textMissing,
                                 boolean panelNeedsInspection,
                                 long nowMs, long lastNamedMs,
                                 long lastUnnamedMs) {
        if (!EasySetForegroundScanPolicy.XCS_PACKAGE.equals(pkg)
                || !authorizedProjection || !bubbleVisible) return false;
        if (namedTap) {
            return nowMs >= lastNamedMs
                    && nowMs - lastNamedMs >= NAMED_TAP_COOLDOWN_MS;
        }
        return textMissing && panelNeedsInspection
                && nowMs >= lastUnnamedMs
                && nowMs - lastUnnamedMs >= UNNAMED_TAP_COOLDOWN_MS;
    }
}
