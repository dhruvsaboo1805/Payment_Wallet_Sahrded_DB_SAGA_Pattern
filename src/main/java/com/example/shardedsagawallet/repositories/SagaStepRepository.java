package com.example.shardedsagawallet.repositories;

import com.example.shardedsagawallet.entities.SagaStepEntity;
import com.example.shardedsagawallet.services.saga.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStepEntity,Long> {
    List<SagaStepEntity> findBySagaInstanceId(Long sagaInstanceId);
    Optional<SagaStepEntity> findByNameAndSagaInstanceId(String name, Long sagaInstanceId);

    @Query("SELECT s FROM SagaStepEntity s WHERE s.sagaInstanceId = :sagaInstanceId AND s.status = 'COMPLETED'")
    List<SagaStepEntity> findCompletedStepsBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId );

    @Query("SELECT s FROM SagaStepEntity s WHERE s.sagaInstanceId = :sagaInstanceId AND s.status IN ('COMPLETED', 'COMPENSATED')")
    List<SagaStepEntity> findCompletedOrCompensatedStepsBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId );
}
