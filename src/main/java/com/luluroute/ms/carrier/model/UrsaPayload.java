package com.luluroute.ms.carrier.model;

import java.util.LinkedHashMap;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class UrsaPayload {

    LinkedHashMap<Long, String> input;
    LinkedHashMap<Long, String> out;
}
