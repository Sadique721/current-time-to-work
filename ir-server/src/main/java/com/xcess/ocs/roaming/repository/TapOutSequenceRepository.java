package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.TapOutSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for TapOutSequence entity.
 *
 * The critical method here is findByPartnerIdForUpdate() which uses
 * PESSIMISTIC_WRITE lock (SELECT FOR UPDATE) to ensure atomic sequence
 * increment under concurrent TAP OUT generation.
 *
 * Without the lock, two concurrent TAP OUT jobs for the same partner
 * could read the same lastSequence and generate duplicate sequence numbers,
 * violating GSMA TD.57 requirements.
 */
@Repository
public interface TapOutSequenceRepository extends JpaRepository<TapOutSequence, Long> {

    /**
     * Find the sequence record for a partner with a PESSIMISTIC_WRITE lock.
     *
     * This is SELECT ... FOR UPDATE — blocks other transactions from reading
     * or modifying this row until the current transaction commits.
     *
     * Usage in TapOutFileGenerationService:
     *   1. Call this inside a @Transactional method
     *   2. Increment lastSequence
     *   3. Save the updated record
     *   4. Use the new sequence number for the filename
     *   5. Transaction commits → lock released
     *
     * @param partnerId the partner ID to lock the sequence row for
     * @return the locked TapOutSequence, or empty if not yet created
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TapOutSequence s WHERE s.partner.partnerId = :partnerId")
    Optional<TapOutSequence> findByPartnerIdForUpdate(@Param("partnerId") Long partnerId);

    /**
     * Find sequence record without lock — for read-only queries (monitoring, reporting).
     */
    Optional<TapOutSequence> findByPartner_PartnerId(Long partnerId);
}
