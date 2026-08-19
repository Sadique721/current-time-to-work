package com.xcess.ocs.service;

import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.repository.SmsRatedCdrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsRatedCdrService {

    private static final Logger logger = LoggerFactory.getLogger(SmsRatedCdrService.class);

    private final SmsRatedCdrRepository smsRatedCdrRepository;

    public SmsRatedCdrService(SmsRatedCdrRepository smsRatedCdrRepository) {
        this.smsRatedCdrRepository = smsRatedCdrRepository;
    }

    public SmsRatedCdr saveRatedCdr(SmsRatedCdr smsRatedCdr) {
        try {
            SmsRatedCdr savedEntity = smsRatedCdrRepository.save(smsRatedCdr);

            logger.info("Saved SmsRatedCdr: id={}, calling={}, called={}, inRate={}, outRate={}, eventNos={}",
                    savedEntity.getSmsRatedCdrId(),
                    savedEntity.getCallingNumber(),
                    savedEntity.getCalledNumber(),
                    savedEntity.getIncomingAppliedRate(),
                    savedEntity.getOutgoingAppliedRate(),
                    savedEntity.getEventNos());

            return savedEntity;

        } catch (Exception e) {
            logger.error("Failed to save enhanced SmsRatedCdr: calling={}, called={}, " +
                        "incoming ratingStatus={}, outGoing ratingStatus={}, error={}",
                    smsRatedCdr.getCallingNumber(),
                    smsRatedCdr.getCalledNumber(),
                    smsRatedCdr.getIncomingRatingStatus(),
                    smsRatedCdr.getOutgoingRatingStatus(),
                    e.getMessage(), e);
            throw e;
        }
    }
}
