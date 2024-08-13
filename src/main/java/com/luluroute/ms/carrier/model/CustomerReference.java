package com.luluroute.ms.carrier.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerReference {
    @JsonProperty("CustomerReferenceType")
    private String customerReferenceType;
    @JsonProperty("value")
    private String value;
}
