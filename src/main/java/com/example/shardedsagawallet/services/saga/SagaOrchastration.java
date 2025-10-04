package com.example.shardedsagawallet.services.saga;

import com.example.shardedsagawallet.entities.SagaInstance;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface SagaOrchastration {
    Long startSaga(SagaContext context) throws JsonProcessingException;

    boolean executeStep(Long sagaInstanceId, String stepName) throws JsonProcessingException;

    boolean compensateStep(Long sagaInstanceId, String stepName) throws JsonProcessingException;

    SagaInstance getSagaInstance(Long sagaInstanceId);

    void compensateSaga(Long sagaInstanceId) throws JsonProcessingException;

    void failSaga(Long sagaInstanceId);

    void completeSaga(Long sagaInstanceId);
}
