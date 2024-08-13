package com.luluroute.ms.carrier.fedex.util;

import java.util.UUID;

public class Constants {

    public static final String X_JOB_START_TIME = "X-Job-Start-Time";
    public static final String X_SHIPMENT_CORRELATION_ID = "X-Shipment-Correlation-Id";
    public static final String X_MESSAGE_CORRELATION_ID = "X-Message-Correlation-Id";
    public static final String X_CORRELATION_ID = "X-Correlation-Id";
    public static final String X_CARRIER_STREAM_FUNCTION_ID = "X-Carrier-Stream-Function-Id";
    public static final String PROCESSING_SHIPMENT_REQUEST = "APP_MESSAGE=\"Processing starting for ShipmentCorrelationId {%s} Message\" | message=\"{%s}\"";
    public static final String MESSAGE = "APP_MESSAGE=\"Received Avro %s Message\" | message=\"{%s}\"";
    public static final String PROFILE_MESSAGE = "APP_MESSAGE=\"Profile %s \" | key=\"{%s}\"  | value=\"{%s}\" | ShipmentCorrelationId=\"{%s}\"";
    public static final String MESSAGE_REDIS_KEY_LOADING = "APP_MESSAGE=\"Loading %s for key %s from Redis";
    public static final String STRING_EMPTY = "";
    public static final String STANDARD_ERROR = "APP_MESSAGE=Error Occurred | METHOD=\"{}\" | ERROR=\"{}\"";
    public static final String STANDARD_DATA_ERROR = "APP_MESSAGE=Data Error Occurred | METHOD=\"{%s}\" | FIELD=\"{%s}\" | ERROR=\"{%s}\"";
    public static final String STANDARD_FIELD_INFO = "APP_MESSAGE=Field Identifier # \"{%s}\" | Value # \"{%s}\" ";
    public static final String URSA_TRANSIT_DAY_EXCEPTION_INFO = "URSAStatusException Occurred while performing transit day calculation with message: {} and stack trace: {}";
    public static final String URSA_RUNTIME_EXCEPTION_INFO = "URSA Error: %s | URSA Message: %s";
    public static final String BUILD_ARTIFACT_ROUTE_RULES = "5100";
    public static final String BUILD_ARTIFACT_LABEL_REQUIRE_RENDER = "7200";
    public static final String BUILD_ARTIFACT_LABEL_DND_REQUIRE_RENDER = "7100";
    public static final String STANDARD_INFO = "APP_MESSAGE= METHOD=\"{%s}\" | Identifier# \"{%s}\" | Value # \"{%s}\" ";

    public static final String SHIPMENT_REQUEST_CANCEL = "9989";
    public static final String FEDEX_CARRIER_CANCEL_CONSUMER = "Consumer_FEDEX_Cancel_Shipment";
    public static final String UPDATED_BY = "admin";
    public static final String BUILD_ARTIFACT_CANCEL = "9100";
    public static final String BUILD_ARTIFACT_TYPE_7900 = "7900";
    public static final String MESSAGE_PUBLISHED = "APP_MESSAGE=Message Published | Key=\"{}\" | Message=\"{}\" | Topic=\"{}\"";
    public static final String CONSUMER_STREAM_CONSUMER_GROUP = "%s_%s";
    public static final String ACCOUNT_CARRIER_MODE = "%s-%s-%s";
    public static final long BUILD_ARTIFACT_STATUS_COMPLETE = 200;
    public static final long BUILD_ARTIFACT_STATUS_ERROR = 500;
    public static final String REDIS_KEY_SHIPMENT_MESSAGE = "%s::%s";
    public static final String FEDEX_CARRIER_CONSUMER = "Consumer_Fedex_Create_Shipment";
    public static final String LABEL_PROCESS_NAME = "Carrier Fedex Process";
    public static final long LABEL_PROCESS_CODE = 600;
    public static final long PROCESS_STATUS_COMPLETED = 200;
    public static final long PROCESS_STATUS_FAILED = 500;
    public static final String KAFKA_STREAM_GROUP_ID = "Fedex_Shiment_Stream_";

    public static final String ACCOUNT_TO_BILL = "ACCOUNT_TO_BILL";
    public static final String ACCOUNT_GSN = "ACCOUNT_GSN";
    public static final String MASTER_ACCOUNT = "MASTER_ACCOUNT";
    public static final String ENTITY_TIMEZONE = "TIMEZONE";
    public static final String PACKAGE_TYPE = "PACKAGE_TYPE";
    public static final String PRODUCT_ID = "PRODUCT_ID";
    public static final String DISPATCH_ID = "DISPATCH_ID";
    public static final String METER_NO = "METER_NO";
    public static final String TRACKING_SEED_ID = "%s";

    public static final String GROUND_SHIPMENT_ACCOUNT_NO = "GROUND_SHIPMENT_ACCOUNT_NO";

    public static final String SMART_POST_ID = "SMART_POST_ID";

    public static final String SMART_POST_METER_ID = "SMART_POST_METER_ID";

    public static final String SMART_POST_HUB_ID = "SMART_POST_HUB_ID";

    public static final String TAX_ID = "TAX_ID";

    public static final String TERRITORY_PM = "PM";

    public static final String LABEL_TRACKER_ID = "%s%s";

    public static final String LABEL_REF_LPN_ID = "%s";

    public static final String LABEL_DD_MM_YYYY_FORMAT = "dd/MM/yyyy";
    public static final String LABEL_DDMMMYY_FORMAT = "ddMMMyy";
    public static final String LABEL_YYYYMMDD_FORMAT = "yyyyMMdd";
    public static final String LABEL_TRK_NUMBER_TAG = "TRK#";

    public static final String LABEL_GB90_40_5 = "^GB90,40,5";

    public static final String LABEL_CLIENT_URSA_UVSDK_VERSION = "CLIENT-URSA UVSDK VERSION";
    public static final String DECIMAL_FORMAT = "0.00";
    public static final String SATURDAY_DELIVERY = "03";
    public static final String WEEKDAY_DELIVERY = "02";
    public static final String COMMA = ",";
    public static final String INTERNATIONAL = "_I";
    public static final String CARRIER_CODE_HEADER_KEY = "CarrierCode";
    public static final String FEDEX_SUPPORT_USER = "FEDEX_SUPPORT";

    public static String getCorrelationId() {
        return UUID.randomUUID().toString();
    }

    public static String usingSubstringMethod(String text, int length) {
        if (text.length() <= length) {
            return text;
        } else {
            return text.substring(0, length);
        }
    }

    public static final String TEMP_LOCATION = System.getProperty("java.io.tmpdir");
    public static final String FORWARD_SLASH = "/";
    public static final String BACKWARD_SLASH = "\\";
    public static final String FROM_DATE_FORMAT = "yyyy-MM-dd";
    public static final String PDD_DATE_FORMAT = "yyyyMMdd";
    public static final String TRANSIT_DAYS_NOT_AVAILABLE = "00000000";
    public static final String EXCEPTIONAL_MODE_95 = "95";
    public static final String EXCEPTIONAL_MODE_92 = "92";
    public static final String GROUND_ECONOMY_MODE = "95";
    public static final String HOME_MODE = "90";
    public static final String GROUND_MODE = "92";

    public static final String SED_STATEMENT = "NO EEI 30.37 (a)";

    public static final String UNICODE_PREFIX = "\\u00";
    public static final String UNDERSCORE = "_";
    public static final String DELIMITER = "-";
    public static final String URSA_CD_ETD = "54";
    public static final String URSA_CD_EWO = "83";
    public static final String COUNTRY_CA = "CA";
    public static final String US_DOMESTIC = "US-Domestic";
    public static final String US_INTL = "US-International";
    public static final String CA_DOMESTIC = "CA-Domestic";
    public static final String URSA_SERVICE_ERROR = "FedEx service %s is not offered for the given destination address. Please try with different shipVia OR laneName";
    public static final String URSA_ZIPCODE_ERROR = "FedEx service %s is not available for the destination zip %s. Please check the zipcode associated to destination address is correct";

}
