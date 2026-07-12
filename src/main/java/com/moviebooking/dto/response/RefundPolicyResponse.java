package com.moviebooking.dto.response;

import com.moviebooking.entity.RefundPolicy;
import com.moviebooking.entity.RefundRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RefundPolicyResponse {
    private Long id;
    private String name;
    private List<RefundRule> rules;
    private boolean isDefault;

    public static RefundPolicyResponse from(RefundPolicy p) {
        return RefundPolicyResponse.builder()
                .id(p.getId()).name(p.getName()).rules(p.getRules()).isDefault(p.isDefault())
                .build();
    }
}
