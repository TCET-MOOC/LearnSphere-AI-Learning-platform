package com.MOOC.OnlineLearningPlatfrom.Security;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FlaggedUserInterceptor implements HandlerInterceptor {

    private final UserAccountRepository userAccountRepository;

    public FlaggedUserInterceptor(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            String uri = request.getRequestURI();
            if (uri.endsWith("/auth/login") || uri.endsWith("/auth/register")
                    || uri.endsWith("/auth/refresh") || uri.endsWith("/auth/logout")) {
                return true;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                if (email != null) {
                    UserAccount user = userAccountRepository.findByEmail(email);
                    if (user != null && user.getStatus() == UserAccount.Status.FLAGGED) {
                        throw new AccessDeniedException("Account is flagged and read-only");
                    }
                }
            }
        }
        return true;
    }
}
