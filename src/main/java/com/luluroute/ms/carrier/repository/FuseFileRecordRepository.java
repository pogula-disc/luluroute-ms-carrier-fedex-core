package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.entity.FuseFileRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.luluroute.ms.carrier.repository.query.QueryConstants.FILE_RECORD_EFFECTIVE;

@Repository
public interface FuseFileRecordRepository extends JpaRepository<FuseFileRecordEntity, UUID> {
    List<FuseFileRecordEntity> findByActiveAndFileType(int active, String fileType);

    @Query(value = FILE_RECORD_EFFECTIVE)
    List<FuseFileRecordEntity> findByFileTypeAndEffDate(String fileType, LocalDate todayDate);

}
