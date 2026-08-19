package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;

public interface ProfanityFilterService {

    record CensorResult(
            String originalText,
            String maskedText,
            boolean isClean,
            FlaggedContent.Reason detectedReason,
            double toxicityScore,
            boolean shouldBlock
    ) {}

    /**
     * Analyzes and censors a piece of user-generated text.
     * Replaces profane/bullying tokens with asterisks and computes toxicity severity.
     */
    CensorResult censor(String text);

    /**
     * Quick check if text contains prohibited content.
     */
    boolean containsProhibitedContent(String text);

    /**
     * Returns the censored masked string.
     */
    String mask(String text);
}
