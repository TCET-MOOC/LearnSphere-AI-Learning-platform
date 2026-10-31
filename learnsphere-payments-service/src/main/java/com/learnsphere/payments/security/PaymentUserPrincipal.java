package com.learnsphere.payments.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;
import java.util.List;

@Getter
@AllArgsConstructor
public class PaymentUserPrincipal implements Principal {
    private final Long userId;
    private final String email;
    private final List<String> roles;

    @Override
    public String getName() {
        return email != null ? email : String.valueOf(userId);
    }
}
