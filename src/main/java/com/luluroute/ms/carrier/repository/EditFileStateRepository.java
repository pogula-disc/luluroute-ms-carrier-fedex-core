package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.entity.EditFileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EditFileStateRepository extends CrudRepository<EditFileEntity, UUID> {

    Optional<EditFileEntity> findByState(String state);
}
