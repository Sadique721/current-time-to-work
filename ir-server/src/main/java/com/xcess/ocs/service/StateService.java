package com.xcess.ocs.service;

import com.xcess.ocs.dto.StateDTO;
import com.xcess.ocs.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StateService {

    private final StateRepository stateRepository;

    public List<StateDTO> getStatesByCountryIso(String countryIso) {
        log.debug("Fetching states for country ISO: {}", countryIso);
        return stateRepository.findByCountryIsoOrderByStateName(countryIso).stream()
                .map(state -> {
                    StateDTO dto = new StateDTO();
                    dto.setStateId(state.getStateId());
                    dto.setCountryIso(state.getCountryIso());
                    dto.setStateCode(state.getStateCode());
                    dto.setStateName(state.getStateName());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
