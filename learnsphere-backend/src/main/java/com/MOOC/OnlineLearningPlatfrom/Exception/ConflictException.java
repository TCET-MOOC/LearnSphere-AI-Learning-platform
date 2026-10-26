package com.MOOC.OnlineLearningPlatfrom.Exception;

/**
 * A request conflicts with the current state of the resource, such as
 * assigning a role the user already holds. Mapped to {@code 409 CONFLICT}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
