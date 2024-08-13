package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.luluroute.ms.carrier.entity.EditFileEntity;
import com.luluroute.ms.carrier.entity.FuseFileRecordEntity;
import java.io.File;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author MANDALAKARTHIK1
 */
@Service
@Slf4j
public class BaseURSAService {

    protected File ursaFile;
    protected File editFile;

    @Autowired
    private UrsaFileService ursaFileService;

    @Autowired
    private FuseFileStateStorageService fuseFileStateStorageService;

    @Autowired
    private FuseFileRecordService fuseFileRecordService;

    @PostConstruct
    protected void loadFuseContent() {
        this.loadContent();
    }

    protected void loadContent() {
        FuseFileRecordEntity activeURSAFile = fuseFileRecordService.findActiveUrsaFile();
        Optional<EditFileEntity> currentEditFile = fuseFileStateStorageService.findCurrentEditFile();
        if (activeURSAFile != null && currentEditFile.isPresent()) {
            ursaFile = ursaFileService.getFile(activeURSAFile.getFileName(),
                    activeURSAFile.getFileLocation());
            editFile = ursaFileService.getFile(currentEditFile.get().getFileName(),
                    currentEditFile.get().getFileLocation());

        }
    }

    /**
     * Daily Scheduled Job to Refresh Context for Active FedEx Fuse File Record for all pods
     */
    @Scheduled(cron = "${fedex.file-record.scheduler.cron}")
    public void refreshUVContext() {
        log.info("Fuse File Context Refresh Scheduler Triggered");
        loadContent();
        log.info("Fuse File Context Refresh Finished");
    }

}
