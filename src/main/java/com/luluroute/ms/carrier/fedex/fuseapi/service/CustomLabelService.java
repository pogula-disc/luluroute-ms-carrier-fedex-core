package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.logistics.luluroute.domain.Shipment.Service.ReturnInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.logistics.luluroute.domain.Shipment.Shared.MeasureItem;
import com.logistics.luluroute.redis.shipment.entity.DCRoute;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.redis.shipment.entity.HubInfo;
import com.luluroute.ms.carrier.config.FeatureConfig;
import com.luluroute.ms.carrier.config.FedExModeConfig;
import com.luluroute.ms.carrier.entity.CustomLabel;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.fuseapi.service.helper.CustomLabelHelper;
import com.luluroute.ms.carrier.fedex.util.*;
import com.luluroute.ms.carrier.model.FedExServiceInfo;
import com.luluroute.ms.carrier.model.UrsaPayload;
import com.luluroute.ms.carrier.repository.CustomLabelRepository;
import com.luluroute.ms.carrier.service.RedisRehydrateService;
import com.luluroute.ms.carrier.service.helper.ShipmentCreateHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.text.DecimalFormat;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.fedex.ursa.uvsdk.UVConstants.*;
import static com.luluroute.ms.carrier.fedex.fuseapi.service.helper.CustomLabelHelper.*;
import static com.luluroute.ms.carrier.fedex.util.Constants.*;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.*;

@Service
@Slf4j
public class CustomLabelService {

    @Autowired
    private CustomLabelRepository labelRepository;

    @Autowired
    private FedExModeConfig fedExModeConfig;
    @Autowired
    private RedisCacheLoader redisCacheLoader;
    @Autowired
    private RedisRehydrateService redisRehydrateService;
    @Autowired
    private FeatureConfig featureConfig;
    @Value("${fedex.rc.domestic.entityCode}")
    private String domesticEntityCode;
    @Value("${fedex.rc.international.entityCode}")
    private String internationalEntityCode;
    @Value("${fedex.label.control-characters.hex}")
    private List<String> controlCharList;
    @Value("${fedex.client.clientProductId}")
    private String clientProductId;

    @Value("${fedex.client.clientProductVersion}")
    private String clientProductVersion;

    /**
     * @param entityProfile   : Entity Profile from Redis
     * @param shipmentInfo    : Incoming Shipment
     * @return : BASE64 encoded label content
     */
    public String generateCustomLabel(EntityPayload entityProfile, ShipmentInfo shipmentInfo,
                                      Map<String, String> accountDetailMap,
                                      UrsaPayload ursaPayload, HubInfo hubInfo) throws ShipmentMessageException {
        String msg = "CustomLabelService.generateCustomLabel()";
        String labelContent = STRING_EMPTY;
        try {

            final List<CustomLabel> customLabelList = labelRepository.findByModeAndOrderType(shipmentInfo.getTransitDetails().getTransitMode(),
                    shipmentInfo.getOrderDetails().getOrderType());

            if (!CollectionUtils.isEmpty(customLabelList)) {
                CustomLabel customLabel = customLabelList.get(0);
                labelContent = customLabel.getContent();

                EntityPayload entityPayload = getEntityForReturnCentre(shipmentInfo);

                if (entityPayload != null) {
                    //new method to get from entity payload
                    labelContent = prepareReturnAddress(entityPayload, labelContent);
                    ReturnInfo returnInfo = shipmentInfo.getShipmentHeader().getReturnLocation();
                    // Persist the return address for AWB
                    if (null != returnInfo)
                        updateReturnAddressInShipRequest(entityPayload, returnInfo.getReturnAddress());
                } else {
                    labelContent = prepareFromAddress(shipmentInfo, labelContent);
                }
                if(OrderType.isRetailOrder(shipmentInfo.getOrderDetails().getOrderType())) {
                    if (hubInfo != null) {
                        labelContent = prepareToAddressFromHubInfo(shipmentInfo, hubInfo, labelContent);
                    } else {
                        labelContent = prepareToAddress(shipmentInfo, labelContent);
                    }
                } else {
                    labelContent = prepareToAddress(shipmentInfo, labelContent);
                }
                labelContent = preparePlannedShipDate(entityProfile, shipmentInfo, labelContent);
                labelContent = prepareUpperRight(shipmentInfo, labelContent, accountDetailMap);
                labelContent = prepareServiceIdentifier(shipmentInfo, labelContent);
                labelContent = prepareDeliveryAddressBarCode(shipmentInfo, ursaPayload, labelContent);
                labelContent = prepareTrackingNumberAndTag(shipmentInfo, labelContent);
                labelContent = prepareFormCodeWithBox(shipmentInfo, ursaPayload, labelContent);
                labelContent = prepareServiceDetails(shipmentInfo, ursaPayload, labelContent);
                labelContent = prepareMasterNumber(shipmentInfo, ursaPayload, labelContent);
                labelContent = prepareRoutingAndBarCode(shipmentInfo, ursaPayload, labelContent);
                labelContent = prepareStubDetails(shipmentInfo, ursaPayload, labelContent);

                log.debug(String.format(STANDARD_FIELD_INFO, "CustomLabel", labelContent));
            } else {
                log.error(STANDARD_ERROR, msg, "Label not defined for the mode " + shipmentInfo.getTransitDetails().getTransitMode());
                throw new ShipmentMessageException(CODE_LABEL_NOT_DEFINED, "Shipment Label template is not available", ExceptionConstants.CODE_LABEL_SOURCE);
            }
            log.debug(String.format(STANDARD_FIELD_INFO, "CustomLabel-Updated", labelContent));

            // Encode to BASE 64 before sending to topic
            return Base64.getEncoder().encodeToString(labelContent.getBytes());
        } catch (ShipmentMessageException e) {
            throw e;
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw new ShipmentMessageException(CODE_LABEL_NOT_GENERATED, "Technical Error occurred during Label generation" + e.getMessage(), ExceptionConstants.CODE_LABEL_SOURCE);
        }
    }

    public static HubInfo getHubInfoIfPMDelivery(EntityPayload entityProfile, ShipmentInfo shipmentInfo, String carrierCode) {
        if(StringUtils.isEmpty(shipmentInfo.getShipmentHeader().getDestination().getEntityCode())){
            return null;
        }
        DCRoute routeInfo = ShipmentCreateHelper.getDCRoutingInfoForStoreEntityCode(
                carrierCode, entityProfile, shipmentInfo.getShipmentHeader().getDestination().getEntityCode());
        if(routeInfo!=null && routeInfo.getTerritory().equalsIgnoreCase(Constants.TERRITORY_PM)){
            return routeInfo.getHubInfo();
        }
        return null;
    }

    /**
     * Checks if shipment is SFS and if feature flag is enabled. If SFS shipment, return null EntityPayload so that return location from shipment request is used.
     * If shipment is originated from DC, retrieve Return Center Entity Profile from Cache
     */
    private EntityPayload getEntityForReturnCentre(ShipmentInfo shipmentInfo) throws ShipmentMessageException {
        String method = "CustomLabelService.getEntityForReturnCentre";
        EntityPayload entityProfile = null;

        String originEntityCd = shipmentInfo.getShipmentHeader().getOrigin().getEntityCode();
        if (featureConfig.isSfsEnabled && StringUtils.isNumeric(originEntityCd)) {
            log.debug(String.format(STANDARD_INFO, method, "Ship From Store Fedex Shipment", "Mapping Return Location from shipment request"));
            return null;
        } else {
            if (OrderType.isEcommOrder(shipmentInfo.getOrderDetails().getOrderType())) {
                if (CustomLabelHelper.isInternational(shipmentInfo)) {
                    entityProfile = getReturnCentre(internationalEntityCode);
                } else {
                    entityProfile = getReturnCentre(domesticEntityCode);
                }
            }
        }
        return entityProfile;
    }

    private EntityPayload getReturnCentre(String entityCode) throws ShipmentMessageException {
        EntityPayload entityProfile = redisRehydrateService.getEntityByCode(entityCode);
        if (ObjectUtils.isEmpty(entityProfile))
            throw new ShipmentMessageException(CODE_RC_PROFILE_NOT_AVAILABLE, "No Return Centre Entity from Redis Cache",
                    " entityCode # " + entityCode);
        log.debug(String.format(STANDARD_FIELD_INFO, "Entity " + entityCode, entityProfile));
        return entityProfile;
    }

    private String prepareStubDetails(ShipmentInfo shipmentInfo, UrsaPayload ursaPayload, String labelContent) {
        labelContent = labelContent
                .replace("@dest_entity_code", toUpper(shipmentInfo.getShipmentHeader().getDestination().getEntityCode()))
                .replace("@to_contact_storename", toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContact()))
                .replace("@to_address_1", toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription1()))
                .replace("@to_address_2", toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription2()))
                .replace("@to_store_city", toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCity()))
                .replace("@to_store_state", toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getState()))
                .replace("@to_store_zip", shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode())
                .replace("@front_back_house", toUpper(shipmentInfo.getTransitDetails().getFrontBackOfHouse()))
                .replace("@order_type", toUpper(shipmentInfo.getOrderDetails().getOrderType()))
                .replace("@lulu_stub_readable_tracking_number", toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_10))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_10) : STRING_EMPTY))
                .replace("@lulu_stub_tracking_number_barcode_value", createShipperRefLPNBarcode(toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_10))
                                                ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_10) : STRING_EMPTY)))
                .replace("@lulu_stub_carton_number_barcode_value", getDefaultWithValue(shipmentInfo.getOrderDetails().getReferenceLPN()))
                .replace("@lulu_stub_readable_carton_number", getDefaultWithValue(shipmentInfo.getOrderDetails().getReferenceLPN()));
        return labelContent;
    }

    private String prepareRoutingAndBarCode(ShipmentInfo shipmentInfo, UrsaPayload ursaPayload, String labelContent) {
        labelContent = labelContent
                .replace("@ursa_routing",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_5))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_5) : STRING_EMPTY))
                // .replace("@ursa_routing", STRING_EMPTY)//TBD - Waiting for Business Confirmation
                .replace("@barcode_formated", CustomLabelHelper
                        .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_18))
                                ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_18) : STRING_EMPTY))
                .replace("@fedex_barcode",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_7))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_7) : STRING_EMPTY))
                .replace("@uvsdk_version",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_8))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_8) : STRING_EMPTY))
                .replace("@ge_uvsdkversion",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_C1))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_C1) : STRING_EMPTY))

                .replace("@gs1_barcode",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_F2))
                                        ? formatUSPSLabelBarcode(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_F2)) : STRING_EMPTY))
                .replace("@gs1_number",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_F3))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_F3) : STRING_EMPTY))
        ;
        return labelContent;
    }

    private String prepareMasterNumber(ShipmentInfo shipmentInfo, UrsaPayload ursaPayload, String labelContent) {
        labelContent = labelContent
                .replace("@master_num",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_4))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_4) : STRING_EMPTY))
                .replace("@master_form_code_and_box",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_11))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_11) : STRING_EMPTY))
                .replace("@smallmbox",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_11))
                                        ? LABEL_GB90_40_5 : STRING_EMPTY));
        return labelContent;
    }

    private String prepareServiceDetails(ShipmentInfo shipmentInfo, UrsaPayload ursaPayload, String labelContent) {
        labelContent = labelContent
                .replace("@servicelevel",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_12))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_12) : STRING_EMPTY))
                .replace("@productname",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_13))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_13) : STRING_EMPTY))
                .replace("@specialhandling",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_14))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_14) : STRING_EMPTY))
                .replace("@destpostalcode", CustomLabelHelper
                        .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_15))
                                ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_15) : STRING_EMPTY))
                .replace("@statecountry",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_16))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_16) : STRING_EMPTY))
                .replace("@destairport",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_17))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_17) : STRING_EMPTY));
        return labelContent;
    }

    private String prepareFormCodeWithBox(ShipmentInfo shipmentInfo, UrsaPayload ursaPayload, String labelContent) {
        labelContent = labelContent
                .replace("@form_code",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_3))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_3) : STRING_EMPTY))
                .replace("@smallbox",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_LBL_FIELD_11))
                                        ? LABEL_GB90_40_5 : STRING_EMPTY))  //TBD
                .replace("@piece_num_desc",
                        String.valueOf(shipmentInfo.getShipmentPieces().size()));//check if needed only for multiple shipment
        return labelContent;
    }

    private String prepareTrackingNumberAndTag(ShipmentInfo shipmentInfo, String labelContent) {
        String trackingNo = shipmentInfo.getTransitDetails().getTrackingNo();
        labelContent = labelContent
                .replace("@TRK_NUMBER_TAG", LABEL_TRK_NUMBER_TAG)
                .replace("@tracking", trackingNo.replaceAll("([0-9]{4})", "$1 ").trim());
        return labelContent;
    }

    private String prepareDeliveryAddressBarCode(ShipmentInfo shipmentInfo, UrsaPayload ursaPayload, String labelContent) {
        labelContent = labelContent
                .replace("@delivery_address_barcode", shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription1())
                .replace("@pdf_417", CustomLabelHelper
                        .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_SHIPMENT_2D_BARCODE))
                                ? replaceControlChar(ursaPayload.getOut().get((long) FDXPSP_O_SHIPMENT_2D_BARCODE)) : STRING_EMPTY))
                .replace("@tclpn_id", getDefaultWithValue(shipmentInfo.getOrderDetails().getTclpnid()))
                .replace("@tclpnid_barcode", getDefaultWithValue(shipmentInfo.getOrderDetails().getTclpnid()))
                .replace("@ge_trknumber_tag", (!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_D1)))
                        ? ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_D1) : STRING_EMPTY)
                .replace("@ge_barcodeformatted", CustomLabelHelper
                        .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_D2))
                                ? ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_D2) : STRING_EMPTY))
                .replace("@ge_uvsdkbarcode",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_D3))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_D3) : STRING_EMPTY))
                .replace("@endorsement",
                        CustomLabelHelper
                                .toUpper(!ObjectUtils.isEmpty(ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_E1))
                                        ? ursaPayload.getOut().get((long) FDXPSP_O_FGELBL_FIELD_E1) : STRING_EMPTY));
        return labelContent;
    }

    private String prepareServiceIdentifier(ShipmentInfo shipmentInfo, String labelContent) throws ShipmentMessageException {
        FedExServiceInfo fedExServiceInfo = CustomLabelHelper.getServiceDetailsByMode(fedExModeConfig,
                shipmentInfo.getTransitDetails().getTransitMode());
        labelContent = labelContent
                .replace("@service_name",
                        fedExServiceInfo.getServiceClass())
                .replace("@service_letter",
                        CustomLabelHelper.
                                toUpper(fedExServiceInfo.getServiceLetter()));
        return labelContent;
    }

    private String prepareToAddress(ShipmentInfo shipmentInfo, String labelContent) {
        labelContent = labelContent
                .replace("@to_contact1",
                        CustomLabelHelper
                                .toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContact()))
                .replace("@to_contact2", STRING_EMPTY)
                .replace("@to_desc1",
                        CustomLabelHelper
                                .toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription1()))
                .replace("@to_desc2",
                        CustomLabelHelper
                                .toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription2()))
                .replace("@to_city",
                        CustomLabelHelper.toUpper(
                                shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCity()))
                .replace("@to_state",
                        CustomLabelHelper
                                .toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getState()))
                .replace("@to_zip",
                        CustomLabelHelper
                                .toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode()))
                .replace("@to_phone", CustomLabelHelper.formatPhoneNbr(
                        shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContactPhone()))
                .replace("@to_cntry_code",
                        CustomLabelHelper.toUpper(
                                shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry()))
                .replace("@inv",
                        CustomLabelHelper.getINV(shipmentInfo))
                .replace("@order_id",
                        CustomLabelHelper.toUpper(
                                shipmentInfo.getOrderDetails().getOrderId()))
                .replace("@po",
                        CustomLabelHelper.getPO(shipmentInfo));
        return labelContent;
    }

    private String prepareToAddressFromHubInfo(ShipmentInfo shipmentInfo, HubInfo hubInfo, String labelContent) {
        labelContent = labelContent
                .replace("@to_contact1",
                        CustomLabelHelper
                                .toUpper(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContact()))
                .replace("@to_contact2", STRING_EMPTY)
                .replace("@to_desc1", toUpper(hubInfo.getAddress1()))
                .replace("@to_desc2", toUpper(hubInfo.getAddress2()))
                .replace("@to_city", toUpper(hubInfo.getCity()))
                .replace("@to_state", toUpper(hubInfo.getState()))
                .replace("@to_zip", toUpper(hubInfo.getZipcode()))
                .replace("@to_phone", CustomLabelHelper.formatPhoneNbr(
                        shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContactPhone()))
                .replace("@to_cntry_code", toUpper(hubInfo.getCountryCode()))
                .replace("@inv", getINV(shipmentInfo))
                .replace("@order_id", toUpper(shipmentInfo.getOrderDetails().getOrderId()))
                .replace("@po", getPO(shipmentInfo));
        return labelContent;
    }

    private String prepareUpperRight(ShipmentInfo shipmentInfo, String labelContent, Map<String, String> accountDetailMap) {
        MeasureItem weightDetails = shipmentInfo.getShipmentPieces().get(0).getWeightDetails();
        String roundedWeight = String.valueOf(new DecimalFormat(DECIMAL_FORMAT).format(weightDetails.getValue()));
        StringBuilder cadMeterClient = new StringBuilder(accountDetailMap.get(METER_NO))
                .append(Constants.FORWARD_SLASH).append(clientProductId)
                .append(clientProductVersion);
        labelContent = labelContent
                .replace("@weight_dec",
                        String.valueOf(roundedWeight))
                .replace("@weight_unit",
                        CustomLabelHelper.toUpper(weightDetails.getUom()))
                .replace("@cad_meter_number_string",
                        CustomLabelHelper.toUpper(cadMeterClient.toString()))
                .replace("@bill_pay_type",
                        "Bill Sender"); //TBD
        return labelContent;
    }

    private String preparePlannedShipDate(EntityPayload entityProfile, ShipmentInfo shipmentInfo, String labelContent) {
        final String plannedShipDate = DateUtil.formatEpochToStringWithFormat(
                shipmentInfo.getTransitDetails().getDateDetails().getPlannedShipDate(), entityProfile.getTimezone(),
                LABEL_DDMMMYY_FORMAT);
        labelContent = labelContent.replace("@ship_date", plannedShipDate);
        return labelContent;
    }

    private String prepareFromAddress(ShipmentInfo shipmentInfo, String labelContent) {
        if (!ObjectUtils.isEmpty(shipmentInfo.getShipmentHeader().getReturnLocation()) && !ObjectUtils.isEmpty(shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress())) {
            labelContent = labelContent
                    .replace("@return_contact1", CustomLabelHelper
                            .toUpper(shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getContact()))
                    .replace("@return_contact2",
                            STRING_EMPTY)
                    .replace("@return_desc1",
                            CustomLabelHelper.toUpper(
                                    shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getDescription1()))
                    .replace("@return_desc2",
                            CustomLabelHelper.toUpper(
                                    shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getDescription2()))
                    .replace("@return_city",
                            CustomLabelHelper
                                    .toUpper(shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getCity()))
                    .replace("@return_state",
                            CustomLabelHelper
                                    .toUpper(shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getState()))
                    .replace("@return_zipcode", shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getZipCode())
                    .replace("@from_phone", CustomLabelHelper.formatPhoneNbr(
                            shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getContactPhone()))
                    .replace("@from_country", CustomLabelHelper.toUpper(shipmentInfo.getShipmentHeader().getReturnLocation().getReturnAddress().getCountry()));

        }
        return labelContent;
    }

    private String prepareReturnAddress(EntityPayload entityPayload, String labelContent) {
        labelContent = labelContent
                .replace("@return_contact1", CustomLabelHelper
                        .toUpper(entityPayload.getName()))
                .replace("@return_contact2", STRING_EMPTY)
                .replace("@return_desc1",
                        CustomLabelHelper.toUpper(entityPayload.getAddress1()))
                .replace("@return_desc2",
                        CustomLabelHelper.toUpper(entityPayload.getAddress2()))
                .replace("@return_city",
                        CustomLabelHelper.toUpper(entityPayload.getCity()))
                .replace("@return_state",
                        CustomLabelHelper.toUpper(entityPayload.getState()))
                .replace("@return_zipcode", entityPayload.getZipcode())
                .replace("@from_phone", CustomLabelHelper.formatPhoneNbr(entityPayload.getPhoneNo()))
                .replace("@from_country", CustomLabelHelper.toUpper(entityPayload.getCountrycode()));
        return labelContent;
    }

    /**
     * Finds UTF-8 Control Characters in URSA 2D Barcode Payload
     * Replaces with Hex value equivalent, readable by ZPL Minisoft Converter (XML input)
     *
     * @param barcode String value of FEDEX URSA 2D barcode
     * @return String value of revised 2D barcode
     */
    private String replaceControlChar(String barcode) {
        for (String controlChar : controlCharList) {
            String unicodeRegex = new StringBuilder(UNICODE_PREFIX).append(controlChar).toString();
            String hexReplace = new StringBuilder(UNDERSCORE).append(controlChar).toString();
            barcode = barcode.replaceAll(unicodeRegex, hexReplace);
        }
        return barcode;
    }

    private void updateReturnAddressInShipRequest(EntityPayload entityPayload, LocationItem retLocation) {
        if (null != retLocation) {
            retLocation.setDescription1(entityPayload.getAddress1());
            retLocation.setDescription2(entityPayload.getAddress2());
            retLocation.setCity(entityPayload.getCity());
            retLocation.setState(entityPayload.getState());
            retLocation.setZipCode(entityPayload.getZipcode());
            retLocation.setCountry(entityPayload.getCountrycode());
            retLocation.setContactPhone(entityPayload.getPhoneNo());
        }
        log.debug("Updated ReturnAddress in ShipRequest : {}", retLocation);
    }

    /**
     * URSA Output gives with "~1" as part of USPS barcode value for the field FDXPSP_O_FGELBL_FIELD_F2.
     * <p>Per Fedex URSA Requirement: Because barcode F3 is GS1-128 and thus requires some FNC1 characters embedded within it,
     * these must be escaped within the string as they are not an expressible ASCII character. So they will be expressed as ~1.
     * Any time a ~ is seen, if followed by a 1 it should be translated to a FNC1 GS1 character. </p>
     * <p>In ZPL Text, ">8" represents the FNC1 GS1 Character</p>
     * @param barCodeValue ursa output map FDXPSP_O_FGELBL_FIELD_F2 String Value
     */
    private String formatUSPSLabelBarcode(String barCodeValue) {
        return barCodeValue.replace("~1", ">8");
    }
}

