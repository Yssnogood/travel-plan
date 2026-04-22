package com.travelplan.payment.service;

import com.travelplan.payment.dto.CreatePaymentMethodRequest;
import com.travelplan.payment.dto.PaymentMethodDto;
import com.travelplan.payment.entity.PaymentMethod;
import com.travelplan.payment.repository.PaymentMethodRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    private PaymentMethod paymentMethod;
    private final Long userId = 1L;
    private final Long otherUserId = 99L;
    private final Long methodId = 10L;

    @BeforeEach
    void setUp() {
        paymentMethod = PaymentMethod.builder()
                .id(methodId)
                .userId(userId)
                .type(PaymentMethod.MethodType.CREDIT_CARD)
                .provider("Visa")
                .lastFourDigits("4242")
                .expiryMonth(12)
                .expiryYear(2030)
                .isDefault(false)
                .isActive(true)
                .build();
    }

    @Test
    void getUserPaymentMethods_returnsMappedList() {
        when(paymentMethodRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(paymentMethod));

        List<PaymentMethodDto> result = paymentMethodService.getUserPaymentMethods(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(methodId);
        assertThat(result.get(0).getLastFourDigits()).isEqualTo("4242");
        verify(paymentMethodRepository).findByUserIdAndIsActiveTrue(userId);
    }

    @Test
    void getUserPaymentMethodsPaged_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentMethod> page = new PageImpl<>(List.of(paymentMethod), pageable, 1);

        when(paymentMethodRepository.findByUserIdAndIsActiveTrue(userId, pageable)).thenReturn(page);

        Page<PaymentMethodDto> result = paymentMethodService.getUserPaymentMethods(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProvider()).isEqualTo("Visa");
    }

    @Test
    void getPaymentMethodById_asOwner_returnsMethod() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));

        PaymentMethodDto result = paymentMethodService.getPaymentMethodById(methodId, userId, false);

        assertThat(result.getId()).isEqualTo(methodId);
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void getPaymentMethodById_asAdmin_returnsMethod() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));

        PaymentMethodDto result = paymentMethodService.getPaymentMethodById(methodId, otherUserId, true);

        assertThat(result.getId()).isEqualTo(methodId);
    }

    @Test
    void getPaymentMethodById_notOwnerNonAdmin_throwsBusinessException() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));

        assertThatThrownBy(() -> paymentMethodService.getPaymentMethodById(methodId, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getPaymentMethodById_notFound_throwsResourceNotFoundException() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService.getPaymentMethodById(methodId, userId, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPaymentMethod_withDefault_unsetsOtherDefaults() {
        CreatePaymentMethodRequest request = CreatePaymentMethodRequest.builder()
                .type(PaymentMethod.MethodType.DEBIT_CARD)
                .provider("Mastercard")
                .lastFourDigits("1111")
                .isDefault(true)
                .build();

        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> {
            PaymentMethod saved = invocation.getArgument(0);
            saved.setId(55L);
            return saved;
        });

        PaymentMethodDto result = paymentMethodService.createPaymentMethod(request, userId);

        assertThat(result.getId()).isEqualTo(55L);
        assertThat(result.getProvider()).isEqualTo("Mastercard");
        verify(paymentMethodRepository).unsetOtherDefaults(userId, 0L);
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    void createPaymentMethod_withoutDefault_doesNotUnsetOthers() {
        CreatePaymentMethodRequest request = CreatePaymentMethodRequest.builder()
                .type(PaymentMethod.MethodType.PAYPAL)
                .provider("PayPal")
                .isDefault(false)
                .build();

        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);

        PaymentMethodDto result = paymentMethodService.createPaymentMethod(request, userId);

        assertThat(result.getId()).isEqualTo(methodId);
        verify(paymentMethodRepository, never()).unsetOtherDefaults(anyLong(), anyLong());
    }

    @Test
    void setAsDefault_asOwner_updatesMethod() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentMethodDto result = paymentMethodService.setAsDefault(methodId, userId);

        assertThat(result.getIsDefault()).isTrue();
        verify(paymentMethodRepository).unsetOtherDefaults(userId, methodId);
        verify(paymentMethodRepository).save(paymentMethod);
    }

    @Test
    void setAsDefault_notOwner_throwsBusinessException() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));

        assertThatThrownBy(() -> paymentMethodService.setAsDefault(methodId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void deletePaymentMethod_asOwner_softDeletesMethod() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));

        paymentMethodService.deletePaymentMethod(methodId, userId, false);

        assertThat(paymentMethod.getIsActive()).isFalse();
        verify(paymentMethodRepository).save(paymentMethod);
    }

    @Test
    void deletePaymentMethod_asAdmin_softDeletesMethod() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));

        paymentMethodService.deletePaymentMethod(methodId, otherUserId, true);

        assertThat(paymentMethod.getIsActive()).isFalse();
        verify(paymentMethodRepository).save(paymentMethod);
    }

    @Test
    void deletePaymentMethod_notOwnerNonAdmin_throwsBusinessException() {
        when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(paymentMethod));

        assertThatThrownBy(() -> paymentMethodService.deletePaymentMethod(methodId, otherUserId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }
}
