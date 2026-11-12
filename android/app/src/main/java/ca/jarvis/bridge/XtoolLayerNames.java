package ca.jarvis.bridge;

import java.util.regex.Pattern;

/** Etiquettes colorees, strictement d'apres les mots exposes par Android. */
final class XtoolLayerNames {
    private static final Pattern RED = Pattern.compile(
            "(?iu)\\b(rouges?|reds?)\\b");
    private static final Pattern GREEN = Pattern.compile(
            "(?iu)\\b(verts?|vertes?|greens?)\\b");
    private static final Pattern BLUE = Pattern.compile(
            "(?iu)\\b(bleus?|bleues?|blues?)\\b");
    private static final Pattern LAYER = Pattern.compile(
            "(?iu)\\b(calques?|layers?)\\b");

    private XtoolLayerNames() { }

    static String classify(String value) {
        if (value == null) return "AUCUN";
        if (RED.matcher(value).find()) return "ROUGE";
        if (GREEN.matcher(value).find()) return "VERT";
        if (BLUE.matcher(value).find()) return "BLEU";
        if (LAYER.matcher(value).find()) return "CALQUE";
        return "AUCUN";
    }
}
