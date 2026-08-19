package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.roaming.entity.CallType;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TapFileCdrDetailsResponseDTO {
    private String senderTadig;
    private String recipientTadig;
    private BigInteger tapDecimalPlaces;
    private Integer fileSequenceNo;
    private List<RoamingCdrDetailDTO> cdrs;

    @Data
    public static class RoamingCdrDetailDTO {
        private CallType callType;
        private String callingNumber;
        private String calledNumber;
        private LocalDateTime callStartTime;
        private Integer callDurationSec;
        private String homePlmn;
        private String visitedPlmn;
        private String currency;
        private String ratePackageName;
        private BigDecimal appliedRate;
        private BigDecimal totalUsage;
        private Integer eventNos;
    }
}
