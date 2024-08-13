package com.luluroute.ms.carrier.fedex.util;

public class ExceptionConstants {

    public static final String CODE_NO_ARTIFACTS = "3311";
    public static final String CODE_NO_ARTIFACTS_SOURCE = "ShipmentArtifacts";

    public static final String CODE_RC_PROFILE_NOT_AVAILABLE = "3312";
    public static final String CODE_PROFILE_NOT_AVAILABLE = "3304";
    public static final String CODE_NO_DATA = "104";
    public static final String CODE_NO_SHIPMENT = "3301";
    public static final String CODE_NO_TRACKING_SEED = "3302";
    public static final String CODE_TRACKING_SEED_EXCEEDED = "3303";
    public static final String CODE_ACCOUNT_NOT_FOUND = "3305";
    public static final String CODE_LABEL_NOT_DEFINED = "3307";
    public static final String CODE_LABEL_NOT_GENERATED = "3308";
    public static final String TECHNICAL_SHIPMENT_ERROR = "Technical Error occurred during shipment creation ";
    public static final String CODE_NO_DATA_SOURCE = "ShipmentMessage";
    public static final String CODE_NO_DATA_POSTGRES_SOURCE = "Postgres";
    public static final String CODE_NO_DATA_REDIS_CACHE = "Redis";
    public static final String CODE_UNKNOWN = "100";
    public static final String CODE_UNKNOWN_SOURCE = "Unknown";
    public static final String CODE_MANIFESTED = "110";
    public static final String CODE_MANIFESTED_SOURCE = "Shipment is manifested";
    public static final String CODE_MODE_NOT_AVAILABLE = "3306";
    public static final String CODE_LABEL_SOURCE = "Custom Label Generation";
    public static final String CODE_UVSDK = "3309";
    public static final String CODE_UVSDK_SOURCE = "FedEx UVSDK";
    public static final String CODE_ARTIFACT_CONSUME = "3310";
    public static final String CODE_ARTIFACT_CONSUME_SOURCE = "ShipmentArtifacts";
    public static final String PARSER_ERROR_FORMAT = "Parsing Datetime Exception, Field:{%s} , Exception=\"{%s}\"";
    public static final String CARRIER_NAME = "FEDEX: ";
    public static final String CODE_TRANSIT_TIME = "3315";
    public static final String SOURCE_TRANSIT_TIME = "Transit Time API";
    public static final String CODE_UVSDK_SERVICE_ERROR = "3316";
    public static final String CODE_UVSDK_ZIPCODE_ERROR = "3317";
}
