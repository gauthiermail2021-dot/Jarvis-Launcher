package ca.jarvis.bridge;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Locates only the short xTool "Passer" control row.
 * Pure Java so geometry can be tested without Android or ML Kit.
 * The crop is a hint for OCR, not proof of a configured machine value.
 */
final class EasySetPassesRegion {
    private static final Pattern PASSES = Pattern.compile(
            "(?iu)^\\s*(?:passer|passage|passages|passes|pass count)\\b");
    final int left, top, right, bottom;
    // Exact OCR label bounds are retained for a second, *numeric-only* crop.
    final int labelTop, labelBottom;

    private EasySetPassesRegion(int left, int top, int right, int bottom,
                                int labelTop, int labelBottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.labelTop = labelTop;
        this.labelBottom = labelBottom;
    }

    /**
     * Numerical field immediately to the left of xTool's chevron.
     * This is a crop HINT based on the detected Passer label, not evidence
     * that a valid number exists. The OCR must still prove the number.
     */
    EasySetPassesRegion numberField(int screenWidth, int screenHeight) {
        int labelHeight = labelBottom - labelTop;
        if (labelHeight < 8) return null;
        int padding = Math.max(8, Math.round(labelHeight * 0.35f));
        int l = Math.max(0, Math.round(screenWidth * 0.845f));
        int r = Math.min(screenWidth, Math.round(screenWidth * 0.935f));
        int t = Math.max(0, labelTop - padding);
        int b = Math.min(screenHeight, labelBottom + padding);
        if (r - l < 12 || b - t < 12) return null;
        return new EasySetPassesRegion(l, t, r, b, labelTop, labelBottom);
    }

    int width() { return right - left; }
    int height() { return bottom - top; }

    static EasySetPassesRegion find(Collection<EasySetValues.Line> ocr,
                                    int screenWidth, int screenHeight) {
        if (ocr == null || screenWidth < 280 || screenHeight < 300) return null;
        EasySetValues.Line best = null;
        for (EasySetValues.Line line : ocr) {
            if (!PASSES.matcher(line.text).find()) continue;
            int h = line.bottom - line.top;
            if (h < 8 || h > screenHeight / 5 || line.right <= line.left) continue;
            if (line.top < 0 || line.bottom > screenHeight) continue;
            // The xTool label is on the left, numeric spinner on the right.
            if (line.left > screenWidth * 0.50f) continue;
            // Prefer the lowest visible "Passer" label if another earlier
            // unrelated text fragment happens to mention passages.
            if (best == null || line.top > best.top) best = line;
        }
        if (best == null) return null;
        int h = best.bottom - best.top;
        // Keep the controls context, but exclude most of the neighboring
        // Vitesse and "Paramétrage en un clic" rows.
        int top = Math.max(0, best.top - Math.max(28, Math.round(0.85f * h)));
        int bottom = Math.min(screenHeight,
                best.bottom + Math.max(60, Math.round(1.8f * h)));
        int left = Math.max(0, Math.round(screenWidth * 0.025f));
        int right = Math.min(screenWidth, Math.round(screenWidth * 0.975f));
        if (right - left < 200 || bottom - top < 50) return null;
        return new EasySetPassesRegion(left, top, right, bottom, best.top, best.bottom);
    }
}
