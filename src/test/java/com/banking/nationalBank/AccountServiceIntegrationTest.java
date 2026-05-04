package com.banking.nationalBank;

import com.banking.nationalBank.dto.*;
import com.banking.nationalBank.exception.AccountNotFoundException;
import com.banking.nationalBank.exception.InsufficientFundsException;
import com.banking.nationalBank.repository.AccountRepository;
import com.banking.nationalBank.repository.TransactionRepository;
import com.banking.nationalBank.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createAccount_withInitialDeposit_shouldReturnAccountWithBalance() {
        CreateAccountRequest request = new CreateAccountRequest("Alice", new BigDecimal("1000.00"));

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.getAccountNumber()).isNotBlank();
        assertThat(response.getAccountHolderName()).isEqualTo("Alice");
        assertThat(response.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void createAccount_withoutInitialDeposit_shouldHaveZeroBalance() {
        CreateAccountRequest request = new CreateAccountRequest("Bob", BigDecimal.ZERO);

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getAccount_existingAccount_shouldReturnAccount() {
        AccountResponse created = accountService.createAccount(new CreateAccountRequest("Carol", BigDecimal.ZERO));

        AccountResponse fetched = accountService.getAccount(created.getAccountNumber());

        assertThat(fetched.getAccountNumber()).isEqualTo(created.getAccountNumber());
        assertThat(fetched.getAccountHolderName()).isEqualTo("Carol");
    }

    @Test
    void getAccount_nonExistingAccount_shouldThrow() {
        assertThatThrownBy(() -> accountService.getAccount("0000000000000"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void credit_shouldIncreaseBalance() {
        AccountResponse account = accountService.createAccount(new CreateAccountRequest("Dave", BigDecimal.ZERO));

        AccountResponse updated = accountService.credit(account.getAccountNumber(),
                new CreditDebitRequest(new BigDecimal("500.00"), "Salary"));

        assertThat(updated.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void credit_shouldRecordTransaction() {
        AccountResponse account = accountService.createAccount(new CreateAccountRequest("Dave", BigDecimal.ZERO));
        accountService.credit(account.getAccountNumber(),
                new CreditDebitRequest(new BigDecimal("200.00"), "Bonus"));

        List<TransactionResponse> transactions = accountService.getTransactions(account.getAccountNumber());

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo("200.00");
        assertThat(transactions.get(0).getDescription()).isEqualTo("Bonus");
    }

    @Test
    void debit_withSufficientFunds_shouldDecreaseBalance() {
        AccountResponse account = accountService.createAccount(
                new CreateAccountRequest("Eve", new BigDecimal("1000.00")));

        AccountResponse updated = accountService.debit(account.getAccountNumber(),
                new CreditDebitRequest(new BigDecimal("300.00"), "Rent"));

        assertThat(updated.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void debit_withInsufficientFunds_shouldThrow() {
        AccountResponse account = accountService.createAccount(
                new CreateAccountRequest("Frank", new BigDecimal("100.00")));

        assertThatThrownBy(() -> accountService.debit(account.getAccountNumber(),
                new CreditDebitRequest(new BigDecimal("500.00"), "Withdrawal")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transfer_withSufficientFunds_shouldUpdateBothBalances() {
        AccountResponse source = accountService.createAccount(
                new CreateAccountRequest("Grace", new BigDecimal("1000.00")));
        AccountResponse dest = accountService.createAccount(
                new CreateAccountRequest("Henry", new BigDecimal("200.00")));

        accountService.transfer(new TransferRequest(
                source.getAccountNumber(), dest.getAccountNumber(),
                new BigDecimal("300.00"), "Payment"));

        assertThat(accountService.getAccount(source.getAccountNumber()).getBalance())
                .isEqualByComparingTo("700.00");
        assertThat(accountService.getAccount(dest.getAccountNumber()).getBalance())
                .isEqualByComparingTo("500.00");
    }

    @Test
    void transfer_withInsufficientFunds_shouldThrow() {
        AccountResponse source = accountService.createAccount(
                new CreateAccountRequest("Ivy", new BigDecimal("50.00")));
        AccountResponse dest = accountService.createAccount(
                new CreateAccountRequest("Jack", BigDecimal.ZERO));

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(
                source.getAccountNumber(), dest.getAccountNumber(),
                new BigDecimal("500.00"), "Payment")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transfer_toSameAccount_shouldThrow() {
        AccountResponse account = accountService.createAccount(
                new CreateAccountRequest("Kim", new BigDecimal("500.00")));

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(
                account.getAccountNumber(), account.getAccountNumber(),
                new BigDecimal("100.00"), "Self")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_shouldRecordTransactionsForBothAccounts() {
        AccountResponse source = accountService.createAccount(
                new CreateAccountRequest("Leo", new BigDecimal("1000.00")));
        AccountResponse dest = accountService.createAccount(
                new CreateAccountRequest("Mia", BigDecimal.ZERO));

        accountService.transfer(new TransferRequest(
                source.getAccountNumber(), dest.getAccountNumber(),
                new BigDecimal("400.00"), "Rent"));

        List<TransactionResponse> sourceTx = accountService.getTransactions(source.getAccountNumber());
        List<TransactionResponse> destTx = accountService.getTransactions(dest.getAccountNumber());

        // source has initial deposit tx + transfer_debit
        assertThat(sourceTx).anyMatch(t -> t.getTransactionType().name().equals("TRANSFER_DEBIT"));
        assertThat(destTx).anyMatch(t -> t.getTransactionType().name().equals("TRANSFER_CREDIT"));
    }
}
