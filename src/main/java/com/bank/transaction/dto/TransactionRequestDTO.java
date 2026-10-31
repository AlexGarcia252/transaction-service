package com.bank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransactionRequestDTO(
        @NotBlank(message = "La cuenta origen es obligatoria")
        String sourceAccount,

        @NotBlank(message = "La cuenta destino es obligatoria")
        String targetAccount,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal amount,

        String description
) {}