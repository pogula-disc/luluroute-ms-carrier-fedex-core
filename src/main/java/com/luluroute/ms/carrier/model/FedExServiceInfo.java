package com.luluroute.ms.carrier.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FedExServiceInfo {
    List<String> transitModes;
    String serviceLetter;
    String serviceName;
    String serviceClass;
    String carrierServiceCode;
    String manifestFileName;
}
