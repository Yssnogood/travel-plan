package com.travelplan.payment.controller;

import com.travelplan.payment.dto.CreatePaymentRequest;
import com.travelplan.payment.dto.PaymentDto;
import com.travelplan.payment.dto.RefundRequest;
import com.travelplan.payment.entity.Payment;
import com.travelplan.payment.service.PaymentService;
import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.shared.dto.UserContext;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private UserContext regularUser;
    private PaymentDto paymentDto;

    @BeforeEach
    void setUp() {
        regularUser = UserContext.builder().userId(1L).role("USER").build();
        paymentDto = PaymentDto.builder()
                .id(10L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .status(Payment.PaymentStatus.PENDING)
                .build();
    }

    @Test
    void getAllPayments_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentDto> page = new PageImpl<>(List.of(paymentDto), pageable, 1);
        when(paymentService.getAllPayments(1L, Payment.PaymentStatus.PENDING, null, null, pageable)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<PaymentDto>>> response = paymentController.getAllPayments(
                1L, Payment.PaymentStatus.PENDING, null, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPageInfo()).isNotNull();
    }

    @Test
    void getMyPayments_returnsCurrentUserPayments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentDto> page = new PageImpl<>(List.of(paymentDto), pageable, 1);
        when(paymentService.getUserPayments(1L, pageable)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<PaymentDto>>> response = paymentController.getMyPayments(regularUser, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).getUserPayments(1L, pageable);
    }

    @Test
    void getPaymentById_returnsPayment() {
        when(paymentService.getPaymentById(10L, 1L, false)).thenReturn(paymentDto);

        ResponseEntity<ApiResponse<PaymentDto>> response = paymentController.getPaymentById(10L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getId()).isEqualTo(10L);
    }

    @Test
    void getPaymentsByTravelId_returnsList() {
        when(paymentService.getPaymentsByTravelId(100L)).thenReturn(List.of(paymentDto));

        ResponseEntity<ApiResponse<List<PaymentDto>>> response = paymentController.getPaymentsByTravelId(100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void createPayment_returnsCreated() {
        CreatePaymentRequest request = CreatePaymentRequest.builder().amount(new BigDecimal("50.00")).build();
        when(paymentService.createPayment(request, 1L)).thenReturn(paymentDto);

        ResponseEntity<ApiResponse<PaymentDto>> response = paymentController.createPayment(request, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("created");
    }

    @Test
    void processPayment_returnsOk() {
        PaymentDto processed = PaymentDto.builder().id(10L).status(Payment.PaymentStatus.COMPLETED).build();
        when(paymentService.processPayment(10L, 1L)).thenReturn(processed);

        ResponseEntity<ApiResponse<PaymentDto>> response = paymentController.processPayment(10L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("processed");
    }

    @Test
    void cancelPayment_returnsOk() {
        PaymentDto cancelled = PaymentDto.builder().id(10L).status(Payment.PaymentStatus.CANCELLED).build();
        when(paymentService.cancelPayment(10L, 1L, false)).thenReturn(cancelled);

        ResponseEntity<ApiResponse<PaymentDto>> response = paymentController.cancelPayment(10L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("cancelled");
    }

    @Test
    void refundPayment_returnsCreated() {
        RefundRequest request = RefundRequest.builder().amount(new BigDecimal("10.00")).reason("test").build();
        PaymentDto.RefundDto refundDto = PaymentDto.RefundDto.builder()
                .id(1L)
                .amount(new BigDecimal("10.00"))
                .status("COMPLETED")
                .processedAt(LocalDateTime.now())
                .build();
        when(paymentService.createRefund(10L, request, 1L, false)).thenReturn(refundDto);

        ResponseEntity<ApiResponse<PaymentDto.RefundDto>> response = paymentController.refundPayment(10L, request, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Refund");
    }

    @Test
    void getPaymentStats_returnsStats() {
        PaymentService.PaymentStats stats = PaymentService.PaymentStats.builder()
                .pendingCount(1)
                .completedCount(2)
                .failedCount(0)
                .refundedCount(1)
                .totalCompletedAmount(new BigDecimal("120.00"))
                .build();
        when(paymentService.getPaymentStats()).thenReturn(stats);

        ResponseEntity<ApiResponse<PaymentService.PaymentStats>> response = paymentController.getPaymentStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getCompletedCount()).isEqualTo(2L);
    }
}
