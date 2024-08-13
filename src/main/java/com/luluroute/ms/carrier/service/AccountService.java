package com.luluroute.ms.carrier.service;

import com.logistics.luluroute.redis.shipment.carriermain.AccountDetails;
import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.carriermain.TransitModes;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.service.helper.ShipmentCreateHelper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.carrier.fedex.util.Constants.*;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_ACCOUNT_NOT_FOUND;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_NO_ARTIFACTS_SOURCE;

@Service
@Slf4j
public class AccountService {

    public Map<String, String> getAccountsDetails(String orderType,
                                                  CarrierMainPayload carrierProfile, String carrierCode,
                                                  String carrierModeCode, EntityPayload entityProfile,
                                                  String destEntityCode) throws ShipmentMessageException {
        String targetAccountType = ShipmentCreateHelper.getTargetAccountType(carrierCode, carrierModeCode, entityProfile, orderType, destEntityCode);
        log.debug(String.format("Identified Target Account Type: %s | Carrier Mode Code: %s | Destination Entity Code: %s", targetAccountType, carrierModeCode, destEntityCode));
        return selectAccount(carrierProfile, carrierModeCode, entityProfile.getEntityCode(), targetAccountType);
    }

    /**
     * Selects account based on transit mode among carrier options
     *
     * @param carrierMainPayload : Carrier Redis Payload
     * @param carrierModeCode    : Carrier Mode
     * @return : Account To Bill
     */
    private Map<String, String> selectAccount(CarrierMainPayload carrierMainPayload, String carrierModeCode,
                                              String sourceEntityCode, String targetAccountType)
            throws ShipmentMessageException {
        log.info(String.format(STANDARD_FIELD_INFO, "carrierModeCode", carrierModeCode));
        // Get the account for Mode
        Map<String, String> accountDetailMap = null;
        for (TransitModes transitModes : carrierMainPayload.getTransitModes()) {
            if (transitModes.getModeCode().equalsIgnoreCase(carrierModeCode)) {
                accountDetailMap = mapAccountsInformation(transitModes, sourceEntityCode, targetAccountType);
                //PRODUCT_ID
                accountDetailMap.put(PRODUCT_ID, transitModes.getProductId());
                //PACKAGE_TYPE
                accountDetailMap.put(PACKAGE_TYPE, transitModes.getMailType());

            }
        }
        checkMandatoryCarrierData(accountDetailMap, ACCOUNT_TO_BILL);
        return accountDetailMap;
    }

    /**
     * @param accountDetailMap
     * @param key
     * @throws ShipmentMessageException
     */
    private void checkMandatoryCarrierData(Map<String, String> accountDetailMap, String key) throws ShipmentMessageException {
        String msg = "AccountService.checkMandatoryCarrierData";
        if (!accountDetailMap.containsKey(key) || StringUtils.isEmpty(accountDetailMap.get(key)))
            throw new ShipmentMessageException(CODE_ACCOUNT_NOT_FOUND,
                    String.format(STANDARD_DATA_ERROR, msg, key, "MANDATORY : NOT AVAILABLE"), CODE_NO_ARTIFACTS_SOURCE);

    }

    /**
     * Get the account for the matching accountModeId
     * Tax Id is expected to be loaded in from the ref_1 field from the carrier account table
     *
     * @param transitModes      Carrier's transit modes which contain mode accounts
     * @param sourceEntityCode  query accounts by source entity code (ie. LAX01)
     * @param targetAccountType targeet account type (ie. RETAIL, RETAIL PM, ECOMM, etc) used to match and find account
     * @return Map of Chosen Account Details
     */
    private Map<String, String> mapAccountsInformation(TransitModes transitModes, String sourceEntityCode, String targetAccountType) throws ShipmentMessageException {
        Map<String, String> accountDetailMap = new HashMap<>();
        for (AccountDetails accountDetails : transitModes.getAccounts()) {
            if (accountDetails.getSourceEntityCode().equalsIgnoreCase(sourceEntityCode) && accountDetails.getAccountType().equalsIgnoreCase(targetAccountType)) {
                String billToAccountNo = accountDetails.getBillToAccountNo();
                accountDetailMap.put(ACCOUNT_TO_BILL, billToAccountNo);
                accountDetailMap.put(ACCOUNT_GSN, accountDetails.getAccountGsn());
                accountDetailMap.put(MASTER_ACCOUNT, accountDetails.getAccountNo());
                accountDetailMap.put(DISPATCH_ID, accountDetails.getDispatchId());
                accountDetailMap.put(METER_NO, accountDetails.getMeterNo());
                accountDetailMap.put(GROUND_SHIPMENT_ACCOUNT_NO, accountDetails.getGroundShipmentAccountNo());
                accountDetailMap.put(SMART_POST_ID, accountDetails.getSmartPostId());
                accountDetailMap.put(SMART_POST_METER_ID, accountDetails.getSmartPostMeterId());
                accountDetailMap.put(SMART_POST_HUB_ID, accountDetails.getSmartPostHubId());
                accountDetailMap.put(TAX_ID, accountDetails.getRef_1());
                log.debug(String.format("Identified Account Number: %s | Source Entity Code: %s | Account Type: %s", billToAccountNo, sourceEntityCode, targetAccountType));
                break;
            }
        }
        return accountDetailMap;
    }

}
