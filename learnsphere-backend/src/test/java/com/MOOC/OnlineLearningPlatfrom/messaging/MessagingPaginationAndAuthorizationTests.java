package com.MOOC.OnlineLearningPlatfrom.messaging;

import com.MOOC.OnlineLearningPlatfrom.Dto.SendMessageRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StartConversationRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W5-08: Messaging pagination, participant authorization, report/block, retention note
 * Rules: [E-F17], spec §4 SCR-23, NFR-03
 *
 * Scenarios:
 * 1. Participant can get messages with pagination and Cache-Control: private, no-store.
 * 2. Pagination query parameters (page=0, size=2) slice messages and return pagination headers (X-Total-Count, etc.).
 * 3. Non-participant accessing messages returns 404 NOT_FOUND (IDOR defense).
 * 4. Non-participant sending a message returns 404 NOT_FOUND (IDOR defense).
 * 5. Admin can access messages in any conversation -> 200 OK.
 * 6. Unauthenticated requests return 401 UNAUTHORIZED.
 * 7. Participant can block conversation -> 200 OK with isBlocked=true.
 * 8. Sending message in a blocked conversation returns 400 BAD_REQUEST.
 * 9. Blocker participant can unblock; non-blocker participant cannot unblock (400 BAD_REQUEST).
 * 10. Participant can report conversation/message to moderation -> 200 OK and creates flag in flagged_content.
 * 11. Non-participant cannot block or report conversation -> 404 NOT_FOUND.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MessagingPaginationAndAuthorizationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FlaggedContentRepository flaggedContentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount userA;
    private UserAccount userB;
    private UserAccount outsider;
    private UserAccount admin;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        flaggedContentRepository.deleteAll();

        userA = findOrCreateUser("msg.alice@learnsphere.com", "Alice Smith", "STUDENT");
        userB = findOrCreateUser("msg.bob@learnsphere.com", "Bob Jones", "STUDENT");
        outsider = findOrCreateUser("msg.charlie@learnsphere.com", "Charlie Brown", "STUDENT");
        admin = findOrCreateUser("msg.admin@learnsphere.com", "Message Admin", "ADMIN");

        conversation = new Conversation();
        conversation.setParticipantA(userA);
        conversation.setParticipantB(userB);
        conversation = conversationRepository.save(conversation);

        // Prepopulate 5 messages
        for (int i = 1; i <= 5; i++) {
            Message m = new Message();
            m.setConversation(conversation);
            m.setSender(i % 2 == 1 ? userA : userB);
            m.setText("Message " + i);
            messageRepository.save(m);
        }
    }

    private RequestPostProcessor asUser(UserAccount account) {
        var userDetails = userDetailsService.loadUserByUsername(account.getEmail());
        return user(userDetails);
    }

    private UserAccount findOrCreateUser(String email, String name, String roleName) {
        UserAccount existing = userAccountRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(name);
        u.setPassword("hashedpassword");
        u.setStatus(UserAccount.Status.ACTIVE);
        u = userAccountRepository.save(u);

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
        }

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        userRoleRepository.save(ur);
        return u;
    }

    @Test
    @DisplayName("Scenario 1: Participant can get messages with pagination headers and Cache-Control: private, no-store")
    void participantCanGetMessagesWithPaginationHeaders() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId())
                        .with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("Scenario 2: Pagination query parameters slice messages and return accurate page headers")
    void paginationParametersSliceMessages() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/messages?page=0&size=2", conversation.getId())
                        .with(asUser(userB)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(header().string("X-Page-Number", "0"))
                .andExpect(header().string("X-Page-Size", "2"))
                .andExpect(header().string("X-Total-Pages", "3"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].text").value("Message 1"))
                .andExpect(jsonPath("$[1].text").value("Message 2"));

        mockMvc.perform(get("/api/conversations/{id}/messages?page=1&size=2", conversation.getId())
                        .with(asUser(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].text").value("Message 3"))
                .andExpect(jsonPath("$[1].text").value("Message 4"));
    }

    @Test
    @DisplayName("Scenario 3: Non-participant accessing messages returns 404 NOT_FOUND (IDOR defense)")
    void outsiderAccessingMessagesReturns404() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId())
                        .with(asUser(outsider)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 4: Non-participant sending message returns 404 NOT_FOUND (IDOR defense)")
    void outsiderSendingMessageReturns404() throws Exception {
        SendMessageRequestDto dto = new SendMessageRequestDto();
        dto.setText("Unauthorized ping");

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .with(asUser(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 5: Admin can access conversation messages -> 200 OK")
    void adminCanAccessConversationMessages() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId())
                        .with(asUser(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("Scenario 6: Unauthenticated requests return 401 UNAUTHORIZED")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 7: Participant can block conversation -> 200 OK with isBlocked=true")
    void participantCanBlockConversation() throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/block", conversation.getId())
                        .with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));
    }

    @Test
    @DisplayName("Scenario 8: Sending message in a blocked conversation returns 400 BAD_REQUEST")
    void sendingMessageInBlockedConversationReturns400() throws Exception {
        // userA blocks conversation
        mockMvc.perform(post("/api/conversations/{id}/block", conversation.getId())
                        .with(asUser(userA)))
                .andExpect(status().isOk());

        SendMessageRequestDto dto = new SendMessageRequestDto();
        dto.setText("Hello?");

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .with(asUser(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("blocked")));
    }

    @Test
    @DisplayName("Scenario 9: Only blocker participant can unblock; non-blocker participant cannot unblock")
    void onlyBlockerCanUnblock() throws Exception {
        // userA blocks
        mockMvc.perform(post("/api/conversations/{id}/block", conversation.getId())
                        .with(asUser(userA)))
                .andExpect(status().isOk());

        // userB attempts to unblock -> 400 BAD_REQUEST
        mockMvc.perform(post("/api/conversations/{id}/unblock", conversation.getId())
                        .with(asUser(userB)))
                .andExpect(status().isBadRequest());

        // userA unblocks -> 200 OK
        mockMvc.perform(post("/api/conversations/{id}/unblock", conversation.getId())
                        .with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    @DisplayName("Scenario 10: Participant can report conversation to moderation -> creates flag in flagged_content")
    void participantCanReportConversation() throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/report", conversation.getId())
                        .with(asUser(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "BULLYING"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        List<FlaggedContent> flags = flaggedContentRepository.findAll();
        assertThat(flags).isNotEmpty();
        FlaggedContent flag = flags.get(flags.size() - 1);
        assertThat(flag.getContentType()).isEqualTo(FlaggedContent.ContentType.MESSAGE);
        assertThat(flag.getReporterId()).isEqualTo(userA.getUserId());
        assertThat(flag.getReason()).isEqualTo(FlaggedContent.Reason.BULLYING);
    }

    @Test
    @DisplayName("Scenario 11: Non-participant cannot block or report conversation -> 404 NOT_FOUND")
    void nonParticipantCannotBlockOrReport() throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/block", conversation.getId())
                        .with(asUser(outsider)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/conversations/{id}/report", conversation.getId())
                        .with(asUser(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "SPAM"))))
                .andExpect(status().isNotFound());
    }
}
