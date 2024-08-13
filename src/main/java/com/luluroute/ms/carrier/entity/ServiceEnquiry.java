package com.luluroute.ms.carrier.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Table(name = "service_enquiry", schema = "fedex")
@TypeDefs({
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Data
public class ServiceEnquiry {

    @Id
    @Column(name = "service_enquiry_id", unique = true, nullable = false)
    private UUID serviceEnquiryId;

    @Column(name = "message_correlation_id", length = 80, nullable = false)
    private String messageCorrelationId;

    @Column(name = "shipment_correlation_id", length = 80, nullable = false)
    private String shipmentCorrelationId;

    @Column(name = "shipment_date", nullable = false)
    private LocalDateTime shipmentDate;

    @Column(name = "account", length = 30, nullable = false)
    private String account;

    @Column(name = "master_account", length = 30, nullable = false)
    private String masterAccount;

    @Column(name = "meter", length = 40)
    private String meter;

    @Enumerated(EnumType.STRING)
    @Column(name = "carrier_service_code", length = 20)
    private CarrierServiceCode carrierServiceCode;

    @Column(name = "mode_code", length = 20)
    private String modeCode;

    @Column(name = "tracking_no", length = 30)
    private String trackingNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_type", length = 20)
    private ShipmentType shipmentType;

    @Type(type = "jsonb")
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private JsonNode requestPayload;

    @Type(type = "jsonb")
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private JsonNode responsePayload;

    @Type(type = "jsonb")
    @Column(name = "ursa_input_tags", columnDefinition = "jsonb")
    private JsonNode ursaInput;

    @Type(type = "jsonb")
    @Column(name = "ursa_output_tags", columnDefinition = "jsonb")
    private JsonNode ursaOutput;

    @Type(type = "jsonb")
    @Column(name = "customer_references", columnDefinition = "jsonb")
    private JsonNode customerReferences;

    @Column(name = "barcode_type", length = 20)
    private String barcodeType;

    @Column(name = "barcode_value", length = 20)
    private String barcodeValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", length = 20)
    private ShipmentStatus shipmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "manifest_status", length = 20)
    private ManifestStatus manifestStatus;

    @Column(name = "manifest_file_name", length = 80)
    private String manifestFileName;

    @Column(name = "gsn", length = 80)
    private String gsn;

    @Column(name = "sp_id", length = 80)
    private String spId;

    @Column(name = "spm_id", length = 80)
    private String spmId;

    @Column(name = "sp_hub_id", length = 80)
    private String spHubId;

    @Column(name = "tax_id", length = 80)
    private String taxId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by", length = 20, nullable = false)
    private String createdBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    // Constructors, getters, and setters

    // Enumerations for carrier code, shipment status, and manifest status
    public enum CarrierServiceCode {
        FDXG,
        FDXE,
        FXSP,
        FXFR;
    }

    public enum ShipmentStatus {
        CREATED,
        CANCELLED
    }

    public enum ShipmentType {
        DOMESTIC,
        INTERNATIONAL
    }

    public enum ManifestStatus {
        NOT_MANIFESTED,
        MANIFESTED,

        MANIFEST_FAILED;
    }
}
