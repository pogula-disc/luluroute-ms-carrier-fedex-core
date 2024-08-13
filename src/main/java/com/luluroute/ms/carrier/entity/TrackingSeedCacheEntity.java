package com.luluroute.ms.carrier.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingSeedCacheEntity {

    @Id
    UUID id;

    @Indexed
    int meter;

    @Indexed
    int account;
    Long beginTN;
    Long endTN;
    Long currentTN;
    int rangeSize;
}
