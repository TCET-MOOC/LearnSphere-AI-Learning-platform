package com.MOOC.OnlineLearningPlatfrom.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Service
public class NvidiaNimService {

    private static final Logger log = LoggerFactory.getLogger(NvidiaNimService.class);
    private static final String DEFAULT_BASE_URL = "https://integrate.api.nvidia.com/v1";
    private static final String DEFAULT_MODEL = "meta/llama-3.1-70b-instruct";

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NvidiaNimService(
            @Value("${app.ai.nvidia.api-key:${NVIDIA_NIM_API_KEY:}}") String configuredApiKey,
            @Value("${app.ai.nvidia.base-url:${NVIDIA_NIM_BASE_URL:https://integrate.api.nvidia.com/v1}}") String baseUrl,
            @Value("${app.ai.nvidia.model:${NVIDIA_NIM_MODEL:meta/llama-3.1-70b-instruct}}") String model,
            ObjectMapper objectMapper) {

        this.apiKey = resolveEnvValue("NVIDIA_NIM_API_KEY", configuredApiKey, "");
        this.baseUrl = resolveEnvValue("NVIDIA_NIM_BASE_URL", baseUrl, DEFAULT_BASE_URL);
        this.model = resolveEnvValue("NVIDIA_NIM_MODEL", model, DEFAULT_MODEL);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        if (isConfigured()) {
            log.info("NVIDIA NIM AI Service initialized with model: {} via endpoint: {}", this.model, this.baseUrl);
        } else {
            log.warn("NVIDIA NIM AI Service initialized WITHOUT API key. Fallbacks will be active until NVIDIA_NIM_API_KEY is provided in .env");
        }
    }

    private String resolveEnvValue(String envVarName, String propertyValue, String defaultValue) {
        if (propertyValue != null && !propertyValue.isBlank() && !propertyValue.startsWith("${")) {
            return propertyValue.trim();
        }
        // Check System Environment
        String envKey = System.getenv(envVarName);
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        // Check .env files in project root or current directory
        String[] potentialEnvPaths = {
            ".env",
            "../.env",
            System.getProperty("user.dir") + File.separator + ".env",
            System.getProperty("user.home") + File.separator + ".env"
        };
        for (String pathStr : potentialEnvPaths) {
            try {
                Path p = Paths.get(pathStr);
                if (Files.exists(p)) {
                    List<String> lines = Files.readAllLines(p);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith(envVarName + "=")) {
                            String val = trimmed.substring((envVarName + "=").length()).trim();
                            if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (val.startsWith("'") && val.endsWith("'") && val.length() > 1) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isBlank()) {
                                return val;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getActiveModel() {
        return model;
    }

    /**
     * Calls NVIDIA NIM chat completion with messages array.
     */
    public String callChatCompletion(List<Map<String, String>> messages, double temperature, int maxTokens) {
        if (!isConfigured()) {
            throw new IllegalStateException("NVIDIA NIM API key is not configured.");
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", this.model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("top_p", 1.0);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode messageNode = choices.get(0).path("message");
                    String raw = messageNode.path("content").asText("");
                    return cleanThinkingOutput(raw);
                }
                return "";
            } else {
                log.error("NVIDIA NIM API returned error status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("NVIDIA NIM API error status: " + response.statusCode() + " -> " + response.body());
            }
        } catch (Exception e) {
            log.error("Error executing NVIDIA NIM API call", e);
            throw new RuntimeException(e);
        }
    }

    private String cleanThinkingOutput(String content) {
        if (content == null) return "";
        String cleaned = content.trim();
        // Strip <think>...</think> tags if model includes them
        if (cleaned.contains("<think>") && cleaned.contains("</think>")) {
            int endIdx = cleaned.indexOf("</think>") + 8;
            cleaned = cleaned.substring(endIdx).trim();
        }
        // Strip "Here's a thinking process:" header if present
        if (cleaned.toLowerCase().startsWith("here's a thinking process:") || cleaned.toLowerCase().startsWith("thinking process:")) {
            int firstDoubleBreak = cleaned.indexOf("\n\n");
            if (firstDoubleBreak != -1 && firstDoubleBreak + 2 < cleaned.length()) {
                cleaned = cleaned.substring(firstDoubleBreak + 2).trim();
            }
        }
        return cleaned;
    }

    /**
     * Chat response tailored for LearnSphere academic assistant.
     */
    public String generateChatResponse(String userMessage, String roleContext) {
        String systemPrompt = """
            You are Spherie, the expert AI Academic Mentor and Copilot for LearnSphere, an online learning and college MOOC platform.
            Your Instructions:
            1. Provide direct, smart, structured, and insightful academic explanations, code solutions, or platform guidance.
            2. Do NOT output internal scratchpads, meta-thinking steps, or 'Here is a thinking process'. Start directly with your helpful answer.
            3. Help students, teachers, and admins with courses, attendance recovery, remedial credit fulfillment (>= 40% passing score), leaderboard points (+10 lecture, +25 test, +50 bonus), and lecture notes.
            4. Format key terms and code cleanly with markdown. Keep the tone intelligent, concise, and encouraging.
            """;

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt + (roleContext != null ? "\nContext: " + roleContext : ""));
        messages.add(sysMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        return callChatCompletion(messages, 0.3, 1024);
    }

    /**
     * Extracts assessment MCQs from lecture transcripts or notes.
     */
    public List<Map<String, Object>> extractAssessmentQuestions(String transcriptOrNotes, int count) {
        String prompt = String.format("""
            Analyze the following lecture transcript/study notes and generate exactly %d multiple-choice assessment questions.
            Return ONLY a valid JSON array of objects with the exact schema:
            [
              {
                "question": "The question text?",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctIndex": 0,
                "explanation": "Clear explanation of why this answer is correct."
              }
            ]

            Lecture Content:
            %s
            """, count, transcriptOrNotes);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "You are an expert exam creator. Respond only with valid JSON matching the requested schema without markdown backticks or extra commentary."),
            Map.of("role", "user", "content", prompt)
        );

        String rawResponse = callChatCompletion(messages, 0.2, 2048);
        try {
            // Clean markdown blocks if present
            String cleaned = rawResponse.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            List<Map<String, Object>> questions = objectMapper.readValue(cleaned, List.class);
            return questions;
        } catch (Exception e) {
            log.error("Failed to parse questions JSON from NVIDIA NIM output: {}", rawResponse, e);
            throw new RuntimeException("Could not parse AI generated questions: " + e.getMessage());
        }
    }

    /**
     * Generates multi-language lecture summary and formula/code extraction.
     */
    public Map<String, Object> summarizeTranscript(String transcript, String targetLanguage) {
        String prompt = String.format("""
            You are an AI study engine. Analyze the lecture transcript and generate a study guide in %s.
            Return ONLY a valid JSON object with the exact schema:
            {
              "keyTakeaways": ["Point 1", "Point 2", "Point 3"],
              "formulaAndCode": ["Important equation or code snippet 1", "Important equation or code snippet 2"],
              "summaryParagraph": "A 2-3 sentence overview."
            }

            Transcript:
            %s
            """, targetLanguage != null ? targetLanguage : "English", transcript);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "You are an expert AI study note extractor. Respond strictly with a JSON object matching the schema."),
            Map.of("role", "user", "content", prompt)
        );

        String rawResponse = callChatCompletion(messages, 0.2, 1024);
        try {
            String cleaned = rawResponse.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();

            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse transcript summary JSON: {}", rawResponse, e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("summaryParagraph", rawResponse);
            fallback.put("keyTakeaways", List.of("Core lecture concepts reviewed", "Key algorithmic properties analyzed", "Practical applications highlighted"));
            fallback.put("formulaAndCode", List.of());
            return fallback;
        }
    }
}
