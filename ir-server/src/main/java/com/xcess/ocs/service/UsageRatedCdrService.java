package com.xcess.ocs.service;

import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.repository.UsageRatedCdrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageRatedCdrService {

    private final UsageRatedCdrRepository usageRatedCdrRepository;

    @Transactional
    public UsageRatedCdr saveRatedCdr(UsageRatedCdr cdr) {
        return usageRatedCdrRepository.save(cdr);
    }
}
