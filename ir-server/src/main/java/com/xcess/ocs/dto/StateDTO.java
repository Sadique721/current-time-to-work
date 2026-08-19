package com.xcess.ocs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StateDTO {

    private Long stateId;
    private String countryIso;
    private String stateCode;
    private String stateName;
}
