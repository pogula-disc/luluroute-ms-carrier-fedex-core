package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.HubInfo;
import com.luluroute.ms.carrier.fedex.exception.LabelCreationException;
import com.luluroute.ms.carrier.fedex.exception.MappingFormatException;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.model.UrsaPayload;
import java.util.Map;

/**
 * Service which creates a FedEX Label for a given Shipment Message
 */
public interface FedExLabelService {
    UrsaPayload generateUrsaPayload(ShipmentInfo shipmentInfo, Map<String, String> carrierMetaDataMap, boolean isSaturdayDelivery, HubInfo retailHubInfo)
            throws LabelCreationException, MappingFormatException, ShipmentMessageException;
}
