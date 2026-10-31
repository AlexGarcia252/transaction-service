package com.bank.transaction.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction extends PanacheEntity {

    @Column(name = "source_account", nullable = false)
    public String sourceAccount;

    @Column(name = "target_account", nullable = false)
    public String targetAccount;

    @Column(nullable = false)
    public BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    public LocalDateTime transactionDate;

    @Column(nullable = false)
    public String status; // PENDING, COMPLETED, FAILED

    public String description;
}