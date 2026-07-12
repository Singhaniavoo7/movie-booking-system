package com.moviebooking.service;

import com.moviebooking.dto.request.PricingTierRequest;
import com.moviebooking.entity.PricingTier;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.PricingTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingTierService {

    private final PricingTierRepository pricingTierRepository;

    @Transactional
    public PricingTier create(PricingTierRequest req) {
        PricingTier tier = PricingTier.builder()
                .name(req.getName())
                .seatTypePrices(req.getSeatTypePrices())
                .active(true)
                .build();
        return pricingTierRepository.save(tier);
    }

    public List<PricingTier> listAll() {
        return pricingTierRepository.findAll();
    }

    public PricingTier get(Long id) {
        return pricingTierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing tier not found: " + id));
    }
}
