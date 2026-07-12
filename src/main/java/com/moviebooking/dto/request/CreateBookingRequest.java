package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateBookingRequest {
    @NotNull
    private Long showId;

    @NotEmpty
    private List<Long> showSeatIds;

    /** Optional discount code to apply. */
    private String discountCode;

    /** Mock payment method label, e.g. "CARD", "UPI". */
    @NotNull
    private String paymentMethod;

    /**
     * Test/demo hook only: when true, the mock payment gateway simulates a decline
     * so the "payment failed -> seats released" path can be exercised deterministically.
     */
    private boolean simulatePaymentFailure = false;
}
