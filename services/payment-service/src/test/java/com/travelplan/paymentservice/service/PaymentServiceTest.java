package com.travelplan.paymentservice.service;

import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.paymentservice.dto.CreatePaymentRequest;
import com.travelplan.paymentservice.dto.PaymentDto;
import com.travelplan.paymentservice.entity.Payment;
import com.travelplan.paymentservice.entity.PaymentMethod;
import com.travelplan.paymentservice.entity.PaymentStatus;
import com.travelplan.paymentservice.entity.PaymentType;
import com.travelplan.paymentservice.repository.PaymentRepository;
import com.travelplan.paymentservice.repository.PaymentMethodRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private PaymentMethod testPaymentMethod;
    private CreatePaymentRequest createRequest;

    @BeforeEach
    void setUp() {
        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setId(1L);
        testPaymentMethod.setUserId(1L);
        testPaymentMethod.setType(PaymentType.CREDIT_CARD);
        testPaymentMethod.setProvider("Visa");
        testPaymentMethod.setLastFourDigits("4242");
        testPaymentMethod.setExpiryMonth(12);
        testPaymentMethod.setExpiryYear(2025);
        testPaymentMethod.setDefault(true);
        testPaymentMethod.setActive(true);

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setUserId(1L);
        testPayment.setPaymentMethodId(1L);
        testPayment.setAmount(new BigDecimal("150.00"));
        testPayment.setCurrency("EUR");
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setTransactionId(UUID.randomUUID().toString());
        testPayment.setDescription("Travel booking payment");
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreatePaymentRequest();
        createRequest.setUserId(1L);
        createRequest.setPaymentMethodId(1L);
        createRequest.setAmount(new BigDecimal("200.00"));
        createRequest.setCurrency("EUR");
        createRequest.setDescription("New payment");
    }

    @Test
    void getAllPayments_ShouldReturnPagedPayments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(List.of(testPayment), pageable, 1);
        
        when(paymentRepository.findAll(pageable)).thenReturn(paymentPage);

        ApiResponse<List<PaymentDto>> response = paymentService.getAllPayments(0, 10, null, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getAmount()).isEqualTo(new BigDecimal("150.00"));
        verify(paymentRepository).findAll(pageable);
    }

    @Test
    void getPaymentById_WhenPaymentExists_ShouldReturnPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        ApiResponse<PaymentDto> response = paymentService.getPaymentById(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1L);
        assertThat(response.getData().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void getPaymentById_WhenPaymentNotFound_ShouldThrowException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void createPayment_ShouldCreateAndReturnPayment() {
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(2L);
            payment.setTransactionId(UUID.randomUUID().toString());
            return payment;
        });

        ApiResponse<PaymentDto> response = paymentService.createPayment(createRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.getData().getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_WithInvalidPaymentMethod_ShouldThrowException() {
        when(paymentMethodRepository.findById(999L)).thenReturn(Optional.empty());
        createRequest.setPaymentMethodId(999L);

        assertThatThrownBy(() -> paymentService.createPayment(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment method not found");
    }

    @Test
    void processPayment_ShouldUpdateStatusToProcessing() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setStatus(PaymentStatus.COMPLETED);
            return payment;
        });

        ApiResponse<PaymentDto> response = paymentService.processPayment(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void refundPayment_WhenPaymentCompleted_ShouldRefund() {
        testPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setStatus(PaymentStatus.REFUNDED);
            return payment;
        });

        ApiResponse<PaymentDto> response = paymentService.refundPayment(1L, null);

        assertThat(response.isSuccess()).isTrue();
        verify(paymentRepository).save(argThat(payment -> payment.getStatus() == PaymentStatus.REFUNDED));
    }

    @Test
    void refundPayment_WhenPaymentNotCompleted_ShouldThrowException() {
        testPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() -> paymentService.refundPayment(1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot refund a payment that is not completed");
    }

    @Test
    void cancelPayment_WhenPaymentPending_ShouldCancel() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        ApiResponse<PaymentDto> response = paymentService.cancelPayment(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(paymentRepository).save(argThat(payment -> payment.getStatus() == PaymentStatus.CANCELLED));
    }

    @Test
    void getPaymentsByUserId_ShouldReturnUserPayments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(List.of(testPayment), pageable, 1);
        
        when(paymentRepository.findByUserId(1L, pageable)).thenReturn(paymentPage);

        ApiResponse<List<PaymentDto>> response = paymentService.getAllPayments(0, 10, 1L, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        verify(paymentRepository).findByUserId(1L, pageable);
    }
}
