package com.xcess.ocs.controller;

import com.xcess.ocs.dto.StateDTO;
import com.xcess.ocs.service.StateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/states")
@RequiredArgsConstructor
@Tag(name = "State", description = "State reference data endpoints")
public class StateController {

    private final StateService stateService;

    @GetMapping
    @Operation(summary = "Get states by country ISO code", description = "Returns states for the specified country ISO code")
    public ResponseEntity<List<StateDTO>> getStatesByCountryIso(@RequestParam String countryIso) {
        log.info("REST request to get states for country ISO: {}", countryIso);
        List<StateDTO> states = stateService.getStatesByCountryIso(countryIso);
        return ResponseEntity.ok(states);
    }
}
