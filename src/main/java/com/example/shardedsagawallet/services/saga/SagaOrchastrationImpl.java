package com.example.shardedsagawallet.services.saga;

import com.example.shardedsagawallet.entities.SagaInstance;
import com.example.shardedsagawallet.entities.SagaStepEntity;
import com.example.shardedsagawallet.enums.SagaStatus;
import com.example.shardedsagawallet.enums.StepStatus;
import com.example.shardedsagawallet.repositories.SagaInstanceRepository;
import com.example.shardedsagawallet.repositories.SagaStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SagaOrchastrationImpl implements SagaOrchastration{
    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final Map<String , SagaStep> stepsPerformer;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) throws JsonProcessingException {
        // step1 -> create and save saga instance
        SagaInstance sagaInstance = new SagaInstance();
        String contextJson = objectMapper.writeValueAsString(context);
        sagaInstance.setStatus(SagaStatus.STARTED);
        sagaInstance.setContext(contextJson);
        SagaInstance savedSagaInstance = sagaInstanceRepository.save(sagaInstance);
        log.info("Saga instance saved: {}", savedSagaInstance);

        // I can use this in better way but just for checking
        List<String> stepNames = new ArrayList<>();
        stepNames.add("DebitSourceWallet");
        stepNames.add("CreditDestinationWalletStep");
        stepNames.add("UpdateTransactionStatus");

        // step 2 -> create and save all steps from saga steps records in pending state
        for(int i = 0; i < stepNames.size(); i++) {
            SagaStep Sagastep =  stepsPerformer.get(stepNames.get(i));
            SagaStepEntity sagaStepEntity = new SagaStepEntity();
            sagaStepEntity.setStepName(Sagastep.getStepName());
            sagaStepEntity.setStepOrder(i);
            sagaStepEntity.setStatus(StepStatus.PENDING);
            sagaStepRepository.save(sagaStepEntity);
        }
        log.info("Saga instance {} created with {} steps. Starting execution.", savedSagaInstance.getId());

        return savedSagaInstance.getId();
    }

    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, String stepName) throws JsonProcessingException {
        // step 1 -> find the step to be executed from map
        SagaStep sagaStepToBeExecuted = stepsPerformer.get(stepName);
        if (sagaStepToBeExecuted == null) {
            throw new IllegalArgumentException("No executor found for step: " + stepName);
        }
        log.info("Saga step which is to executed fetched successfully");

        // step 2 -> execute the saga step using sagaStepRepository
        Optional<SagaStepEntity> step = sagaStepRepository.findByNameAndSagaInstanceId(stepName, sagaInstanceId);

        SagaInstance instance = getSagaInstance(sagaInstanceId);
        SagaContext sagaContextJson = objectMapper.readValue(instance.getContext() , SagaContext.class);
        SagaContext context = sagaContextJson;
        try {
            boolean success = sagaStepToBeExecuted.execute(context);
            if (success) {
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setStatus(StepStatus.COMPLETED));
                sagaStepRepository.save(step.get());
                instance.setCurrentStep(stepName);
                instance.setStatus(SagaStatus.RUNNING);
                log.info("step with status {} executed successfully", StepStatus.COMPLETED);
                return true;
            } else {
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setStatus(StepStatus.FAILED));
                sagaStepRepository.save(step.get());
                log.info("step with status {} failed", StepStatus.FAILED);
                return false;
            }

        } catch (Exception e) {
            if(step.isEmpty()) {
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setStatus(StepStatus.FAILED));
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setErrorMessage(e.getMessage()));
                log.info("step with status {} failed in catch block", StepStatus.FAILED);
                sagaStepRepository.save(step.get());
            }
            return false;
        }
    }
    @Override
    @Transactional
    public boolean compensateStep(Long sagaInstanceId, String stepName) throws JsonProcessingException {
        // step 1 -> find the step to be executed from map
        SagaStep sagaStepToBeCompensate = stepsPerformer.get(stepName);
        if (sagaStepToBeCompensate == null) {
            throw new IllegalArgumentException("No executor found for step: " + stepName);
        }

        log.info("Saga step which is to compensated fetched successfully");

        // step 2 -> compensate the saga step using sagaStepRepository
        Optional<SagaStepEntity> step = sagaStepRepository.findByNameAndSagaInstanceId(stepName, sagaInstanceId);

        SagaInstance instance = getSagaInstance(sagaInstanceId);
        SagaContext sagaContextJson = objectMapper.readValue(instance.getContext() , SagaContext.class);
        SagaContext context = sagaContextJson;
        try {
            boolean success = sagaStepToBeCompensate.compensate(context);
            if (success) {
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setStatus(StepStatus.COMPENSATED));
                sagaStepRepository.save(step.get());
                instance.setCurrentStep(stepName);
                instance.setStatus(SagaStatus.COMPENSATED);
                log.info("step with status {} compensated successfully", StepStatus.COMPENSATED);
                return true;
            } else {
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setStatus(StepStatus.COMPENSATED_FAILED));
                sagaStepRepository.save(step.get());
                return false;
            }

        } catch (Exception e) {
            if(step.isEmpty()) {
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setStatus(StepStatus.COMPENSATED_FAILED));
                step.ifPresent(sagaStepEntity -> sagaStepEntity.setErrorMessage(e.getMessage()));
                sagaStepRepository.save(step.get());
                log.info("step with status {} failed in catch compensated block", StepStatus.COMPENSATED_FAILED);
            }
            return false;
        }
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found with id: " + sagaInstanceId));
    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) throws JsonProcessingException {
        // step 1 take instance object mark its status as compensating
        SagaInstance instance = new SagaInstance();
        instance.setStatus(SagaStatus.COMPENSATED);
        sagaInstanceRepository.save(instance);
        log.info("Saga instance {} compensated successfully", instance.getId());

        // step 2 find list of all completed steps and make the fail or compensating steps mark as fail
        List<SagaStepEntity> completedSteps = sagaStepRepository.findCompletedStepsBySagaInstanceId(sagaInstanceId);
        Collections.reverse(completedSteps); // why this because for last which is to be executed should compensate first

        for (SagaStepEntity step : completedSteps) {
            boolean compensated = compensateStep(sagaInstanceId, step.getStepName());
            if (!compensated) {
                // If compensation fails, we have a major problem Mark the whole saga as FAILED and stop.
                failSaga(sagaInstanceId);
                return;
            }
        }

        instance.setStatus(SagaStatus.FAILED);
        sagaInstanceRepository.save(instance);
    }

    @Override
    public void failSaga(Long sagaInstanceId) throws JsonProcessingException {
        SagaInstance instance = getSagaInstance(sagaInstanceId);
        instance.setStatus(SagaStatus.FAILED);
        sagaInstanceRepository.save(instance);

        compensateSaga(sagaInstanceId);
        log.info("Saga instance {} failed", instance.getId());
    }

    @Override
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance instance = getSagaInstance(sagaInstanceId);
        instance.setStatus(SagaStatus.COMPLETED);
        sagaInstanceRepository.save(instance);
        log.info("Saga instance {} completed successfully", instance.getId());
    }
}

