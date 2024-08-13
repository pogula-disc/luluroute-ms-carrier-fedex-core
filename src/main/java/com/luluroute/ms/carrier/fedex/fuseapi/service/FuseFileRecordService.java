package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.luluroute.ms.carrier.entity.FuseFileRecordEntity;
import com.luluroute.ms.carrier.repository.FuseFileRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
public class FuseFileRecordService {

    @Value("${fedex.file-record.alert.days.before.expire}")
    private long daysBeforeExpire;

    @Autowired
    private FuseFileRecordRepository fuseFileRecordRepository;

    public FuseFileRecordEntity findActiveUrsaFile() {
        log.debug("Entering FuseFileRecordService.findActiveUrsaFile | Retrieving active URSA file");
        List<FuseFileRecordEntity> activeUrsaRecords = fuseFileRecordRepository.findByActiveAndFileType(1, "URSA");
        LocalDate todayDate = LocalDate.now();
        if (activeUrsaRecords.isEmpty()) {
            List<FuseFileRecordEntity> effectiveUrsaFiles = fuseFileRecordRepository.findByFileTypeAndEffDate("URSA", todayDate);
            // set ursa file as active
            if (!effectiveUrsaFiles.isEmpty()) {
                FuseFileRecordEntity fuseFileRecordEntity = effectiveUrsaFiles.get(0);
                fuseFileRecordEntity.setActive(1);
                fuseFileRecordRepository.save(fuseFileRecordEntity);
                log.debug("Exiting FuseFileRecordService.findActiveUrsaFile | No Active URSA Files were originally found | {} was activated.", fuseFileRecordEntity.getFileName());
                return fuseFileRecordEntity;
            } else {
                log.error("No URSA File with active effective/expiration date range. Today's Date: {}", todayDate);
                return null;
            }
        } else {
            FuseFileRecordEntity activeUrsaRecord = activeUrsaRecords.get(0);
            LocalDate expDate = activeUrsaRecord.getExpirationDate();
            String fileName = activeUrsaRecord.getFileName();
            logExpiringFile(todayDate, expDate, fileName);
            log.debug("Exiting FuseFileRecordService.findActiveUrsaFile | Active URSA File, {}, was found and returned", activeUrsaRecord.getFileName());
            return activeUrsaRecord;
        }
    }

    private void logExpiringFile(LocalDate todayDate, LocalDate expDate, String fileName) {
        if (todayDate.plusDays(daysBeforeExpire).isAfter(expDate)) {
            long noOfDays = todayDate.until(expDate, ChronoUnit.DAYS);
            if (noOfDays == 0L)
                log.warn("FEDEX FUSE | Active URSA file, {}, will expire today if not actioned. | File Expiration Date: {}",
                        fileName, noOfDays, expDate);
            else
                log.warn("FEDEX FUSE | Active URSA file, {}, will expire in {} days. | File Expiration Date: {}",
                        fileName, noOfDays, expDate);
        }
    }

}
