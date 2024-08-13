package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.luluroute.ms.carrier.entity.EditFileEntity;
import com.luluroute.ms.carrier.entity.UrsaFileEntity;
import com.luluroute.ms.carrier.repository.EditFileStateRepository;
import com.luluroute.ms.carrier.repository.URSAFileStateRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FuseFileStateStorageService {


    @Autowired
    URSAFileStateRepository ursaFileStateRepository;

    @Autowired
    EditFileStateRepository editFileStateRepository;


    public Optional<EditFileEntity> findCurrentEditFile() {
        return editFileStateRepository.findByState("active");

    }


    public Optional<UrsaFileEntity> findActiveURSAFile() {
        return ursaFileStateRepository.findByState("active");
    }

}
