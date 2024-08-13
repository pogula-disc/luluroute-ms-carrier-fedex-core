package com.luluroute.ms.carrier.service;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.ursa.uvsdk.UVConstants;
import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.luluroute.ms.carrier.config.FeatureConfig;
import com.luluroute.ms.carrier.config.FedExModeConfig;
import com.luluroute.ms.carrier.entity.ServiceEnquiry;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.fuseapi.service.helper.CustomLabelHelper;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.OrderType;
import com.luluroute.ms.carrier.model.CustomerReference;
import com.luluroute.ms.carrier.model.FedExServiceInfo;
import com.luluroute.ms.carrier.model.UrsaPayload;
import com.luluroute.ms.carrier.repository.ServiceEnquiryRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.fedex.ursa.uvsdk.UVConstants.*;
import static com.luluroute.ms.carrier.fedex.util.Constants.GROUND_ECONOMY_MODE;
import static com.luluroute.ms.carrier.fedex.util.Constants.GROUND_SHIPMENT_ACCOUNT_NO;
import static com.luluroute.ms.carrier.fedex.util.Constants.SMART_POST_HUB_ID;
import static com.luluroute.ms.carrier.fedex.util.Constants.SMART_POST_ID;
import static com.luluroute.ms.carrier.fedex.util.Constants.SMART_POST_METER_ID;
import static com.luluroute.ms.carrier.fedex.util.Constants.STRING_EMPTY;
import static com.luluroute.ms.carrier.fedex.util.Constants.TAX_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceEnquiryService {

    @Autowired
    private ServiceEnquiryRepository serviceEnquiryRepository;
    @Autowired
    private FedExModeConfig fedExModeConfig;
    @Autowired
    private FeatureConfig featureConfig;

    private ObjectMapper objectMapper;

    private static String CREATED_BY = "fedex-core";
    private static String BARCODE_TYPE = "FEDEX_1D";

    @PostConstruct
    public void initializeMapper() {
        this.objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.addMixIn(SpecificRecordBase.class, JacksonIgnoreAvroProperties.class);
    }

    public void saveServiceEnquiry(ShipmentMessage shipmentMessage, UrsaPayload ursaPayload, ShipmentArtifact shipmentArtifact,
                                   Map<String, String> carrierMetaDataMap, String shipmentCorrelationId,
                                   String timezone) throws ShipmentMessageException {
        ServiceEnquiry serviceEnquiry = new ServiceEnquiry();

        ShipmentInfo shipmentInfo = shipmentMessage.getMessageBody().getShipments().get(0);

        FedExServiceInfo fedExServiceInfo = CustomLabelHelper.getServiceDetailsByMode(fedExModeConfig,
                shipmentInfo.getTransitDetails().getTransitMode());

        serviceEnquiry.setAccount(carrierMetaDataMap.get(Constants.ACCOUNT_TO_BILL));
        serviceEnquiry.setMasterAccount(carrierMetaDataMap.get(Constants.MASTER_ACCOUNT));
        serviceEnquiry.setCarrierServiceCode(ServiceEnquiry.CarrierServiceCode.valueOf(fedExServiceInfo.getCarrierServiceCode()));
        serviceEnquiry.setModeCode(shipmentInfo.getTransitDetails().getTransitMode());
        serviceEnquiry.setCreatedDate(LocalDateTime.now());
        serviceEnquiry.setCreatedBy(CREATED_BY);
        serviceEnquiry.setServiceEnquiryId(UUID.randomUUID());
        serviceEnquiry.setMeter(carrierMetaDataMap.get(Constants.METER_NO));
        serviceEnquiry.setMessageCorrelationId(shipmentMessage.getMessageHeader().getMessageCorrelationId());
        serviceEnquiry.setTrackingNo(shipmentInfo.getTransitDetails().getTrackingNo());
        serviceEnquiry.setManifestStatus(ServiceEnquiry.ManifestStatus.NOT_MANIFESTED);
        serviceEnquiry.setShipmentCorrelationId(shipmentCorrelationId);
        serviceEnquiry.setRequestPayload(objectMapper.valueToTree(shipmentMessage));
        serviceEnquiry.setResponsePayload(objectMapper.valueToTree(shipmentArtifact));
        serviceEnquiry.setShipmentStatus(ServiceEnquiry.ShipmentStatus.CREATED);
        serviceEnquiry.setUrsaInput(objectMapper.valueToTree(buildURSAValueToKey(ursaPayload.getInput())));
        serviceEnquiry.setUrsaOutput(objectMapper.valueToTree(buildURSAValueToKey(ursaPayload.getOut())));
        ServiceEnquiry.ShipmentType shipmentType = CustomLabelHelper.isInternational(shipmentInfo) ?
                ServiceEnquiry.ShipmentType.INTERNATIONAL : ServiceEnquiry.ShipmentType.DOMESTIC;
        serviceEnquiry.setShipmentType(shipmentType);
        serviceEnquiry.setBarcodeValue(getBarcodeValue(shipmentInfo,ursaPayload));
        serviceEnquiry.setBarcodeType(BARCODE_TYPE);
        LocalDateTime ldt = Instant.ofEpochSecond(shipmentInfo.getTransitDetails().getDateDetails().getPlannedShipDate())
                .atZone(ZoneId.of(timezone)).toLocalDateTime();
        serviceEnquiry.setShipmentDate(ldt);
        serviceEnquiry.setGsn(carrierMetaDataMap.get(GROUND_SHIPMENT_ACCOUNT_NO));
        serviceEnquiry.setSpId(carrierMetaDataMap.get(SMART_POST_ID));
        serviceEnquiry.setSpmId(carrierMetaDataMap.get(SMART_POST_METER_ID));
        serviceEnquiry.setSpHubId(carrierMetaDataMap.get(SMART_POST_HUB_ID));
        serviceEnquiry.setTaxId(carrierMetaDataMap.get(TAX_ID));
        serviceEnquiry.setCustomerReferences(objectMapper.valueToTree(featureConfig.isSfsEnabled ? buildCustomerRefSfsEnabled(shipmentInfo) : buildCustomerReferences(shipmentInfo)));
        serviceEnquiryRepository.save(serviceEnquiry);
    }

    private static String getBarcodeValue(ShipmentInfo shipmentInfo,UrsaPayload ursaPayload) {
        if(shipmentInfo.getTransitDetails().getTransitMode().equalsIgnoreCase(GROUND_ECONOMY_MODE))
            return ursaPayload.getOut().get((long) UVConstants.FDXPSP_O_FGELBL_FIELD_D3);
        return ursaPayload.getOut().get((long) UVConstants.FDXPSP_O_LBL_FIELD_7);
    }

    abstract class JacksonIgnoreAvroProperties {
        @JsonIgnore
        public abstract org.apache.avro.Schema getClassSchema();

        @JsonIgnore
        public abstract org.apache.avro.specific.SpecificData getSpecificData();

        @JsonIgnore
        public abstract java.lang.Object get(int field$);

        @JsonIgnore
        public abstract org.apache.avro.Schema getSchema();
    }

    public static Map<String, String> buildURSAValueToKey(LinkedHashMap<Long, String> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> UVTaggedIOTagName.get(entry.getKey()),
                        entry -> trimUrsaMap(entry.getKey(), entry.getValue())));
    }

    /**
     * Trimming URSA Output 2D can truncate special characters,
     * therefore returning direct value for 2D barcode.
     * @param key entry key
     * @param value entry value
     * @return trimmed string value
     */
    private static String trimUrsaMap(long key, String value) {
        return (key == FDXPSP_O_SHIPMENT_2D_BARCODE) ? value : StringUtils.trim(value);
    }

    /**
     * Mapping for Customer References saved to Service Enquiry Table. Fields are used in Manifest HUF files.
     * <p>Mapping</p>
     * <ul>
     *     <li>CUSTOMER REFERENCE: Order Id</li>
     *     <li>INVOICE NUMBER: For INTL, map to Order Type. For Domestic, map to TCLPNID</li>
     *     <li>P O NUMBER: TCLPNID (ECOMM), ShipVia (RETAIL)</li>
     * </ul>
     * @param shipmentInfo
     * @return list of customer references
     */
     private List<CustomerReference> buildCustomerReferences(ShipmentInfo shipmentInfo){
         String orderType = shipmentInfo.getOrderDetails().getOrderType();
         String invoiceNumber = getInvoiceNumber(shipmentInfo, orderType);
         String poNumber = getPONumber(shipmentInfo, orderType);
         return List.of(CustomerReference.builder()
                         .customerReferenceType("CUSTOMER_REFERENCE")
                         .value(shipmentInfo.getOrderDetails().getOrderId())
                         .build(),
                 CustomerReference.builder()
                         .customerReferenceType("INVOICE_NUMBER")
                         .value(invoiceNumber)
                         .build(),
                 CustomerReference.builder()
                         .customerReferenceType("P_O_NUMBER")
                         .value(poNumber)
                         .build()
         );
    }

    private List<CustomerReference> buildCustomerRefSfsEnabled(ShipmentInfo shipmentInfo){
        List<CustomerReference> customerReferences = new ArrayList<>();
        String orderType = shipmentInfo.getOrderDetails().getOrderType();
        String invoiceNumber = getInvoiceNumber(shipmentInfo, orderType);
        String poNumber = getPONumber(shipmentInfo, orderType);
        addCustomerRef(customerReferences, "CUSTOMER_REFERENCE", shipmentInfo.getOrderDetails().getOrderId());
        addCustomerRef(customerReferences, "INVOICE_NUMBER", invoiceNumber);
        addCustomerRef(customerReferences, "P_O_NUMBER", poNumber);
        return customerReferences;
    }

    private void addCustomerRef(List<CustomerReference> customerReferences, String custRefType, String value) {
         if (!StringUtils.isNullOrEmpty(value)) {
             CustomerReference ref = CustomerReference.builder().customerReferenceType(custRefType).value(value).build();
             customerReferences.add(ref);
         }
    }

    private String getPONumber(ShipmentInfo shipmentInfo, String orderType) {
        String poNumber = STRING_EMPTY;
        if (OrderType.isRetailOrder(orderType)) {
            poNumber = shipmentInfo.getOrderDetails().getShipVia();
        } else if (OrderType.isEcommOrder(orderType)) {
            poNumber = shipmentInfo.getOrderDetails().getTclpnid();
        } else if (OrderType.isStratOrder(orderType)) {
            poNumber = shipmentInfo.getOrderDetails().getLaneName();
        }
        return poNumber != null ? poNumber.toUpperCase() : null;
    }

    private String getInvoiceNumber(ShipmentInfo shipmentInfo, String orderType) {
         String invoiceNumber = STRING_EMPTY;
        if (OrderType.isRetailOrder(orderType) || OrderType.isStratOrder(orderType)) {
            invoiceNumber = shipmentInfo.getOrderDetails().getTclpnid();
        } else if (OrderType.isEcommOrder(orderType)) {
            invoiceNumber = orderType;
        }
        return invoiceNumber.toUpperCase();
    }

}
