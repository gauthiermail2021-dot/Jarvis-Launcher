package ca.jarvis.bridge;

import android.graphics.Rect;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts ML Kit text to ephemeral, positioned OCR candidates.
 *
 * A complete line is kept for field headings and inline "Puissance: 59" text.
 * Each ML Kit Element is also provided separately. In xTool, the chevron
 * beside a numeric value can prevent parsing the enclosing OCR line, whereas
 * its individual numeric Element may be usable.
 *
 * No screenshot or OCR text is written to disk.
 */
final class EasySetTextExtractor {
    private EasySetTextExtractor() { }

    static List<EasySetValues.Line> extract(Text vision) {
        List<EasySetValues.Line> candidates = new ArrayList<>();
        if (vision == null) return candidates;
        for (Text.TextBlock block : vision.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                add(candidates, line.getText(), line.getBoundingBox());
                for (Text.Element element : line.getElements()) {
                    // Avoid exact duplicate text+position candidates when a line
                    // consists of only one element.
                    if (element.getText() != null
                            && element.getText().equals(line.getText())
                            && sameBounds(line.getBoundingBox(), element.getBoundingBox())) {
                        continue;
                    }
                    add(candidates, element.getText(), element.getBoundingBox());
                }
            }
        }
        return candidates;
    }

    private static void add(List<EasySetValues.Line> candidates,
                            String text, Rect box) {
        if (text == null || text.trim().isEmpty()) return;
        if (box == null) {
            candidates.add(new EasySetValues.Line(text, 0, 0, 0, 0));
        } else {
            candidates.add(new EasySetValues.Line(
                    text, box.left, box.top, box.right, box.bottom));
        }
    }

    private static boolean sameBounds(Rect first, Rect second) {
        return first == second || (first != null && first.equals(second));
    }
}
