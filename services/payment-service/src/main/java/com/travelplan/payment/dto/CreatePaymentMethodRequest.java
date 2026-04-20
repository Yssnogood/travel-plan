package com.travelplan.payment.dto;

import com.travelplan.payment.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentMethodRequest {

    @NotNull(message = "Payment method type is required")
    private PaymentMethod.MethodType type;

    private String provider;

    @Size(max = 4, message = "Last four digits must be 4 characters")
    private String lastFourDigits;

    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;

    @Min(value = 2024, message = "Expiry year must be valid")
    private Integer expiryYear;

    private String cardBrand;

    @Email(message = "Invalid billing email")
    private String billingEmail;

    private String providerToken;

    private Boolean isDefault = false;
}
