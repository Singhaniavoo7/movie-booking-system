package com.moviebooking.exception;

/** Thrown when an authenticated user attempts an action they don't own/aren't permitted to do. */
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
