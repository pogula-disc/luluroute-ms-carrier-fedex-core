package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.entity.ServiceEnquiry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceEnquiryRepository extends JpaRepository<ServiceEnquiry, UUID> {

    List<ServiceEnquiry> findByShipmentCorrelationId(@Param("shipmentCorrelationId") String shipmentCorrelationId);

}
