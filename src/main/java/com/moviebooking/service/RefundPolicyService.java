package com.moviebooking.service;

import com.moviebooking.dto.request.RefundPolicyRequest;
import com.moviebooking.dto.request.RefundRuleRequest;
import com.moviebooking.entity.RefundPolicy;
import com.moviebooking.entity.RefundRule;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.RefundPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;

    @Transactional
    public RefundPolicy create(RefundPolicyRequest req) {
        List<RefundRule> rules = req.getRules().stream()
                .map(this::toRule)
                .toList();

        if (req.isDefault()) {
            // Only one default policy at a time -- unset any previous default.
            refundPolicyRepository.findByIsDefaultTrueAndActiveTrue()
                    .ifPresent(existing -> existing.setDefault(false));
        }

        RefundPolicy policy = RefundPolicy.builder()
                .name(req.getName())
                .rules(rules)
                .isDefault(req.isDefault())
                .active(true)
                .build();
        return refundPolicyRepository.save(policy);
    }

    public List<RefundPolicy> listAll() {
        return refundPolicyRepository.findAll();
    }

    public RefundPolicy get(Long id) {
        return refundPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund policy not found: " + id));
    }

    public Optional<RefundPolicy> getDefault() {
        return refundPolicyRepository.findByIsDefaultTrueAndActiveTrue();
    }

    private RefundRule toRule(RefundRuleRequest r) {
        return RefundRule.builder()
                .minHoursBeforeShow(r.getMinHoursBeforeShow())
                .refundPercentage(r.getRefundPercentage())
                .build();
    }
}
