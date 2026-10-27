package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;

import java.util.Set;

/**
 * Defines the frozen role vocabulary for LearnSphere AI.
 *
 * <p>Canonical runtime roles are {@code ADMIN}, {@code TEACHER}, and {@code STUDENT}.
 * Per requirement A-01, {@code INSTRUCTOR} is accepted as an alias at the API edge
 * and mapped to {@code TEACHER}. Arbitrary roles outside this vocabulary are rejected.</p>
 */
public final class RoleVocabulary {

    public static final String STUDENT = "STUDENT";
    public static final String TEACHER = "TEACHER";
    public static final String ADMIN = "ADMIN";
    public static final String INSTRUCTOR_ALIAS = "INSTRUCTOR";

    public static final Set<String> CANONICAL_ROLES = Set.of(STUDENT, TEACHER, ADMIN);

    private RoleVocabulary() {}

    /**
     * Maps an input role name (case-insensitive) to its canonical runtime name.
     * {@code INSTRUCTOR} is mapped to {@code TEACHER} (A-01).
     *
     * @param role input role name
     * @return canonical role name (STUDENT, TEACHER, or ADMIN)
     * @throws BadRequestException if the role is null, blank, or outside the frozen vocabulary
     */
    public static String canonicalize(String role) {
        if (role == null || role.isBlank()) {
            throw new BadRequestException("Role name is required");
        }
        String normalized = role.trim().toUpperCase();
        if (INSTRUCTOR_ALIAS.equals(normalized)) {
            return TEACHER;
        }
        if (CANONICAL_ROLES.contains(normalized)) {
            return normalized;
        }
        throw new BadRequestException("Unknown role: " + role + ". Allowed roles: ADMIN, TEACHER, STUDENT (or alias INSTRUCTOR)");
    }

    /**
     * Checks whether the given string is a valid canonical role or allowed alias.
     */
    public static boolean isValid(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return INSTRUCTOR_ALIAS.equals(normalized) || CANONICAL_ROLES.contains(normalized);
    }
}
