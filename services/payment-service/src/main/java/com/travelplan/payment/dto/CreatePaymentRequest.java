package com.travelplan.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    private Long travelId;

    private Long paymentMethodId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency = "EUR";

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String metadata;
}
