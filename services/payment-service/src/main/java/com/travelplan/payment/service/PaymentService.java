package com.travelplan.payment.service;

import com.travelplan.payment.dto.*;
import com.travelplan.payment.entity.*;
import com.travelplan.payment.repository.*;
import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final RefundRepository refundRepository;

    @Transactional(readOnly = true)
    public Page<PaymentDto> getAllPayments(Long userId, Payment.PaymentStatus status,
                                            LocalDateTime startDate, LocalDateTime endDate,
                                            Pageable pageable) {
        return paymentRepository.findAllWithFilters(userId, status, startDate, endDate, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getUserPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(Long id, Long userId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));

        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to this payment");
        }

        return mapToDto(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByTravelId(Long travelId) {
        return paymentRepository.findByTravelId(travelId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request, Long userId) {
        PaymentMethod paymentMethod = null;
        if (request.getPaymentMethodId() != null) {
            paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", request.getPaymentMethodId()));

            if (!paymentMethod.getUserId().equals(userId)) {
                throw new BusinessException("Invalid payment method");
            }
        }

        Payment payment = Payment.builder()
                .userId(userId)
                .travelId(request.getTravelId())
                .paymentMethod(paymentMethod)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .status(Payment.PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Created payment {} for user {}", saved.getId(), userId);
        return mapToDto(saved);
    }

    @Transactional
    public PaymentDto processPayment(Long id, Long userId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));

        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to this payment");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new BusinessException("Payment is not in pending status");
        }

        // Simulate payment processing
        // In production, this would integrate with Stripe/PayPal
        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment.setProviderPaymentIntentId("pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));

        // Simulate successful payment
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setProviderTransactionId("txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Processed payment {} - status: {}", id, saved.getStatus());
        return mapToDto(saved);
    }

    @Transactional
    public PaymentDto cancelPayment(Long id, Long userId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));

        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to this payment");
        }

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel completed payment. Use refund instead.");
        }

        if (payment.getStatus() == Payment.PaymentStatus.CANCELLED) {
            throw new BusinessException("Payment is already cancelled");
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        Payment saved = paymentRepository.save(payment);

        log.info("Cancelled payment {}", id);
        return mapToDto(saved);
    }

    @Transactional
    public PaymentDto.RefundDto createRefund(Long paymentId, RefundRequest request, Long userId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to this payment");
        }

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED &&
            payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException("Payment must be completed to process refund");
        }

        BigDecimal remainingAmount = payment.getRemainingAmount();
        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new BusinessException("Refund amount exceeds remaining amount: " + remainingAmount);
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(request.getAmount())
                .reason(request.getReason())
                .status(Refund.RefundStatus.PENDING)
                .build();

        // Simulate refund processing
        refund.setStatus(Refund.RefundStatus.COMPLETED);
        refund.setProviderRefundId("re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        refund.setProcessedAt(LocalDateTime.now());
        refund.setProcessedBy(userId);

        Refund savedRefund = refundRepository.save(refund);

        // Update payment status
        if (request.getAmount().compareTo(remainingAmount) == 0) {
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
        }
        paymentRepository.save(payment);

        log.info("Created refund {} for payment {}", savedRefund.getId(), paymentId);
        return mapRefundToDto(savedRefund);
    }

    public PaymentStats getPaymentStats() {
        return PaymentStats.builder()
                .pendingCount(paymentRepository.countByStatus(Payment.PaymentStatus.PENDING))
                .completedCount(paymentRepository.countByStatus(Payment.PaymentStatus.COMPLETED))
                .failedCount(paymentRepository.countByStatus(Payment.PaymentStatus.FAILED))
                .refundedCount(paymentRepository.countByStatus(Payment.PaymentStatus.REFUNDED))
                .totalCompletedAmount(paymentRepository.sumAmountByStatus(Payment.PaymentStatus.COMPLETED))
                .build();
    }

    private PaymentDto mapToDto(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .travelId(payment.getTravelId())
                .paymentMethodId(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getId() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .providerTransactionId(payment.getProviderTransactionId())
                .description(payment.getDescription())
                .failureReason(payment.getFailureReason())
                .paidAt(payment.getPaidAt())
                .totalRefunded(payment.getTotalRefunded())
                .remainingAmount(payment.getRemainingAmount())
                .refunds(payment.getRefunds().stream().map(this::mapRefundToDto).collect(Collectors.toList()))
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private PaymentDto.RefundDto mapRefundToDto(Refund refund) {
        return PaymentDto.RefundDto.builder()
                .id(refund.getId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus().name())
                .providerRefundId(refund.getProviderRefundId())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class PaymentStats {
        private long pendingCount;
        private long completedCount;
        private long failedCount;
        private long refundedCount;
        private BigDecimal totalCompletedAmount;
    }
}
