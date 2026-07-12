package com.moviebooking.dto.response;

import com.moviebooking.entity.DiscountCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class DiscountCodeResponse {
    private Long id;
    private String code;
    private String type;
    private BigDecimal value;
    private Instant validFrom;
    private Instant validTo;
    private Integer maxUses;
    private int usedCount;
    private boolean active;

    public static DiscountCodeResponse from(DiscountCode d) {
        return DiscountCodeResponse.builder()
                .id(d.getId()).code(d.getCode()).type(d.getType().name()).value(d.getValue())
                .validFrom(d.getValidFrom()).validTo(d.getValidTo()).maxUses(d.getMaxUses())
                .usedCount(d.getUsedCount()).active(d.isActive())
                .build();
    }
}
