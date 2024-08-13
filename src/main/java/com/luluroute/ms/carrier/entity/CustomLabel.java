package com.luluroute.ms.carrier.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "custom_label", schema = "fedex")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CustomLabel implements Serializable {

    private final static long serialVersionUID = 7702L;
    @Id
    @Column(name = "custom_label_id", unique = true, nullable = false)
    private UUID customLabelId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "label_code", nullable = false)
    private String labelCode;

    @Column(name = "label_type", nullable = false)
    private String labelType;

    @Column(name = "carrier_code", nullable = false)
    private String carrierCode;

    @Type(type = "jsonb")
    @Column(name = "mode_code", columnDefinition = "jsonb")
    private LabelModeCode modeCode;

    @Type(type = "jsonb")
    @Column(name = "order_type", columnDefinition = "jsonb")
    private LabelOrderType orderType;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "target_entity_code")
    private String targetEntityCode;

    @Column(name = "target_entity_type")
    private String targetEntityType;

    @Column(name = "content")
    private String content;


    @Column(name = "width")
    private Double width;

    @Column(name = "height")
    private Double height;

    @Column(name = "uom")
    private String uom;

    @Column(name = "format")
    private String format;

    @Column(name = "ref_1")
    private String ref1;

    @Column(name = "ref_2")
    private String ref2;

    @Column(name = "ref_3")
    private String ref3;

    @Column(name = "active", nullable = false)
    private Integer active;

    @Column(name = "created_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    @Column(name = "updated_by")
    private String updatedBy;
}
