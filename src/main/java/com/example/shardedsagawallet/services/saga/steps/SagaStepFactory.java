package com.example.shardedsagawallet.services.saga.steps;

import java.util.List;
import java.util.Map;

import com.example.shardedsagawallet.services.saga.SagaStep;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SagaStepFactory {

    private final Map<String, SagaStep> sagaStepMap;

    public enum SagaStepType {
        DEBIT_SOURCE_WALLET_STEP,
        CREDIT_DESTINATION_WALLET_STEP,
        UPDATE_TRANSACTION_STATUS_STEP
    }

    public static List<SagaStepType> TransferMoneySagaSteps = List.of(
            SagaStepFactory.SagaStepType.DEBIT_SOURCE_WALLET_STEP,
            SagaStepFactory.SagaStepType.CREDIT_DESTINATION_WALLET_STEP,
            SagaStepFactory.SagaStepType.UPDATE_TRANSACTION_STATUS_STEP
    );


    public SagaStep getSagaStep(String stepName) {
        return sagaStepMap.get(stepName);
    }

}