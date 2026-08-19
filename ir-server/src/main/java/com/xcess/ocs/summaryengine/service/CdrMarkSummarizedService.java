package com.xcess.ocs.summaryengine.service;

import com.xcess.ocs.repository.SmsRatedCdrRepository;
import com.xcess.ocs.repository.UsageRatedCdrRepository;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CdrMarkSummarizedService {

    @Autowired
    private VoiceRatedCdrRepository voiceRatedCdrRepository;

    @Autowired
    private SmsRatedCdrRepository smsRatedCdrRepository;

    @Autowired
    private UsageRatedCdrRepository usageRatedCdrRepository;

    /**
     * Mark OUTGOING-rated CDRs as summarized across all three service types.
     * Called immediately after outgoing summary generation succeeds so that a
     * failure in incoming summary generation cannot cause outgoing CDRs to be
     * re-summarized on the next scheduler run.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long markOutgoingAsSummarized(LocalDateTime startTime, LocalDateTime endTime) {
        int voice = voiceRatedCdrRepository.markOutgoingCdrsAsSummarized(startTime, endTime);
        int sms   = smsRatedCdrRepository.markOutgoingCdrsAsSummarized(startTime, endTime);
        int usage = usageRatedCdrRepository.markOutgoingCdrsAsSummarized(startTime, endTime);
        log.info("Marked OUTGOING as summarized — Voice: {}, SMS: {}, Usage: {}", voice, sms, usage);
        return voice + sms + usage;
    }

    /**
     * Mark INCOMING-rated CDRs as summarized across all three service types.
     * Called immediately after incoming summary generation succeeds.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long markIncomingAsSummarized(LocalDateTime startTime, LocalDateTime endTime) {
        int voice = voiceRatedCdrRepository.markIncomingCdrsAsSummarized(startTime, endTime);
        int sms   = smsRatedCdrRepository.markIncomingCdrsAsSummarized(startTime, endTime);
        int usage = usageRatedCdrRepository.markIncomingCdrsAsSummarized(startTime, endTime);
        log.info("Marked INCOMING as summarized — Voice: {}, SMS: {}, Usage: {}", voice, sms, usage);
        return voice + sms + usage;
    }
}
