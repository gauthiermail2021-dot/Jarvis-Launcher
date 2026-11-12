package ca.jarvis.bridge;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class XtoolAccessibleWordsTest {
    @Test public void exposesAllAccessibleTextsNotJustEasySetHeadings() {
        List<String> words = new ArrayList<>();
        XtoolAccessibleWords.add(words, "Volet EasySet", "Texte");
        XtoolAccessibleWords.add(words, "Paramétrage en un clic", "Texte");
        XtoolAccessibleWords.add(words, "Puissance(%)", "Description");
        XtoolAccessibleWords.add(words, "Vitesse(mm/s)", "Texte");
        XtoolAccessibleWords.add(words, "Passer", "Texte");
        XtoolAccessibleWords.add(words, "60", "Texte");
        XtoolAccessibleWords.add(words, "Volet EasySet", "Texte");
        String result = XtoolAccessibleWords.format(words, 102);
        assertTrue(result.contains("7 textes/descriptions relevés sur 102 nœuds"));
        assertTrue(result.contains("2. Texte : «Paramétrage en un clic»"));
        assertTrue(result.contains("3. Description : «Puissance(%)»"));
        assertTrue(result.contains("6. Texte : «60»"));
        assertTrue(result.contains("7. Texte : «Volet EasySet»"));
    }

    @Test public void normalizesWhitespaceAndKeepsSource() {
        List<String> words = new ArrayList<>();
        XtoolAccessibleWords.add(words, " \n Puissance   ( % ) \t", "Description");
        XtoolAccessibleWords.add(words, "", "Texte");
        XtoolAccessibleWords.add(words, null, "Texte");
        assertEquals(1, words.size());
        assertEquals("Description : «Puissance ( % )»", words.get(0));
    }

    @Test public void boundsEntriesAndLongSingleText() {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < 130; i++)
            XtoolAccessibleWords.add(words,
                    "Texte de contrôle très long " + "x".repeat(120), "Texte");
        assertEquals(XtoolAccessibleWords.MAX_ENTRIES, words.size());
        assertTrue(words.get(0).length() < 125);
        assertTrue(XtoolAccessibleWords.format(words, 240)
                .contains("Limite d'affichage : premiers 100 éléments"));
    }

    @Test public void emptyTreeShowsItsInspectedNodeCount() {
        assertTrue(XtoolAccessibleWords.format(new ArrayList<>(), 102)
                .contains("Nœuds examinés : 102"));
    }
}
