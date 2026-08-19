package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.Partner;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tracks the last used TAP OUT file sequence number per roaming partner.
 *
 * GSMA standard (TD.57) requires TAP OUT files to be sequentially numbered per
 * sender-recipient TADIG pair. Sequence numbers must never repeat or skip.
 *
 * Production-grade design:
 *   - One row per partner (unique constraint on partner_id)
 *   - Incremented atomically using SELECT FOR UPDATE (pessimistic lock)
 *     in TapOutSequenceRepository.getNextSequence() to prevent duplicates
 *     under concurrent TAP OUT generation
 *   - Sequence starts at 1 and increments by 1 per file
 *
 * Filename format (GSMA TD.57):
 *   TD{ourTadig(5)}{partnerTadig(5)}{zeroPad(sequence, 5)}
 *   e.g. "TDEUR01AUTPT00006"
 *
 * The sequence is stored here and NOT derived from incoming TAP IN files,
 * because TAP OUT files are generated from our rated_cdr records independently.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tap_out_sequence",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_tap_out_sequence_partner",
               columnNames = "partner_id"
       ))
public class TapOutSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * The roaming partner this sequence belongs to.
     * One sequence counter per partner — unique constraint enforced.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    /**
     * The last sequence number used for this partner's TAP OUT files.
     * Starts at 0 (first file will be sequence 1).
     * Incremented atomically before each file generation.
     */
    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence = 0;

    /**
     * Timestamp of the last TAP OUT file generated for this partner.
     * Used for audit trail and monitoring.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
