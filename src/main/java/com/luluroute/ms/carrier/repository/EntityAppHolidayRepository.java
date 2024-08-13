package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.model.EntityApplicableHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntityAppHolidayRepository extends JpaRepository<EntityApplicableHoliday, UUID> {

	@Query(value = """
            select * from transit.enttholidayapplicable h

            where (h.carrierid is null or :carrierId = cast(h.carrierid as varchar))
              and (h.modeid is null or :modeId = cast(h.modeid as varchar))
              and (h.country is null or :country = h.country)
              and (h.state is null or :state = h.state)
              and (h.city is null or :city = h.city)
              and h.active = 1
            """, nativeQuery = true)
	List<EntityApplicableHoliday> findApplicableHolidays(@Param("carrierId") String carrierId,
														 @Param("modeId") String modeId,
														 @Param("country") String country,
														 @Param("state") String state,
														 @Param("city") String city);
}
