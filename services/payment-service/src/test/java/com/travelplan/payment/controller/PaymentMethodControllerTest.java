package com.travelplan.payment.controller;

import com.travelplan.payment.dto.CreatePaymentMethodRequest;
import com.travelplan.payment.dto.PaymentMethodDto;
import com.travelplan.payment.entity.PaymentMethod;
import com.travelplan.payment.service.PaymentMethodService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMethodControllerTest {

    @Mock
    private PaymentMethodService paymentMethodService;

    @InjectMocks
    private PaymentMethodController paymentMethodController;

    private UserContext regularUser;
    private PaymentMethodDto methodDto;

    @BeforeEach
    void setUp() {
        regularUser = UserContext.builder().userId(1L).role("USER").build();
        methodDto = PaymentMethodDto.builder()
                .id(10L)
                .userId(1L)
                .type(PaymentMethod.MethodType.CREDIT_CARD)
                .provider("Visa")
                .isDefault(false)
                .isActive(true)
                .build();
    }

    @Test
    void getPaymentMethods_returnsCurrentUserMethods() {
        when(paymentMethodService.getUserPaymentMethods(1L)).thenReturn(List.of(methodDto));

        ResponseEntity<ApiResponse<List<PaymentMethodDto>>> response = paymentMethodController.getPaymentMethods(regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void getPaymentMethodsPaged_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentMethodDto> page = new PageImpl<>(List.of(methodDto), pageable, 1);
        when(paymentMethodService.getUserPaymentMethods(1L, pageable)).thenReturn(page);

        ResponseEntity<ApiResponse<Page<PaymentMethodDto>>> response = paymentMethodController.getPaymentMethodsPaged(regularUser, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPageInfo()).isNotNull();
    }

    @Test
    void getPaymentMethodById_returnsMethod() {
        when(paymentMethodService.getPaymentMethodById(10L, 1L, false)).thenReturn(methodDto);

        ResponseEntity<ApiResponse<PaymentMethodDto>> response = paymentMethodController.getPaymentMethodById(10L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getId()).isEqualTo(10L);
    }

    @Test
    void createPaymentMethod_returnsCreated() {
        CreatePaymentMethodRequest request = CreatePaymentMethodRequest.builder()
                .type(PaymentMethod.MethodType.CREDIT_CARD)
                .provider("Visa")
                .build();
        when(paymentMethodService.createPaymentMethod(request, 1L)).thenReturn(methodDto);

        ResponseEntity<ApiResponse<PaymentMethodDto>> response = paymentMethodController.createPaymentMethod(request, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("created");
    }

    @Test
    void setAsDefault_returnsOk() {
        PaymentMethodDto updated = PaymentMethodDto.builder().id(10L).isDefault(true).build();
        when(paymentMethodService.setAsDefault(10L, 1L)).thenReturn(updated);

        ResponseEntity<ApiResponse<PaymentMethodDto>> response = paymentMethodController.setAsDefault(10L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("default");
    }

    @Test
    void deletePaymentMethod_returnsOk() {
        ResponseEntity<ApiResponse<Void>> response = paymentMethodController.deletePaymentMethod(10L, regularUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("deleted");
        verify(paymentMethodService).deletePaymentMethod(10L, 1L, false);
    }
}
