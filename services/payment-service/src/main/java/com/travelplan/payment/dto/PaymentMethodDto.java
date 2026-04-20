package com.travelplan.payment.dto;

import com.travelplan.payment.entity.PaymentMethod;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodDto {

    private Long id;
    private Long userId;
    private PaymentMethod.MethodType type;
    private String provider;
    private String lastFourDigits;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardBrand;
    private String billingEmail;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
