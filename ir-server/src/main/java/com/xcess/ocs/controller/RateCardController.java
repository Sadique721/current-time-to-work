package com.xcess.ocs.controller;

import com.xcess.ocs.dto.RateCardRequestDTO;
import com.xcess.ocs.dto.RateCardResponseDTO;
import com.xcess.ocs.service.RateCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rate-card")
@RequiredArgsConstructor
public class RateCardController {
    private final RateCardService rateCardService;

    @PostMapping
    public ResponseEntity<RateCardResponseDTO> getRateCard(@RequestBody RateCardRequestDTO requestDTO) {

            RateCardResponseDTO response = rateCardService.getRateCard(requestDTO);
            return ResponseEntity.ok(response);
    }
}
