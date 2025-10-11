package com.example.shardedsagawallet.services.saga;

import com.example.shardedsagawallet.entities.Transaction;
import com.example.shardedsagawallet.repositories.TransactionRepository;
import com.example.shardedsagawallet.services.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaTransferService {
    private final TransactionService transactionService;
    private final SagaOrchastration sagaOrchastration;

    public Long initiateTransfer(Long  fromWalletId, Long toWalletId, BigDecimal amount, String description) throws JsonProcessingException {
        log.info("Initiating transfer from wallet {} to wallet {} with amount {} and description {}", fromWalletId, toWalletId, amount, description);
        Transaction transaction = transactionService.createTransaction(fromWalletId, toWalletId, amount, description);

        SagaContext sagaContext = SagaContext.builder()
                .data(Map.ofEntries(
                        Map.entry("transactionId", transaction.getId()),
                        Map.entry("fromWalletId", fromWalletId),
                        Map.entry("toWalletId", toWalletId),
                        Map.entry("amount", amount),
                        Map.entry("description", description)
                ))
                .build();

        log.info("Saga context created with id {}", sagaContext.get("description"));

        Long sagaInstanceId = sagaOrchastration.startSaga(sagaContext);
        log.info("Saga instance created with id {}", sagaInstanceId);

        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);

        executeTransferSaga(sagaInstanceId);

        return sagaInstanceId;
    }

    public void executeTransferSaga(Long sagaInstanceId) throws JsonProcessingException {
        log.info("Executing transfer saga with id {}", sagaInstanceId);
        try {
            for (SagaStepFactory.SagaStepType step : SagaStepFactory.TransferMoneySagaSteps) {
                boolean success = sagaOrchastration.executeStep(sagaInstanceId, step.toString());
                if (!success) {
                    log.error("Failed to execute step {}", step.toString());
                    sagaOrchastration.failSaga(sagaInstanceId);
                    return;
                }
            }
            sagaOrchastration.completeSaga(sagaInstanceId);
            log.info("Transfer saga completed with id {}", sagaInstanceId);
        } catch (Exception e) {
            log.error("Failed to execute transfer saga with id {}", sagaInstanceId, e);
            sagaOrchastration.failSaga(sagaInstanceId);
        }
    }

}
