package com.moviebooking.dto.response;

import com.moviebooking.entity.Booking;
import com.moviebooking.entity.BookingSeat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long showId;
    private String movieTitle;
    private String theaterName;
    private String status;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private List<String> seats;
    private java.time.Instant createdAt;

    public static BookingResponse from(Booking b) {
        List<String> seatLabels = b.getBookingSeats().stream()
                .map(BookingResponse::seatLabel)
                .toList();
        return BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .showId(b.getShow().getId())
                .movieTitle(b.getShow().getMovie().getTitle())
                .theaterName(b.getShow().getScreen().getTheater().getName())
                .status(b.getStatus().name())
                .subtotalAmount(b.getSubtotalAmount())
                .discountAmount(b.getDiscountAmount())
                .finalAmount(b.getFinalAmount())
                .seats(seatLabels)
                .createdAt(b.getCreatedAt())
                .build();
    }

    private static String seatLabel(BookingSeat bs) {
        return bs.getShowSeat().getSeatTemplate().getRowLabel() + bs.getShowSeat().getSeatTemplate().getSeatNumber();
    }
}
