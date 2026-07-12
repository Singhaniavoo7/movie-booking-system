package com.moviebooking.controller;

import com.moviebooking.dto.request.DiscountCodeRequest;
import com.moviebooking.dto.request.RefundPolicyRequest;
import com.moviebooking.dto.response.DiscountCodeResponse;
import com.moviebooking.dto.response.RefundPolicyResponse;
import com.moviebooking.service.DiscountCodeService;
import com.moviebooking.service.RefundPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only pricing/policy management: discount codes and refund policies. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final DiscountCodeService discountCodeService;
    private final RefundPolicyService refundPolicyService;

    @PostMapping("/discount-codes")
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(@Valid @RequestBody DiscountCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DiscountCodeResponse.from(discountCodeService.create(request)));
    }

    @GetMapping("/discount-codes")
    public List<DiscountCodeResponse> listDiscountCodes() {
        return discountCodeService.listAll().stream().map(DiscountCodeResponse::from).toList();
    }

    @DeleteMapping("/discount-codes/{id}")
    public ResponseEntity<Void> deactivateDiscountCode(@PathVariable Long id) {
        discountCodeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refund-policies")
    public ResponseEntity<RefundPolicyResponse> createRefundPolicy(@Valid @RequestBody RefundPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RefundPolicyResponse.from(refundPolicyService.create(request)));
    }

    @GetMapping("/refund-policies")
    public List<RefundPolicyResponse> listRefundPolicies() {
        return refundPolicyService.listAll().stream().map(RefundPolicyResponse::from).toList();
    }
}
