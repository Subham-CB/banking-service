package com.banking.nationalBank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditDebitRequest {

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;
}
