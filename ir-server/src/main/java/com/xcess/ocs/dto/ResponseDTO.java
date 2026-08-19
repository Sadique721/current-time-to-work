package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "1. Response",
        description = "Schema to hold successful response information"
)
public class ResponseDTO {
        @Schema(description = "Status code in the response")
        private String statusCode;

        @Schema(description = "Status message in the response")
        private String statusMsg;

        public static ResponseDTO ok(String message) {
                return ResponseDTO.builder()
                                .statusCode("200")
                                .statusMsg(message)
                                .build();
        }

        public static ResponseDTO failed(String message) {
                return ResponseDTO.builder()
                                .statusCode("417")
                                .statusMsg(message)
                                .build();
        }
}
