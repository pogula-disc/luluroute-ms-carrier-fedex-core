package com.luluroute.ms.carrier.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.UUID;


/**
 * @deprecated RedisHash objects cannot be serialized to JSON string as required by team standard. Only use @Cacheable
 */
@Data
@RedisHash("UrsaFiles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Deprecated()
public class UrsaFileEntity implements Serializable {


    @Id
    UUID id;

    String fileName;

    String fileLocation;

    String effectiveDate;

    String expirationDate;

    @Indexed
    String state;
}
