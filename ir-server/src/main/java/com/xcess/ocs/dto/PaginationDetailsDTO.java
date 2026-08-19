package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "17. Page Details",
        description = "Schema to hold pagination details",
        accessMode = Schema.AccessMode.READ_ONLY
)
public class PaginationDetailsDTO {

    @Schema(
            description = "Total number of pages",
            example = "6"
    )
    private int totalPages;

    @Schema(
            description = "Total number of elements",
            example = "30"
    )
    private long totalRecords;

    @Schema(
            description = "Size of the page",
            example = "5"
    )
    private int totalRecordsPerPage;

    @Schema(
            description = "Current page number",
            example = "1"
    )
    private int currentPageNumber;

//
//    @Schema(
//            description = "Whether the current page is the last page",
//            example = "false",
//            accessMode = Schema.AccessMode.READ_ONLY
//    )
//    private boolean lastPage;
//
//    @Schema(
//            description = "Whether the current page is the first page",
//            example = "true",
//            accessMode = Schema.AccessMode.READ_ONLY
//    )
//    private boolean firstPage;
//
//    @Schema(
//            description = "Whether there is a next page",
//            example = "true",
//            accessMode = Schema.AccessMode.READ_ONLY
//    )
//    private boolean hasNextPage;
//
//    @Schema(
//            description = "Whether there is a previous page",
//            example = "false",
//            accessMode = Schema.AccessMode.READ_ONLY
//    )
//    private boolean hasPreviousPage;

}
