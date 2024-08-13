package com.luluroute.ms.carrier.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "enttholiday", schema = "domain")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityHoliday implements Serializable {

    private final static long serialVersionUID = 7702L;

    @Id
    @Column(name = "holidayid", unique = true, nullable = false)
    private UUID holidayId;

    @Column(name = "holidaytype", nullable = false)
    private String holidayType;

    @Column(name = "holidaydate", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date holidayDate;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "ref_1")
    private String ref1;

    @Column(name = "ref_2")
    private String ref2;

    @Column(name = "ref_3")
    private String ref3;

    @Column(name = "active")
    private int active;

    @Column(name = "createddate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "createdby")
    private String createdBy;

    @Column(name = "updateddate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    @Column(name = "updatedby")
    private String updatedBy;
}
