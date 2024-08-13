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
@RedisHash("EditFiles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Deprecated()
public class EditFileEntity implements Serializable {


    @Id
    UUID id;

    String fileName;

    String fileLocation;

    @Indexed
    String state;
}
