package com.moviebooking.exception;

/** Thrown when a seat hold referenced by a booking attempt is missing, expired, or owned by someone else. */
public class InvalidHoldException extends RuntimeException {
    public InvalidHoldException(String message) {
        super(message);
    }
}
