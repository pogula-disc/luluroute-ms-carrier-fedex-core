package com.luluroute.ms.carrier.entity;


import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.luluroute.ms.carrier.fedex.util.Constants.FEDEX_SUPPORT_USER;

@Entity
@Table(name = "file_record", schema = "fedex")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class FuseFileRecordEntity implements Serializable {


    @Id
    @Column(name = "file_record_id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_name", unique = true, nullable = false)
    private String fileName;

    @Column(name = "file_location", nullable = false)
    private String fileLocation;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "ref_1")
    private String ref1;

    @Column(name = "ref_2")
    private String ref2;

    @Column(name = "ref_3")
    private String ref3;

    @Column(name = "active", nullable = false)
    private int active;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (createdBy == null) {
            createdBy = FEDEX_SUPPORT_USER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        updatedBy = FEDEX_SUPPORT_USER;
    }

}
