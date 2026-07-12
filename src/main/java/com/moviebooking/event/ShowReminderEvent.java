package com.moviebooking.event;

/** Published by a scheduled job for shows starting soon; triggers reminder notifications. */
public record ShowReminderEvent(Long bookingId) {
}
