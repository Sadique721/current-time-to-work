package com.xcess.ocs.dto.search;

import com.xcess.ocs.entity.ClearingHouseStatus;
import com.xcess.ocs.entity.ClearingHouseType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Search criteria for clearing houses")
public class ClearingHouseSearchDTO {

    @Schema(description = "Partial name match", example = "GSMA")
    private String name;

    @Schema(description = "DCH, FCH, BOTH")
    private ClearingHouseType type;

    @Schema(description = "ACTIVE, INACTIVE")
    private ClearingHouseStatus status;
}
