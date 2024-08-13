package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.entity.UrsaFileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface URSAFileStateRepository extends CrudRepository<UrsaFileEntity, UUID> {

    Optional<UrsaFileEntity> findByState(String state);
}
