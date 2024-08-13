package com.luluroute.ms.carrier.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "enttholidayapplicable", schema = "transit")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityApplicableHoliday implements Serializable {

    private final static long serialVersionUID = 7702L;

    @Id
    @Column(name = "holidayapplicableid", unique = true, nullable = false)
    private UUID holidayapplicableid;

    @Column(name = "holidayid", unique = true, nullable = false)
    private UUID holidayId;

    @Column(name = "applicabletype", nullable = false)
    private String applicableType;

    @Column(name = "carrierid")
    private String carrierId;

    @Column(name = "modeid")
    private String modeId;

    @Column(name = "country")
    private String country;

    @Column(name = "state")
    private String state;

    @Column(name = "city")
    private String city;

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
