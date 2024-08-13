package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.entity.CustomLabel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomLabelRepository extends JpaRepository<CustomLabel, UUID> {

    @Query(value = "SELECT R.* FROM fedex.custom_label R where jsonb_exists(mode_code->'modeCode', :modeCode) " +
            "and jsonb_exists(order_type->'orderType', :orderType) and active = 1", nativeQuery = true)
    List<CustomLabel> findByModeAndOrderType(@Param("modeCode") String modeCode, @Param("orderType") String orderType);
}
