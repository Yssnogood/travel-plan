package com.travelplan.payment.dto;

import com.travelplan.payment.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {

    private Long id;
    private Long userId;
    private Long travelId;
    private Long paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private String providerTransactionId;
    private String description;
    private String failureReason;
    private LocalDateTime paidAt;
    private BigDecimal totalRefunded;
    private BigDecimal remainingAmount;
    private List<RefundDto> refunds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundDto {
        private Long id;
        private BigDecimal amount;
        private String reason;
        private String status;
        private String providerRefundId;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
    }
}
