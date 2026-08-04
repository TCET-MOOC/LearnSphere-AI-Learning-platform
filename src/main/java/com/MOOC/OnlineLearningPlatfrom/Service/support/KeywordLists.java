package com.MOOC.OnlineLearningPlatfrom.Service.support;

import java.util.List;

/**
 * Hardcoded keyword lists backing the platform's simple, deterministic, keyword-based
 * moderation auto-flagger and sentiment scorer. There is no ML/NLP infrastructure in this
 * project, so this is intentionally a heuristic (substring/phrase matching), not real
 * sentiment analysis or content classification.
 */
public final class KeywordLists {
    private KeywordLists() {}

    /** Bullying/harassment signal words -> FlaggedContent.Reason.BULLYING. */
    public static final List<String> BULLYING_WORDS = List.of(
            "stupid", "idiot", "shut up", "kill yourself", "loser", "hate you", "worthless"
    );

    /** Promotional/spam signal phrases -> FlaggedContent.Reason.SPAM. */
    public static final List<String> SPAM_WORDS = List.of(
            "spam", "buy now", "click here", "free money", "subscribe to my", "visit my website"
    );

    /** Positive sentiment words. */
    public static final List<String> POSITIVE_WORDS = List.of(
            "great", "helpful", "thanks", "thank you", "love", "excellent", "clear", "amazing", "awesome", "good job"
    );

    /** Negative sentiment words: bullying + spam signal words plus generic negatives. */
    public static final List<String> NEGATIVE_WORDS = concatNegatives();

    private static List<String> concatNegatives() {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        list.addAll(BULLYING_WORDS);
        list.addAll(SPAM_WORDS);
        list.addAll(List.of("confusing", "bad", "hate", "boring", "useless", "terrible", "waste of time"));
        return List.copyOf(list);
    }

    /** Counts (case-insensitive, non-overlapping) occurrences of a phrase in text. */
    public static int countOccurrences(String textLower, String phraseLower) {
        if (textLower == null || textLower.isEmpty() || phraseLower == null || phraseLower.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = textLower.indexOf(phraseLower, idx)) != -1) {
            count++;
            idx += phraseLower.length();
        }
        return count;
    }

    /** Total hits of any word in the given list within the text. */
    public static int totalHits(String text, List<String> words) {
        if (text == null || text.isBlank()) return 0;
        String lower = text.toLowerCase();
        int total = 0;
        for (String w : words) {
            total += countOccurrences(lower, w.toLowerCase());
        }
        return total;
    }

    public static boolean containsAny(String text, List<String> words) {
        return totalHits(text, words) > 0;
    }
}
