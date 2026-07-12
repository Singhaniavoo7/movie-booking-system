package com.moviebooking.exception;

/** Thrown when one or more requested seats are not AVAILABLE (already held/booked by someone). */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
