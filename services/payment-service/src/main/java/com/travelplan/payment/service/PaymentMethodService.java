package com.travelplan.payment.service;

import com.travelplan.payment.dto.CreatePaymentMethodRequest;
import com.travelplan.payment.dto.PaymentMethodDto;
import com.travelplan.payment.entity.PaymentMethod;
import com.travelplan.payment.repository.PaymentMethodRepository;
import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional(readOnly = true)
    public List<PaymentMethodDto> getUserPaymentMethods(Long userId) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PaymentMethodDto> getUserPaymentMethods(Long userId, Pageable pageable) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrue(userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PaymentMethodDto getPaymentMethodById(Long id, Long userId, boolean isAdmin) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", id));

        if (!isAdmin && !paymentMethod.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to this payment method");
        }

        return mapToDto(paymentMethod);
    }

    @Transactional
    public PaymentMethodDto createPaymentMethod(CreatePaymentMethodRequest request, Long userId) {
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .userId(userId)
                .type(request.getType())
                .provider(request.getProvider())
                .lastFourDigits(request.getLastFourDigits())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cardBrand(request.getCardBrand())
                .billingEmail(request.getBillingEmail())
                .providerToken(request.getProviderToken())
                .isDefault(request.getIsDefault())
                .isActive(true)
                .build();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            paymentMethodRepository.unsetOtherDefaults(userId, 0L);
        }

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        log.info("Created payment method {} for user {}", saved.getId(), userId);
        return mapToDto(saved);
    }

    @Transactional
    public PaymentMethodDto setAsDefault(Long id, Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", id));

        if (!paymentMethod.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to this payment method");
        }

        paymentMethodRepository.unsetOtherDefaults(userId, id);
        paymentMethod.setIsDefault(true);
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        log.info("Set payment method {} as default for user {}", id, userId);
        return mapToDto(saved);
    }

    @Transactional
    public void deletePaymentMethod(Long id, Long userId, boolean isAdmin) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", id));

        if (!isAdmin && !paymentMethod.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to this payment method");
        }

        paymentMethod.setIsActive(false);
        paymentMethodRepository.save(paymentMethod);
        log.info("Deleted payment method {} for user {}", id, userId);
    }

    private PaymentMethodDto mapToDto(PaymentMethod entity) {
        return PaymentMethodDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType())
                .provider(entity.getProvider())
                .lastFourDigits(entity.getLastFourDigits())
                .expiryMonth(entity.getExpiryMonth())
                .expiryYear(entity.getExpiryYear())
                .cardBrand(entity.getCardBrand())
                .billingEmail(entity.getBillingEmail())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
