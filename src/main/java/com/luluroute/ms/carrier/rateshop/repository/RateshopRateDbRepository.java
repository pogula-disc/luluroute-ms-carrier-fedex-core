package com.luluroute.ms.carrier.rateshop.repository;

import com.luluroute.ms.carrier.rateshop.entity.RateshopRateDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface RateshopRateDbRepository extends JpaRepository<RateshopRateDb, UUID> {

    @Query(value = """
            select *
            from fedex.rateshop_rate rate
            inner join fedex.rateshop_zone_zip_code zip
            on rate.rateshop_zone = zip.rateshop_zone
            where weight = :weight and
            rate.active = B'1' and zip.active = B'1' and
            (   ( :isMilitary and zip.rateshop_zone = '99' )
                or
                ( not :isMilitary and
                    (   not :destinationRequiresZip or
                        (starts_with(:originZipCodePrefix, origin_zip_code_prefix) and
                         starts_with(:destinationZipCodePrefix, destination_zip_code_prefix))
                    )
                    and
                    (   :destinationRequiresZip or
                        (origin_country_code = :originCountryCode and destination_country_code = :destinationCountryCode) )
                )
            );"""
            , nativeQuery = true)
    Set<RateshopRateDb> findByWeightOriginDestination(
            @Param("weight") Double weight,
            @Param("isMilitary") boolean isMilitary,
            @Param("destinationRequiresZip") boolean destinationRequiresZip,
            @Param("originZipCodePrefix") String originZipCodePrefix,
            @Param("destinationZipCodePrefix") String destinationZipCodePrefix,
            @Param("originCountryCode") String originCountryCode,
            @Param("destinationCountryCode") String destinationCountryCode);
}
