package com.MOOC.OnlineLearningPlatfrom.Security;

import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import com.MOOC.OnlineLearningPlatfrom.Repository.LiveSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket authorization interceptor (W1-09).
 *
 * <p>Validates STOMP CONNECT authentication and authorizes STOMP SUBSCRIBE
 * per destination, rejecting foreign user queues and unauthorized courses/sessions.</p>
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private static final Pattern COURSE_TOPIC_PATTERN = Pattern.compile("^/topic/course/(\\d+)(/.*)?$");
    private static final Pattern SESSION_TOPIC_PATTERN = Pattern.compile("^/topic/live-sessions/(\\d+)(/.*)?$");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final OwnershipService ownershipService;
    private final LiveSessionRepository liveSessionRepository;

    public WebSocketAuthInterceptor(JwtService jwtService,
                                    CustomUserDetailsService userDetailsService,
                                    OwnershipService ownershipService,
                                    LiveSessionRepository liveSessionRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.ownershipService = ownershipService;
        this.liveSessionRepository = liveSessionRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            return handleConnect(message, accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        String token = null;

        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                token = header.substring(7);
            } else {
                token = header;
            }
        }

        if (token == null) {
            List<String> tokenHeaders = accessor.getNativeHeader("token");
            if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
                token = tokenHeaders.get(0);
            }
        }

        if (token != null) {
            try {
                String email = jwtService.extractEmail(token);
                if (email != null && jwtService.isTokenValid(token, email)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (!userDetails.isAccountNonLocked() || !userDetails.isEnabled()) {
                        throw new AccessDeniedException("Account is inactive or blacklisted");
                    }
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    if (accessor.isMutable()) {
                        accessor.setUser(authentication);
                        return message;
                    } else {
                        StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
                        mutableAccessor.setUser(authentication);
                        return MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
                    }
                } else {
                    throw new AccessDeniedException("Invalid or expired WebSocket token");
                }
            } catch (AccessDeniedException ade) {
                throw ade;
            } catch (Exception e) {
                log.warn("WebSocket JWT authentication failed: {}", e.getMessage());
                throw new AccessDeniedException("Invalid or expired WebSocket token");
            }
        }
        return message;
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Destination required for SUBSCRIBE");
        }

        Object userObj = accessor.getUser();
        if (!(userObj instanceof Authentication auth) || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required to subscribe to " + destination);
        }

        CustomUserDetails userDetails = null;
        if (auth.getPrincipal() instanceof CustomUserDetails cud) {
            userDetails = cud;
        }

        if (userDetails == null) {
            throw new AccessDeniedException("Valid user details required to subscribe to " + destination);
        }

        // 1. /topic/admin/**
        if (destination.startsWith("/topic/admin")) {
            try {
                ownershipService.requireAdmin(userDetails);
            } catch (Exception e) {
                throw new AccessDeniedException("Admin access required for " + destination);
            }
            return;
        }

        // 2. /topic/course/{courseId}/**
        Matcher courseMatcher = COURSE_TOPIC_PATTERN.matcher(destination);
        if (courseMatcher.matches()) {
            Long courseId = Long.parseLong(courseMatcher.group(1));
            try {
                ownershipService.requireEnrolled(courseId, userDetails);
            } catch (Exception e) {
                throw new AccessDeniedException("Enrolment required for course topic " + destination);
            }
            return;
        }

        // 3. /topic/live-sessions/{sessionId}/**
        Matcher sessionMatcher = SESSION_TOPIC_PATTERN.matcher(destination);
        if (sessionMatcher.matches()) {
            Long sessionId = Long.parseLong(sessionMatcher.group(1));
            LiveSession liveSession = liveSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new AccessDeniedException("Live session not found: " + sessionId));
            if (liveSession.getCourse() != null) {
                try {
                    ownershipService.requireEnrolled(liveSession.getCourse().getId(), userDetails);
                } catch (Exception e) {
                    throw new AccessDeniedException("Enrolment required for session topic " + destination);
                }
            }
            return;
        }

        // 4. User destination: /user/...
        if (destination.startsWith("/user/")) {
            if (destination.startsWith("/user/queue/")) {
                return;
            }
            String pathAfterUser = destination.substring("/user/".length());
            int nextSlash = pathAfterUser.indexOf('/');
            if (nextSlash > 0) {
                String targetedUser = pathAfterUser.substring(0, nextSlash);
                boolean isMatch = targetedUser.equalsIgnoreCase(userDetails.getUsername())
                        || (userDetails.getUser() != null && targetedUser.equals(String.valueOf(userDetails.getUser().getUserId())));
                if (!isMatch) {
                    throw new AccessDeniedException("Cannot subscribe to foreign user destination: " + destination);
                }
            }
        }
    }
}
