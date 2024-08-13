package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.entity.TrackingSeedEntity;
import java.util.UUID;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface TrackingSeedRepository extends JpaRepository<TrackingSeedEntity, UUID> {

    @Modifying
    @Query(value = "UPDATE FEDEX.SEEDING  SET SEEDCURRENT = ?1 WHERE METER = ?2 and ACTIVE = 1", nativeQuery = true)
    void updateCurrentTrackingSeed(Long newTrackingSeed, String meter);

    TrackingSeedEntity findByMeterAndActive(@Param("meter") String meter, @Param("active") int active);

    @Query(value = "select nextval('fedex.package_seq')", nativeQuery = true)
    Long getPackageNextValue();
}
