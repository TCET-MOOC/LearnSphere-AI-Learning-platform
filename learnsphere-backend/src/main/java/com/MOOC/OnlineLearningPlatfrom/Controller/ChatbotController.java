package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Service.NvidiaNimService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    private final NvidiaNimService nvidiaNimService;

    public record ChatQuery(String message) {}
    public record ChatResponse(String reply, List<String> suggestions, boolean aiPowered, String model) {}

    private static final Map<String, String> KNOWLEDGE_BASE = new LinkedHashMap<>();

    static {
        KNOWLEDGE_BASE.put("attendance", "In LearnSphere, if your college attendance or credit falls below the required threshold, your department faculty flags you for Remedial Certification. Passing the course's remedial assessment with 40%+ restores your attendance credit automatically.");
        KNOWLEDGE_BASE.put("remedial", "Remedial courses allow students with attendance shortfall or supplementary needs to recover credits. Attempt the remedial test in the Assessments tab to unlock your fulfillment certificate.");
        KNOWLEDGE_BASE.put("certificate", "Certificates are automatically awarded once you complete 100% of a course's lecture modules and pass the final assessment. You can download official PDF diplomas and add them to LinkedIn from the Certificates tab.");
        KNOWLEDGE_BASE.put("live", "Live Sessions and masterclasses are hosted by college faculty. Check the 'Live Sessions' tab on your sidebar to view upcoming schedules and join active broadcasts in real-time.");
        KNOWLEDGE_BASE.put("payment", "Students from the same college access all internal courses for free! External students from other institutions can purchase individual courses with instant activation.");
        KNOWLEDGE_BASE.put("points", "You earn platform points by completing lectures (+10 pts), taking assessments (+25 pts), scoring above 90% (+50 bonus pts), and posting helpful discussion replies (+5 pts). Check your rank on the Leaderboard!");
        KNOWLEDGE_BASE.put("notes", "You can take timestamped notes while watching lectures. Use the 'Export All as PDF' or 'Copy to Clipboard' buttons in the Notes tab to export your study materials.");
        KNOWLEDGE_BASE.put("royalty", "Teachers earn royalties (80% split) on external student purchases and institutional course utilization. Earnings are disbursed monthly once your balance crosses ₹500.");
    }

    public ChatbotController(NvidiaNimService nvidiaNimService) {
        this.nvidiaNimService = nvidiaNimService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatQuery query) {
        String msg = query != null && query.message() != null ? query.message().trim() : "";
        if (msg.isEmpty()) {
            return ResponseEntity.ok(new ChatResponse("Hello! How can I assist you with LearnSphere today?", defaultSuggestions(), false, ""));
        }

        // 1. Try NVIDIA NIM LLM if configured
        if (nvidiaNimService.isConfigured()) {
            try {
                String aiReply = nvidiaNimService.generateChatResponse(msg, "LearnSphere MOOC & Engineering Platform");
                if (aiReply != null && !aiReply.isBlank()) {
                    return ResponseEntity.ok(new ChatResponse(aiReply, defaultSuggestions(), true, nvidiaNimService.getActiveModel()));
                }
            } catch (Exception e) {
                log.warn("NVIDIA NIM call failed, falling back to local academic knowledge base: {}", e.getMessage());
            }
        }

        // 2. Fallback to LearnSphere Academic Knowledge Base
        String lower = msg.toLowerCase();
        String answer = null;

        for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
            if (lower.contains(entry.getKey())) {
                answer = entry.getValue();
                break;
            }
        }

        if (answer == null) {
            if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
                answer = "Hello! I'm Spherie, your LearnSphere AI Academic Assistant. How can I help you with your courses, attendance recovery, assessments, or certificates today?";
            } else if (lower.contains("help") || lower.contains("who are you")) {
                answer = "I'm LearnSphere's AI Assistant! I can help guide you through course navigation, certificate generation, attendance recovery rules, live lectures, and point earning rules.";
            } else {
                answer = "Here is what I found regarding your query: You can explore all engineering and computing curriculum modules across departments, attend live masterclasses, or reach out to your course instructor via 1-on-1 Messages.";
            }
        }

        return ResponseEntity.ok(new ChatResponse(answer, defaultSuggestions(), false, "knowledge-base"));
    }

    private List<String> defaultSuggestions() {
        return List.of(
            "How does attendance recovery work?",
            "How do I earn certificates?",
            "How to earn points for leaderboard?",
            "Where do I join live lectures?"
        );
    }
}
