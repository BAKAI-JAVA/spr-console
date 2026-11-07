package kg.bakai.stopfactor.util;

/** Joins the messages shown to an administrator into longer sentences. */
public final class Sentences {

    private Sentences() {
    }

    /**
     * A sentence continued after a colon: "Укажите «Период с»." in "Правило 2: укажите «Период с».". A word in
     * capitals (an abbreviation such as КИБ) or a quoted name keeps its case.
     */
    public static String lowerFirst(String sentence) {
        if (sentence.length() < 2 || !Character.isUpperCase(sentence.charAt(0)) || Character.isUpperCase(sentence.charAt(1))) {
            return sentence;
        }
        return Character.toLowerCase(sentence.charAt(0)) + sentence.substring(1);
    }
}
