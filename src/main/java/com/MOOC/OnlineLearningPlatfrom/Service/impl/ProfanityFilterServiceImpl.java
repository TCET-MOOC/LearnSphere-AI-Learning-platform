package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;
import com.MOOC.OnlineLearningPlatfrom.Service.ProfanityFilterService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProfanityFilterServiceImpl implements ProfanityFilterService {

    // High risk severe phrases
    private static final List<String> SEVERE_BULLYING = List.of(
            "kill yourself", "die", "kys", "worthless piece", "hate you all", "threat", "harm yourself"
    );

    // Bullying & insults
    private static final List<String> BULLYING_INSULTS = List.of(
            "stupid", "idiot", "loser", "moron", "dumb", "shut up", "trash", "fool", "ugly", "worthless", "retard"
    );

    // General profanity & offensive slang
    private static final List<String> PROFANITY = List.of(
            "damn", "hell", "crap", "bastard", "bitch", "asshole", "shit", "fuck", "dick", "pussy"
    );

    // Spam & promotional patterns
    private static final List<String> SPAM_PHRASES = List.of(
            "buy now", "click here", "free money", "crypto", "telegram @", "whatsapp me", "visit my website", "subscribe to my"
    );

    // Leetspeak translation map
    private static final Map<Character, Character> LEET_MAP = new HashMap<>();
    static {
        LEET_MAP.put('@', 'a');
        LEET_MAP.put('4', 'a');
        LEET_MAP.put('$', 's');
        LEET_MAP.put('5', 's');
        LEET_MAP.put('1', 'i');
        LEET_MAP.put('!', 'i');
        LEET_MAP.put('0', 'o');
        LEET_MAP.put('3', 'e');
        LEET_MAP.put('7', 't');
        LEET_MAP.put('8', 'b');
    }

    private String normalizeLeetspeak(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            sb.append(LEET_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }

    @Override
    public CensorResult censor(String text) {
        if (text == null || text.isBlank()) {
            return new CensorResult(text, text, true, null, 0.0, false);
        }

        String normalized = normalizeLeetspeak(text);
        String masked = text;
        FlaggedContent.Reason detectedReason = null;
        double toxicityScore = 0.0;
        boolean shouldBlock = false;

        // 1. Check Severe Bullying & Threats (High Risk -> Block)
        for (String phrase : SEVERE_BULLYING) {
            if (normalized.contains(phrase) || text.toLowerCase().contains(phrase)) {
                detectedReason = FlaggedContent.Reason.HIGH_RISK;
                toxicityScore = Math.max(toxicityScore, 0.95);
                shouldBlock = true;
                masked = maskPhrase(masked, phrase);
            }
        }

        // 2. Check Bullying & Insults
        for (String word : BULLYING_INSULTS) {
            if (containsWord(normalized, word) || containsWord(text.toLowerCase(), word)) {
                if (detectedReason == null) detectedReason = FlaggedContent.Reason.BULLYING;
                toxicityScore = Math.max(toxicityScore, 0.70);
                masked = maskWord(masked, word);
            }
        }

        // 3. Check General Profanity
        for (String prof : PROFANITY) {
            if (containsWord(normalized, prof) || containsWord(text.toLowerCase(), prof)) {
                if (detectedReason == null) detectedReason = FlaggedContent.Reason.BULLYING;
                toxicityScore = Math.max(toxicityScore, 0.60);
                masked = maskWord(masked, prof);
            }
        }

        // 4. Check Spam Phrases
        for (String spam : SPAM_PHRASES) {
            if (normalized.contains(spam) || text.toLowerCase().contains(spam)) {
                if (detectedReason == null) detectedReason = FlaggedContent.Reason.SPAM;
                toxicityScore = Math.max(toxicityScore, 0.50);
                masked = maskPhrase(masked, spam);
            }
        }

        boolean isClean = (detectedReason == null);
        return new CensorResult(text, masked, isClean, detectedReason, toxicityScore, shouldBlock);
    }

    @Override
    public boolean containsProhibitedContent(String text) {
        return !censor(text).isClean();
    }

    @Override
    public String mask(String text) {
        return censor(text).maskedText();
    }

    private boolean containsWord(String source, String word) {
        Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b");
        return pattern.matcher(source).find();
    }

    private String maskWord(String text, String word) {
        Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String matched = matcher.group();
            String replacement = maskToken(matched);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskPhrase(String text, String phrase) {
        Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(phrase));
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String matched = matcher.group();
            String replacement = "[censored]";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskToken(String token) {
        if (token.length() <= 2) {
            return "*".repeat(token.length());
        }
        return token.charAt(0) + "*".repeat(token.length() - 2) + token.charAt(token.length() - 1);
    }
}
