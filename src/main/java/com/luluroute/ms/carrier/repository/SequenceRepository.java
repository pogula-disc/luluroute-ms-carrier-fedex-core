package com.luluroute.ms.carrier.repository;

import com.luluroute.ms.carrier.model.Sequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SequenceRepository extends JpaRepository<Sequence, UUID> {

    Sequence findSequenceByKey(String key);

}
