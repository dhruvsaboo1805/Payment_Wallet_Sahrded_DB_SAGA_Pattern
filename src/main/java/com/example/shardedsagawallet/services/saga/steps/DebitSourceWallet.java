package com.example.shardedsagawallet.services.saga.steps;

import com.example.shardedsagawallet.entities.Wallet;
import com.example.shardedsagawallet.repositories.WalletRepository;
import com.example.shardedsagawallet.services.saga.SagaContext;
import com.example.shardedsagawallet.services.saga.SagaStep;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebitSourceWallet implements SagaStep {
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext context) {
        // step1 fetch the wallet id from context
        Long fromWalletId = context.getLong("fromWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Execution debit from source wallet {} with amount {}", fromWalletId, amount);

        // step 2 updating db with lock
        Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        context.put("originalSourceWalletBalance", wallet.getBalance());
        log.info("Wallet fetched with balance {}", wallet.getBalance());

        // step 3 now finally saving the db
        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("Wallet was saved with balance {}", wallet.getBalance());
        context.put("sourceWalletBalanceAfterDebitExecution", wallet.getBalance());

        log.info("Debit execution of source wallet step executed successfully");
        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext context) {
        // step1 fetch the wallet id from context
        Long fromWalletId = context.getLong("fromWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("compensation credit from source wallet {} with amount {}", fromWalletId, amount);

        // step 2 updating db with lock
        Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        log.info("Wallet was fetched with balance {}", wallet.getBalance());
        context.put("sourceWalletBalanceBeforeCreditCompensation", wallet.getBalance());

        // step 3 now finally saving the db
        wallet.credit(amount);
        walletRepository.save(wallet);
        log.info("Wallet saved with balance {}", wallet.getBalance());
        context.put("sourceWalletBalanceAfterCreditCompensation", wallet.getBalance());

        log.info("Credit compensation of source wallet step executed successfully");
        return true;
    }

    @Override
    public String getStepName() {
        return SagaStepFactory.SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString();
    }
}
