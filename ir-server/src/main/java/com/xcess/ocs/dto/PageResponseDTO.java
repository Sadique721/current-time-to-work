package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "16. Get Response",
        description = "Schema to hold  response details",
        accessMode = Schema.AccessMode.READ_ONLY
)
public class PageResponseDTO<D> {

    @Schema(
            description = "Schema to hold pagination details"
    )
    private PaginationDetailsDTO pageDetails;

    @Schema(
            description = "Content of the page"
    )
    private List<D> content;
}
