package com.example.shardedsagawallet.repositories;

import com.example.shardedsagawallet.entities.SagaStepEntity;
import com.example.shardedsagawallet.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.shardedsagawallet.enums.SagaStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStepEntity,Long> {
    List<SagaStepEntity> findBySagaInstanceId(Long sagaInstanceId);

//    List<SagaStepEntity> findBySagaInstanceIdAndStatus(Long sagaInstanceId, StepStatus status);

    Optional<SagaStepEntity> findBySagaInstanceIdAndStepNameAndStatus(Long sagaInstanceId, String stepName, StepStatus status);

    @Query("SELECT s FROM SagaStepEntity s WHERE s.sagaInstanceId = :sagaInstanceId AND s.status = :status")
    List<SagaStepEntity> findStepsBySagaInstanceIdAndStatus(
            @Param("sagaInstanceId") Long sagaInstanceId,
            @Param("status") StepStatus status
    );
}
