package com.luluroute.ms.carrier.rule;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.domain.Shipment.Message.MessageBodyInfo;
import com.logistics.luluroute.domain.Shipment.Message.MessageHeaderInfo;
import com.logistics.luluroute.domain.Shipment.Service.RequestInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.ItemInfo;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.luluroute.ms.carrier.fedex.util.Constants.*;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ShipmentCreateBRule {


    /**
     * @param shipmentInfoAPI
     * @param messageCorrelationId
     * @return
     */
    public static com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage buildShipmentMessage(
            ShipmentInfo shipmentInfoAPI, String messageCorrelationId, String timeZone) {

        com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage shipmentMessage = com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage
                .builder().MessageBody(MessageBodyInfo.builder().shipments(new ArrayList<>()).build())
                .MessageHeader(MessageHeaderInfo.builder().messageCorrelationId(messageCorrelationId).build())
                .RequestHeader(RequestInfo.builder().extended(new ArrayList<>()).build()).build();

        ItemInfo accountItem = new ItemInfo();
        accountItem.setKey(ACCOUNT_TO_BILL);
        // accountItem.setValue(accountDetailMap.get(ACCOUNT_TO_BILL));
        shipmentMessage.getRequestHeader().getExtended().add(accountItem);

        ItemInfo dispatchItem = new ItemInfo();
        dispatchItem.setKey(DISPATCH_ID);
        // dispatchItem.setValue(accountDetailMap.get(DISPATCH_ID));
        shipmentMessage.getRequestHeader().getExtended().add(dispatchItem);

        ItemInfo productItem = new ItemInfo();
        productItem.setKey(PRODUCT_ID);
        // productItem.setValue(accountDetailMap.get(PRODUCT_ID));
        shipmentMessage.getRequestHeader().getExtended().add(productItem);

        ItemInfo packageItem = new ItemInfo();
        packageItem.setKey(PACKAGE_TYPE);
        // packageItem.setValue(accountDetailMap.get(PACKAGE_TYPE));
        shipmentMessage.getRequestHeader().getExtended().add(packageItem);

        shipmentMessage.getMessageBody().getShipments().add(shipmentInfoAPI);
        ItemInfo timezoneItem = new ItemInfo();
        timezoneItem.setKey(ENTITY_TIMEZONE);
        timezoneItem.setValue(timeZone);
        shipmentMessage.getRequestHeader().getExtended().add(timezoneItem);

        return shipmentMessage;
    }

    /**
     *
     * @param routeRuleArtifact
     * @param shipmentInfo
     * @return
     */
    public static ShipmentInfo checkAndUpdatePlannedShippedDate(ShipmentArtifact routeRuleArtifact, ShipmentInfo shipmentInfo) {
        // Use 5100 artifacts PSD
        if (routeRuleArtifact.getArtifactBody().getTransitTimes() != null) {
            shipmentInfo.getTransitDetails().getDateDetails()
                    .setPlannedShipDate(routeRuleArtifact.getArtifactBody().getTransitTimes().get(0).getPickUpDate());
            shipmentInfo.getTransitDetails().getDateDetails()
                    .setPlannedDeliveryDate(routeRuleArtifact.getArtifactBody().getTransitTimes().get(0).getPlannedDeliveryDate());
        }

        return shipmentInfo;
    }

}
