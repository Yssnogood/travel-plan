package com.travelplan.payment.service;

import com.travelplan.payment.dto.CreatePaymentRequest;
import com.travelplan.payment.dto.PaymentDto;
import com.travelplan.payment.dto.RefundRequest;
import com.travelplan.payment.entity.Payment;
import com.travelplan.payment.entity.PaymentMethod;
import com.travelplan.payment.entity.Refund;
import com.travelplan.payment.repository.PaymentMethodRepository;
import com.travelplan.payment.repository.PaymentRepository;
import com.travelplan.payment.repository.RefundRepository;
import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private RefundRepository refundRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;
    private PaymentMethod paymentMethod;
    private final Long userId = 1L;
    private final Long paymentId = 100L;

    @BeforeEach
    void setUp() {
        paymentMethod = PaymentMethod.builder()
                .id(10L)
                .userId(userId)
                .type(PaymentMethod.MethodType.CREDIT_CARD)
                .lastFourDigits("4242")
                .cardBrand("Visa")
                .isDefault(true)
                .isActive(true)
                .build();

        payment = Payment.builder()
                .id(paymentId)
                .userId(userId)
                .travelId(50L)
                .paymentMethod(paymentMethod)
                .amount(new BigDecimal("500.00"))
                .currency("EUR")
                .status(Payment.PaymentStatus.PENDING)
                .description("Travel payment")
                .refunds(new ArrayList<>())
                .build();
    }

    @Test
    void getAllPayments_returnsPageOfPaymentDto() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 12, 31, 23, 59);
        Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);

        when(paymentRepository.findAllWithFilters(userId, Payment.PaymentStatus.PENDING, startDate, endDate, pageable))
                .thenReturn(page);

        Page<PaymentDto> result = paymentService.getAllPayments(userId, Payment.PaymentStatus.PENDING, startDate, endDate, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(paymentRepository).findAllWithFilters(userId, Payment.PaymentStatus.PENDING, startDate, endDate, pageable);
    }

    @Test
    void getPaymentById_asOwner_returnsPaymentDto() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentDto result = paymentService.getPaymentById(paymentId, userId, false);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPaymentById_asAdmin_returnsOtherUserPayment() {
        Long adminUserId = 99L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentDto result = paymentService.getPaymentById(paymentId, adminUserId, true);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
    }

    @Test
    void getPaymentById_nonOwnerNonAdmin_throwsBusinessException() {
        Long otherUserId = 99L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getPaymentById_nonExistingId_throwsResourceNotFoundException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(999L, userId, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPayment_withPaymentMethod_returnsPaymentDto() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .travelId(50L)
                .paymentMethodId(10L)
                .amount(new BigDecimal("500.00"))
                .currency("EUR")
                .description("Travel payment")
                .build();

        when(paymentMethodRepository.findById(10L)).thenReturn(Optional.of(paymentMethod));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentDto result = paymentService.createPayment(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(paymentMethodRepository).findById(10L);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_withoutPaymentMethod_returnsPaymentDto() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .travelId(50L)
                .amount(new BigDecimal("300.00"))
                .currency("EUR")
                .build();

        Payment savedPayment = Payment.builder()
                .id(200L)
                .userId(userId)
                .travelId(50L)
                .amount(new BigDecimal("300.00"))
                .currency("EUR")
                .status(Payment.PaymentStatus.PENDING)
                .refunds(new ArrayList<>())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentDto result = paymentService.createPayment(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        verify(paymentMethodRepository, never()).findById(anyLong());
    }

    @Test
    void createPayment_invalidPaymentMethod_throwsBusinessException() {
        Long otherUserId = 99L;
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .paymentMethodId(10L)
                .amount(new BigDecimal("100.00"))
                .build();

        when(paymentMethodRepository.findById(10L)).thenReturn(Optional.of(paymentMethod));

        assertThatThrownBy(() -> paymentService.createPayment(request, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid payment method");
    }

    @Test
    void createPayment_paymentMethodNotFound_throwsResourceNotFoundException() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .paymentMethodId(999L)
                .amount(new BigDecimal("100.00"))
                .build();

        when(paymentMethodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void processPayment_pendingPayment_completesSuccessfully() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto result = paymentService.processPayment(paymentId, userId);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(result.getPaidAt()).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_nonPendingPayment_throwsBusinessException() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(paymentId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not in pending status");
    }

    @Test
    void processPayment_otherUser_throwsBusinessException() {
        Long otherUserId = 99L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(paymentId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

        @Test
        void processPayment_notFound_throwsResourceNotFoundException() {
                when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> paymentService.processPayment(paymentId, userId))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

    @Test
    void cancelPayment_pendingPayment_asOwner_cancelsSuccessfully() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto result = paymentService.cancelPayment(paymentId, userId, false);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void cancelPayment_completedPayment_throwsBusinessException() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(paymentId, userId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel completed payment");
    }

    @Test
    void cancelPayment_alreadyCancelled_throwsBusinessException() {
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(paymentId, userId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelPayment_nonOwnerNonAdmin_throwsBusinessException() {
        Long otherUserId = 99L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(paymentId, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

        @Test
        void cancelPayment_notFound_throwsResourceNotFoundException() {
                when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> paymentService.cancelPayment(paymentId, userId, false))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

    @Test
    void createRefund_completedPayment_returnsRefundDto() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        RefundRequest request = RefundRequest.builder()
                .amount(new BigDecimal("200.00"))
                .reason("Customer request")
                .build();

        Refund savedRefund = Refund.builder()
                .id(1L)
                .payment(payment)
                .amount(new BigDecimal("200.00"))
                .reason("Customer request")
                .status(Refund.RefundStatus.COMPLETED)
                .providerRefundId("re_test123")
                .processedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenReturn(savedRefund);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentDto.RefundDto result = paymentService.createRefund(paymentId, request, userId, false);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.getReason()).isEqualTo("Customer request");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(refundRepository).save(any(Refund.class));
    }

    @Test
    void createRefund_amountExceedsRemaining_throwsBusinessException() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        RefundRequest request = RefundRequest.builder()
                .amount(new BigDecimal("999.00"))
                .reason("Too much")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.createRefund(paymentId, request, userId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refund amount exceeds remaining amount");
    }

    @Test
    void createRefund_pendingPayment_throwsBusinessException() {
        RefundRequest request = RefundRequest.builder()
                .amount(new BigDecimal("100.00"))
                .reason("Nope")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.createRefund(paymentId, request, userId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be completed to process refund");
    }

    @Test
    void createRefund_nonOwnerNonAdmin_throwsBusinessException() {
        Long otherUserId = 99L;
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        RefundRequest request = RefundRequest.builder()
                .amount(new BigDecimal("100.00"))
                .reason("Test")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.createRefund(paymentId, request, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

        @Test
        void createRefund_fullAmount_marksPaymentAsRefunded() {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                RefundRequest request = RefundRequest.builder()
                                .amount(new BigDecimal("500.00"))
                                .reason("Full refund")
                                .build();

                Refund savedRefund = Refund.builder()
                                .id(2L)
                                .payment(payment)
                                .amount(new BigDecimal("500.00"))
                                .reason("Full refund")
                                .status(Refund.RefundStatus.COMPLETED)
                                .build();

                when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
                when(refundRepository.save(any(Refund.class))).thenReturn(savedRefund);
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

                PaymentDto.RefundDto result = paymentService.createRefund(paymentId, request, userId, false);

                assertThat(result.getId()).isEqualTo(2L);
                assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        }

        @Test
        void createRefund_paymentNotFound_throwsResourceNotFoundException() {
                RefundRequest request = RefundRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .reason("Missing")
                                .build();

                when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> paymentService.createRefund(paymentId, request, userId, false))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getUserPayments_returnsMappedPage() {
                Pageable pageable = PageRequest.of(0, 10);
                Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);
                when(paymentRepository.findByUserId(userId, pageable)).thenReturn(page);

                Page<PaymentDto> result = paymentService.getUserPayments(userId, pageable);

                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getId()).isEqualTo(paymentId);
        }

        @Test
        void getPaymentsByTravelId_returnsMappedList() {
                when(paymentRepository.findByTravelId(50L)).thenReturn(List.of(payment));

                List<PaymentDto> result = paymentService.getPaymentsByTravelId(50L);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTravelId()).isEqualTo(50L);
        }

        @Test
        void getPaymentStats_returnsAggregatedValues() {
                when(paymentRepository.countByStatus(Payment.PaymentStatus.PENDING)).thenReturn(1L);
                when(paymentRepository.countByStatus(Payment.PaymentStatus.COMPLETED)).thenReturn(2L);
                when(paymentRepository.countByStatus(Payment.PaymentStatus.FAILED)).thenReturn(3L);
                when(paymentRepository.countByStatus(Payment.PaymentStatus.REFUNDED)).thenReturn(4L);
                when(paymentRepository.sumAmountByStatus(Payment.PaymentStatus.COMPLETED)).thenReturn(new BigDecimal("123.45"));

                PaymentService.PaymentStats result = paymentService.getPaymentStats();

                assertThat(result.getPendingCount()).isEqualTo(1L);
                assertThat(result.getCompletedCount()).isEqualTo(2L);
                assertThat(result.getFailedCount()).isEqualTo(3L);
                assertThat(result.getRefundedCount()).isEqualTo(4L);
                assertThat(result.getTotalCompletedAmount()).isEqualByComparingTo(new BigDecimal("123.45"));
        }
}