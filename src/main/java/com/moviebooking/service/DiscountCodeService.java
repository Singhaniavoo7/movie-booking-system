package com.moviebooking.service;

import com.moviebooking.dto.request.DiscountCodeRequest;
import com.moviebooking.entity.DiscountCode;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;

    @Transactional
    public DiscountCode create(DiscountCodeRequest req) {
        if (discountCodeRepository.findByCodeIgnoreCase(req.getCode()).isPresent()) {
            throw new IllegalArgumentException("Discount code already exists: " + req.getCode());
        }
        DiscountCode code = DiscountCode.builder()
                .code(req.getCode().toUpperCase())
                .type(req.getType())
                .value(req.getValue())
                .minBookingAmount(req.getMinBookingAmount())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .maxUses(req.getMaxUses())
                .usedCount(0)
                .active(true)
                .build();
        return discountCodeRepository.save(code);
    }

    public List<DiscountCode> listAll() {
        return discountCodeRepository.findAll();
    }

    public Optional<DiscountCode> findByCode(String code) {
        return discountCodeRepository.findByCodeIgnoreCase(code);
    }

    public DiscountCode get(Long id) {
        return discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount code not found: " + id));
    }

    @Transactional
    public void deactivate(Long id) {
        get(id).setActive(false);
    }
}
