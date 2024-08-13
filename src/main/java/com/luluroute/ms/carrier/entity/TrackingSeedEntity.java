package com.luluroute.ms.carrier.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seeding", schema = "fedex")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackingSeedEntity implements Serializable {

    private final static long serialVersionUID = 7702L;

    @Id
    @Column(name = "seedid", unique = true, nullable = false)
    private UUID seedId;

    @Column(name = "account", nullable = false)
    private String account;

    @Column(name = "seedbegin", nullable = false)
    private Long seedBegin;

    @Column(name = "seedend")
    private Long seedEnd;

    @Column(name = "seedcurrent")
    private Long seedCurrent;

    @Column(name = "seedprefix")
    private String seedPrefix;

    @Column(name = "meter")
    private String meter;

    @Column(name = "seedenabled")
    private Integer seedEnabled;

    @Column(name = "seedsequence")
    private Integer seedSequence;

    @Column(name = "ref_1")
    private String ref1;

    @Column(name = "ref_2")
    private String ref2;

    @Column(name = "ref_3")
    private String ref3;

    @Column(name = "active", nullable = false)
    private Integer active;

    @Column(name = "createddate", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "createdby", nullable = false)
    private String createdBy;

    @Column(name = "updateddate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    @Column(name = "updatedby")
    private String updatedBy;
}
