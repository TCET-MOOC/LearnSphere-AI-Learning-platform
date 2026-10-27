package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Controller.LiveChatWebSocketController;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LiveSessionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Security.JwtService;
import com.MOOC.OnlineLearningPlatfrom.Security.WebSocketAuthInterceptor;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * W1-09 integration test suite for WebSocket authorization.
 *
 * <p>Enforces:
 * - CONNECT frame validates JWT token and authenticates session.
 * - Invalid/expired token on CONNECT rejects with AccessDeniedException.
 * - SUBSCRIBE frame is authorized per destination:
 *     - /topic/admin/** requires ADMIN role
 *     - /topic/course/{courseId}/** requires course enrollment or staff/admin
 *     - /topic/live-sessions/{sessionId}/** requires course enrollment or staff/admin
 *     - /user/queue/** requires self, rejecting foreign user queues
 * - SEND/MessageMapping in LiveChatWebSocketController rejects unenrolled users
 *   and overrides client-supplied userId/sender with real authenticated principal.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WebSocketAuthorizationTests extends PostgresIntegrationTest {

    @Autowired
    private WebSocketAuthInterceptor interceptor;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LiveChatWebSocketController liveChatWebSocketController;

    private Long courseId;
    private Long liveSessionId;
    private String adminToken;
    private String studentToken;
    private String unenrolledToken;
    private CustomUserDetails studentPrincipal;
    private CustomUserDetails unenrolledPrincipal;
    private CustomUserDetails adminPrincipal;

    @BeforeEach
    void setUpFixtures() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");
        UserAccount admin = userAccountRepository.findByEmail("admin@learnsphere.com");
        UserAccount external = userAccountRepository.findByEmail("external@learnsphere.com");

        Course course = courseRepository.findByTeacher_UserId(teacher.getUserId()).stream().findFirst().orElseThrow();
        courseId = course.getId();

        LiveSession session = new LiveSession();
        session.setCourse(course);
        session.setTeacher(teacher);
        session.setTitle("Live Class on Graphs");
        session.setScheduledAt(LocalDateTime.now().plusHours(2));
        session.setStatus(LiveSession.Status.LIVE);
        session = liveSessionRepository.save(session);
        liveSessionId = session.getId();

        adminPrincipal = (CustomUserDetails) userDetailsService.loadUserByUsername("admin@learnsphere.com");
        studentPrincipal = (CustomUserDetails) userDetailsService.loadUserByUsername("student@learnsphere.com");
        unenrolledPrincipal = (CustomUserDetails) userDetailsService.loadUserByUsername("external@learnsphere.com");

        adminToken = jwtService.generateToken("admin@learnsphere.com", admin.getUserId(), List.of("ADMIN"));
        studentToken = jwtService.generateToken("student@learnsphere.com", student.getUserId(), List.of("STUDENT"));
        unenrolledToken = jwtService.generateToken("external@learnsphere.com", external.getUserId(), List.of("STUDENT"));
    }

    private Message<?> createConnectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (token != null) {
            accessor.setNativeHeader("Authorization", "Bearer " + token);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> createSubscribeMessage(String destination, CustomUserDetails user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId("sub-1");
        if (user != null) {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            accessor.setUser(auth);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connect_withValidToken_authenticatesUser() {
        Message<?> message = createConnectMessage(studentToken);
        Message<?> result = interceptor.preSend(message, (MessageChannel) null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("student@learnsphere.com");
    }

    @Test
    void connect_withInvalidToken_throwsAccessDeniedException() {
        Message<?> message = createConnectMessage("invalid.bogus.jwt.token");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, (MessageChannel) null));
    }

    @Test
    void subscribe_anonymous_throwsAccessDeniedException() {
        Message<?> message = createSubscribeMessage("/topic/course/" + courseId, null);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, (MessageChannel) null));
    }

    @Test
    void subscribe_adminTopic_byStudent_throwsAccessDeniedException() {
        Message<?> message = createSubscribeMessage("/topic/admin/alerts", studentPrincipal);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, (MessageChannel) null));
    }

    @Test
    void subscribe_adminTopic_byAdmin_succeeds() {
        Message<?> message = createSubscribeMessage("/topic/admin/alerts", adminPrincipal);
        Message<?> result = interceptor.preSend(message, (MessageChannel) null);
        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_courseTopic_byEnrolledStudent_succeeds() {
        Message<?> message = createSubscribeMessage("/topic/course/" + courseId, studentPrincipal);
        Message<?> result = interceptor.preSend(message, (MessageChannel) null);
        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_courseTopic_byUnenrolledStudent_throwsAccessDeniedException() {
        Message<?> message = createSubscribeMessage("/topic/course/" + courseId, unenrolledPrincipal);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, (MessageChannel) null));
    }

    @Test
    void subscribe_liveSessionTopic_byEnrolledStudent_succeeds() {
        Message<?> message = createSubscribeMessage("/topic/live-sessions/" + liveSessionId + "/chat", studentPrincipal);
        Message<?> result = interceptor.preSend(message, (MessageChannel) null);
        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_liveSessionTopic_byUnenrolledStudent_throwsAccessDeniedException() {
        Message<?> message = createSubscribeMessage("/topic/live-sessions/" + liveSessionId + "/chat", unenrolledPrincipal);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, (MessageChannel) null));
    }

    @Test
    void subscribe_userQueue_ownQueue_succeeds() {
        Message<?> message = createSubscribeMessage("/user/queue/notifications", studentPrincipal);
        Message<?> result = interceptor.preSend(message, (MessageChannel) null);
        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_foreignUserQueue_throwsAccessDeniedException() {
        Message<?> message = createSubscribeMessage("/user/admin@learnsphere.com/queue/notifications", studentPrincipal);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, (MessageChannel) null));
    }

    @Test
    void liveChatController_sendLiveChatMessage_overridesUserIdAndSenderWithPrincipal() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 999999L);
        payload.put("sender", "MaliciousAttacker");
        payload.put("message", "Hello live class!");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(studentPrincipal, null, studentPrincipal.getAuthorities());

        liveChatWebSocketController.sendLiveChatMessage(liveSessionId, payload, auth);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/live-sessions/" + liveSessionId + "/chat"), captor.capture());

        Map<String, Object> broadcasted = captor.getValue();
        assertThat(broadcasted.get("userId")).isEqualTo(studentPrincipal.getUser().getUserId());
        assertThat(broadcasted.get("sender")).isEqualTo(studentPrincipal.getUser().getFullName());
        assertThat(broadcasted.get("message")).isEqualTo("Hello live class!");
        assertThat(broadcasted.get("timestamp")).isNotNull();
    }

    @Test
    void liveChatController_sendLiveChatMessage_byUnenrolledStudent_throwsAccessDeniedException() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Unenrolled injection");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(unenrolledPrincipal, null, unenrolledPrincipal.getAuthorities());

        assertThrows(AccessDeniedException.class, () ->
                liveChatWebSocketController.sendLiveChatMessage(liveSessionId, payload, auth));
    }
}
