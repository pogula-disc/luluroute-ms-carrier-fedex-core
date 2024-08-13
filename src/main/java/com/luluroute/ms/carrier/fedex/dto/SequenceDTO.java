package com.luluroute.ms.carrier.fedex.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class SequenceDTO {

    @NonNull
    private String key;
    private long sequence;
    private String description;

}
