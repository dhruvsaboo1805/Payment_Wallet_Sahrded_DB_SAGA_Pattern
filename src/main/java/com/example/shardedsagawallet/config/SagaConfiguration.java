package com.example.shardedsagawallet.config;

import com.example.shardedsagawallet.services.saga.SagaStep;
import com.example.shardedsagawallet.services.saga.steps.CreditDestinationWalletStep;
import com.example.shardedsagawallet.services.saga.steps.DebitSourceWallet;
import com.example.shardedsagawallet.services.saga.steps.SagaStepFactory;
import com.example.shardedsagawallet.services.saga.steps.UpdateTransactionStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SagaConfiguration {

    @Bean
    public Map<String, SagaStep> sagaStepMap(
            DebitSourceWallet debitSourceWalletStep,
            CreditDestinationWalletStep creditDestinationWalletStep,
            UpdateTransactionStatus updateTransactionStatus
    ) {
        Map<String, SagaStep> map = new HashMap<>();
        map.put(SagaStepFactory.SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString(), debitSourceWalletStep);
        map.put(SagaStepFactory.SagaStepType.CREDIT_DESTINATION_WALLET_STEP.toString(), creditDestinationWalletStep);
        map.put(SagaStepFactory.SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString(), updateTransactionStatus);
        return map;
    }

}