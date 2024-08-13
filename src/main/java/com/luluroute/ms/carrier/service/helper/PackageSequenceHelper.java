package com.luluroute.ms.carrier.service.helper;

import com.luluroute.ms.carrier.config.RedisConfig;
import com.luluroute.ms.carrier.fedex.dto.SequenceDTO;
import com.luluroute.ms.carrier.model.Sequence;
import com.luluroute.ms.carrier.repository.SequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageSequenceHelper {

    @Autowired
    private RedisTemplate<String, Long> packageSeqRedisTemplate;

    @Autowired
    private RedisConfig redisConfig;

    @Autowired
    private SequenceRepository sequenceRepository;

    public long getPackageSeqByAccountNo(String accountNo) {
        String key = redisConfig.seqKeyPrefix + accountNo;
        Long sequence = packageSeqRedisTemplate.opsForValue().get(key);
        if (sequence != null) {
            return incrementCounter(key, sequence);
        } else {
            return getPackSeqFromDb(key);
        }
    }

    private long getPackSeqFromDb(String key) {
        Sequence sequenceByKey = sequenceRepository.findSequenceByKey(key);
        if (sequenceByKey == null) {
            return createNewPackageSeq(key);
        } else {
            long dbSeq = sequenceByKey.getSequence();
            log.info("Saving sequence for key, {}, from database into cache", key);
            packageSeqRedisTemplate.opsForValue().set(key, dbSeq);
            return incrementCounter(key, dbSeq);
        }
    }

    private long createNewPackageSeq(String key) {
        log.info("Sequence Key, {}, not Found in database, creating new entry in database.", key);
        Sequence newSeqEntity = buildNewSequenceEntity(key);
        sequenceRepository.save(newSeqEntity);
        long newSeq = newSeqEntity.getSequence();
        packageSeqRedisTemplate.opsForValue().set(key, newSeq);
        return newSeq;
    }

    private long incrementCounter(String key, Long sequence) {
        if (sequence.compareTo(9999999L) >= 0) {
            return resetCounter(key);
        } else {
            RedisAtomicLong counter = new RedisAtomicLong(key, packageSeqRedisTemplate);
            long newSeq = counter.incrementAndGet();
            log.info("Fedex Ground Economy Package Sequence for {} has been incremented to {}", key, newSeq);
            return newSeq;
        }
    }

    private long resetCounter(String key){
        log.info("Fedex Sequence for {} has reached 9999999 and was reset to {}", key, 1);
        packageSeqRedisTemplate.opsForValue().set(key, 1L);
        return 1L;
    }

    private Sequence buildNewSequenceEntity(String key) {
        SequenceDTO seqDto = SequenceDTO.builder()
                .key(key)
                .sequence(1L)
                .build();
        Sequence sequence = new Sequence();
        sequence.setKey(seqDto.getKey());
        sequence.setSequence(seqDto.getSequence());
        return sequence;
    }

}
