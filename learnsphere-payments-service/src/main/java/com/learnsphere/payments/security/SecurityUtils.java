package com.learnsphere.payments.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static PaymentUserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PaymentUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static Long getCurrentUserId() {
        PaymentUserPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.getUserId() : null;
    }
}
