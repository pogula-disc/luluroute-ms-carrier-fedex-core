package com.luluroute.ms.carrier.rateshop.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "fedex")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
@Getter
@Setter
public class RateshopRateDb {

    @Id
    @Column(nullable = false)
    private UUID rateshopRateId;

    @Column
    private String rateshopZone;

    @Column
    private String destinationZipCodePrefix;

    @Column(nullable = false)
    private String modeCode;

    @Column(nullable = false)
    private Double baseRate;

}
