package com.banking.nationalBank.repository;

import com.banking.nationalBank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
