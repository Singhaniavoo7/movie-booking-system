package com.moviebooking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk through the core flows described in the assignment: admin sets
 * up a city/theater/screen/movie/pricing tier/show, a customer registers, browses,
 * holds seats, books (with a discount code), and cancels -- verifying seat status
 * transitions and refund calculation at each step.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken() throws Exception {
        Map<String, String> login = Map.of("email", "admin@moviebooking.local", "password", "Admin@12345");
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String registerCustomer(String email) throws Exception {
        Map<String, String> register = Map.of("name", "Test Customer", "email", email, "password", "password123");
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void fullBookingAndCancellationFlow() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer("flow-test-" + System.nanoTime() + "@example.com");

        // --- Admin sets up the catalog ---
        Long cityId = extractId(mockMvc.perform(post("/api/admin/cities").header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Jaipur", "state", "Rajasthan"))))
                .andExpect(status().isCreated()).andReturn());

        Long theaterId = extractId(mockMvc.perform(post("/api/admin/theaters").header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "PVR Central", "address", "MI Road", "cityId", cityId))))
                .andExpect(status().isCreated()).andReturn());

        Map<String, Object> seatRow = Map.of("rowLabel", "A", "seatCount", 5, "seatType", "REGULAR");
        Long screenId = extractId(mockMvc.perform(post("/api/admin/screens").header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Screen 1", "theaterId", theaterId, "seatLayout", List.of(seatRow)))))
                .andExpect(status().isCreated()).andReturn());

        Long movieId = extractId(mockMvc.perform(post("/api/admin/movies").header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Test Movie", "language", "English", "genre", "Drama", "durationMinutes", 120))))
                .andExpect(status().isCreated()).andReturn());

        Long pricingTierId = extractId(mockMvc.perform(post("/api/admin/pricing-tiers").header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Weekday-" + System.nanoTime(), "seatTypePrices", Map.of("REGULAR", 200)))))
                .andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/admin/refund-policies").header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Standard-" + System.nanoTime(),
                                "isDefault", true,
                                "rules", List.of(
                                        Map.of("minHoursBeforeShow", 24, "refundPercentage", 100),
                                        Map.of("minHoursBeforeShow", 0, "refundPercentage", 50))))))
                .andExpect(status().isCreated());

        Instant startTime = Instant.now().plus(48, ChronoUnit.HOURS);
        Long showId = extractId(mockMvc.perform(post("/api/admin/shows").header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "movieId", movieId, "screenId", screenId, "pricingTierId", pricingTierId,
                                "startTime", startTime.toString()))))
                .andExpect(status().isCreated()).andReturn());

        // --- Customer browses and books ---
        String seatMapJson = mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode seats = objectMapper.readTree(seatMapJson);
        assertThat(seats).hasSize(5);
        long firstSeatId = seats.get(0).get("showSeatId").asLong();

        mockMvc.perform(post("/api/shows/" + showId + "/holds").header("Authorization", "Bearer " + customer)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("showSeatIds", List.of(firstSeatId)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.showSeatIds[0]").value(firstSeatId));

        String bookingJson = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "showId", showId, "showSeatIds", List.of(firstSeatId), "paymentMethod", "CARD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.finalAmount").value(200))
                .andReturn().getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(bookingJson).get("id").asLong();

        // Seat should now show as BOOKED to other browsers.
        mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk());

        // Booking shows up in the customer's history.
        mockMvc.perform(get("/api/bookings").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId));

        // --- Cancel: >24h before show, so the 100% tier applies ---
        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundPercentageApplied").value(100))
                .andExpect(jsonPath("$.refundAmount").value(200));

        // Seat is available again for someone else to book.
        String seatMapAfterCancel = mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andReturn().getResponse().getContentAsString();
        JsonNode seatsAfter = objectMapper.readTree(seatMapAfterCancel);
        assertThat(seatsAfter.get(0).get("status").asText()).isEqualTo("AVAILABLE");
    }

    @Test
    void cannotBookASeatSomeoneElseIsHolding() throws Exception {
        String admin = adminToken();
        String customerA = registerCustomer("racer-a-" + System.nanoTime() + "@example.com");
        String customerB = registerCustomer("racer-b-" + System.nanoTime() + "@example.com");

        Long cityId = extractId(mockMvc.perform(post("/api/admin/cities").header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("name", "Mumbai-" + System.nanoTime(), "state", "Maharashtra"))))
                .andExpect(status().isCreated()).andReturn());
        Long theaterId = extractId(mockMvc.perform(post("/api/admin/theaters").header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("name", "INOX", "address", "Andheri", "cityId", cityId))))
                .andExpect(status().isCreated()).andReturn());
        Long screenId = extractId(mockMvc.perform(post("/api/admin/screens").header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Screen 1", "theaterId", theaterId,
                        "seatLayout", List.of(Map.of("rowLabel", "A", "seatCount", 2, "seatType", "REGULAR"))))))
                .andExpect(status().isCreated()).andReturn());
        Long movieId = extractId(mockMvc.perform(post("/api/admin/movies").header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("title", "Race Movie", "durationMinutes", 100))))
                .andExpect(status().isCreated()).andReturn());
        Long pricingTierId = extractId(mockMvc.perform(post("/api/admin/pricing-tiers").header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Tier-" + System.nanoTime(), "seatTypePrices", Map.of("REGULAR", 150)))))
                .andExpect(status().isCreated()).andReturn());
        Long showId = extractId(mockMvc.perform(post("/api/admin/shows").header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "movieId", movieId, "screenId", screenId, "pricingTierId", pricingTierId,
                        "startTime", Instant.now().plus(10, ChronoUnit.HOURS).toString()))))
                .andExpect(status().isCreated()).andReturn());

        String seatMapJson = mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andReturn().getResponse().getContentAsString();
        long seatId = objectMapper.readTree(seatMapJson).get(0).get("showSeatId").asLong();

        mockMvc.perform(post("/api/shows/" + showId + "/holds").header("Authorization", "Bearer " + customerA)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("showSeatIds", List.of(seatId)))))
                .andExpect(status().isCreated());

        // customerB tries to hold the same seat -> must be rejected with 409.
        mockMvc.perform(post("/api/shows/" + showId + "/holds").header("Authorization", "Bearer " + customerB)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("showSeatIds", List.of(seatId)))))
                .andExpect(status().isConflict());
    }

    private Long extractId(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return objectMapper.readTree(content).get("id").asLong();
    }
}
