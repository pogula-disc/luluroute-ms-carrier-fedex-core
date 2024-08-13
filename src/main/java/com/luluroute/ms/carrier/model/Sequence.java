package com.luluroute.ms.carrier.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sequence", schema = "fedex")
@Getter
@Setter
public class Sequence {

    @Id
    @Column(name = "sequence_id", unique = true, nullable = false)
    private UUID sequenceId;

    @Column(name = "key", length = 100, nullable = false)
    private String key;

    @Column(name = "sequence", nullable = false)
    private long sequence;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @PrePersist
    void create() {
        this.sequenceId = UUID.randomUUID();
        this.createdDate = LocalDateTime.now();
    }

}
