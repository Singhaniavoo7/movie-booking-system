package com.moviebooking.exception;

/** Thrown for state-transition violations, e.g. cancelling an already-cancelled booking. */
public class IllegalBookingStateException extends RuntimeException {
    public IllegalBookingStateException(String message) {
        super(message);
    }
}
