package com.moviebooking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RefundPolicyRequest {
    @NotBlank
    private String name;

    @NotEmpty
    @Valid
    private List<RefundRuleRequest> rules;

    private boolean isDefault;
}
