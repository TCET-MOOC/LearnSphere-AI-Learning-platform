package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionPost;
import com.MOOC.OnlineLearningPlatfrom.Entity.Message;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.DiscussionPostRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.MessageRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.SentimentService;
import com.MOOC.OnlineLearningPlatfrom.Service.support.KeywordLists;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Simple, deterministic, keyword-based sentiment scorer over DiscussionPost.body + Message.text.
 * There is no ML/NLP infrastructure in this project - this is a heuristic word-count classifier,
 * not real sentiment analysis.
 */
@Service
public class SentimentServiceImpl implements SentimentService {

    private static final String[] PALETTE = {"#0f9d58", "#534AB7", "#0d47a1", "#b06000", "#1D9E75", "#8e24aa"};

    private final DiscussionPostRepository discussionPostRepository;
    private final MessageRepository messageRepository;

    public SentimentServiceImpl(DiscussionPostRepository discussionPostRepository, MessageRepository messageRepository) {
        this.discussionPostRepository = discussionPostRepository;
        this.messageRepository = messageRepository;
    }

    private record Item(String text, Course course) {}

    private enum Label { POSITIVE, NEUTRAL, NEGATIVE }

    private Label classify(String text) {
        int pos = KeywordLists.totalHits(text, KeywordLists.POSITIVE_WORDS);
        int neg = KeywordLists.totalHits(text, KeywordLists.NEGATIVE_WORDS);
        if (pos > neg) return Label.POSITIVE;
        if (neg > pos) return Label.NEGATIVE;
        return Label.NEUTRAL;
    }

    @Override
    public SentimentSummaryDto getSentiment() {
        List<Item> items = new ArrayList<>();
        for (DiscussionPost post : discussionPostRepository.findAll()) {
            if (post.getBody() != null && !post.getBody().isBlank()) {
                items.add(new Item(post.getBody(), post.getCourse()));
            }
        }
        for (Message message : messageRepository.findAll()) {
            if (message.getText() != null && !message.getText().isBlank()) {
                Course course = message.getConversation() != null ? message.getConversation().getCourse() : null;
                items.add(new Item(message.getText(), course));
            }
        }

        long total = items.size();
        long positiveCount = 0, neutralCount = 0, negativeCount = 0;
        for (Item item : items) {
            Label label = classify(item.text());
            if (label == Label.POSITIVE) positiveCount++;
            else if (label == Label.NEGATIVE) negativeCount++;
            else neutralCount++;
        }

        int positivePercent = percent(positiveCount, total);
        int negativePercent = percent(negativeCount, total);
        int neutralPercent = total == 0 ? 0 : Math.max(0, 100 - positivePercent - negativePercent);

        List<CourseTypeSentimentDto> courseTypeSentiment = buildCourseTypeSentiment(items);
        List<NegativeKeywordDto> negativeKeywords = buildNegativeKeywords(items);
        List<TeacherScoreDto> teacherScores = buildTeacherScores(items);
        List<RecommendationDto> recommendations = buildRecommendations(courseTypeSentiment, negativeKeywords, teacherScores);

        return new SentimentSummaryDto(total, positivePercent, neutralPercent, negativePercent,
                courseTypeSentiment, negativeKeywords, recommendations, teacherScores);
    }

    private int percent(long part, long total) {
        if (total == 0) return 0;
        return Math.round(part * 100f / total);
    }

    private List<CourseTypeSentimentDto> buildCourseTypeSentiment(List<Item> items) {
        Map<String, List<Item>> byDept = new LinkedHashMap<>();
        for (Item item : items) {
            if (item.course() == null || item.course().getDepartment() == null || item.course().getDepartment().isBlank()) continue;
            byDept.computeIfAbsent(item.course().getDepartment(), k -> new ArrayList<>()).add(item);
        }

        List<CourseTypeSentimentDto> result = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, List<Item>> entry : byDept.entrySet()) {
            long deptTotal = entry.getValue().size();
            long deptPositive = entry.getValue().stream().filter(it -> classify(it.text()) == Label.POSITIVE).count();
            int deptPercent = percent(deptPositive, deptTotal);
            result.add(new CourseTypeSentimentDto(entry.getKey(), deptPercent, PALETTE[i % PALETTE.length]));
            i++;
        }
        return result;
    }

    private List<NegativeKeywordDto> buildNegativeKeywords(List<Item> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String word : KeywordLists.NEGATIVE_WORDS) {
            int count = 0;
            for (Item item : items) {
                count += KeywordLists.countOccurrences(item.text().toLowerCase(), word.toLowerCase());
            }
            if (count > 0) counts.put(word, count);
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        int max = sorted.isEmpty() ? 0 : sorted.get(0).getValue();

        List<NegativeKeywordDto> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sorted.subList(0, Math.min(8, sorted.size()))) {
            String severity = entry.getValue() >= max * 0.5 ? "high" : entry.getValue() >= max * 0.2 ? "medium" : "low";
            result.add(new NegativeKeywordDto(entry.getKey(), entry.getValue(), severity));
        }
        return result;
    }

    private List<TeacherScoreDto> buildTeacherScores(List<Item> items) {
        Map<UserAccount, List<Item>> byTeacher = new LinkedHashMap<>();
        for (Item item : items) {
            if (item.course() == null || item.course().getTeacher() == null) continue;
            byTeacher.computeIfAbsent(item.course().getTeacher(), k -> new ArrayList<>()).add(item);
        }

        List<TeacherScoreDto> result = new ArrayList<>();
        for (Map.Entry<UserAccount, List<Item>> entry : byTeacher.entrySet()) {
            UserAccount teacher = entry.getKey();
            long comments = entry.getValue().size();
            long negative = entry.getValue().stream().filter(it -> classify(it.text()) == Label.NEGATIVE).count();
            double negativeRatio = comments == 0 ? 0 : (double) negative / comments;
            int positivePercent = (int) Math.round((1 - negativeRatio) * 100);
            String status = negativeRatio > 0.15 ? "warning" : "positive";
            result.add(new TeacherScoreDto(initials(teacher.getFullName()), teacher.getFullName(), comments,
                    positivePercent + "% positive", status));
        }
        result.sort((a, b) -> Long.compare(b.getComments(), a.getComments()));
        return result.size() > 8 ? result.subList(0, 8) : result;
    }

    private List<RecommendationDto> buildRecommendations(List<CourseTypeSentimentDto> courseTypeSentiment,
                                                           List<NegativeKeywordDto> negativeKeywords,
                                                           List<TeacherScoreDto> teacherScores) {
        List<RecommendationDto> recs = new ArrayList<>();

        if (!negativeKeywords.isEmpty()) {
            NegativeKeywordDto top = negativeKeywords.get(0);
            recs.add(new RecommendationDto(
                    "\"" + top.getWord() + "\" flagged across the platform",
                    top.getCount() + " comments/messages contain this keyword",
                    "Review posts", "btn-outline-grey"));
        }

        courseTypeSentiment.stream()
                .min(Comparator.comparingInt(CourseTypeSentimentDto::getPositive))
                .filter(c -> c.getPositive() < 60)
                .ifPresent(c -> recs.add(new RecommendationDto(
                        "Sentiment dip in " + c.getType() + " courses",
                        c.getPositive() + "% positive sentiment in this course type",
                        "Notify teachers", "btn-outline-amber")));

        teacherScores.stream()
                .filter(t -> "warning".equals(t.getStatus()))
                .findFirst()
                .ifPresent(t -> recs.add(new RecommendationDto(
                        "Elevated negative feedback for " + t.getName(),
                        t.getComments() + " comments analysed, negative ratio above 15%",
                        "Notify teacher", "btn-outline-amber")));

        return recs.size() > 3 ? recs.subList(0, 3) : recs;
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}
