package com.travelplan.payment.controller;

import com.travelplan.payment.dto.*;
import com.travelplan.payment.service.PaymentMethodService;
import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.shared.dto.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Payment method management endpoints")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment methods", description = "Get all payment methods for the current user")
    public ResponseEntity<ApiResponse<List<PaymentMethodDto>>> getPaymentMethods(
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Fetching payment methods for user: {}", userContext.getUserId());
        List<PaymentMethodDto> methods = paymentMethodService.getUserPaymentMethods(userContext.getUserId());
        return ResponseEntity.ok(ApiResponse.success(methods));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment methods (paginated)", description = "Get all payment methods with pagination")
    public ResponseEntity<ApiResponse<Page<PaymentMethodDto>>> getPaymentMethodsPaged(
            @AuthenticationPrincipal UserContext userContext,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PaymentMethodDto> methods = paymentMethodService.getUserPaymentMethods(userContext.getUserId(), pageable);

        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .page(methods.getNumber())
                .size(methods.getSize())
                .totalElements(methods.getTotalElements())
                .totalPages(methods.getTotalPages())
                .hasNext(methods.hasNext())
                .hasPrevious(methods.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success(methods, pageInfo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment method by ID", description = "Get a specific payment method")
    public ResponseEntity<ApiResponse<PaymentMethodDto>> getPaymentMethodById(
            @Parameter(description = "Payment method ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Fetching payment method {} for user: {}", id, userContext.getUserId());
        PaymentMethodDto method = paymentMethodService.getPaymentMethodById(id, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(method));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create payment method", description = "Add a new payment method")
    public ResponseEntity<ApiResponse<PaymentMethodDto>> createPaymentMethod(
            @Valid @RequestBody CreatePaymentMethodRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Creating payment method for user: {}", userContext.getUserId());
        PaymentMethodDto method = paymentMethodService.createPaymentMethod(request, userContext.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(method, "Payment method created successfully"));
    }

    @PatchMapping("/{id}/default")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set as default", description = "Set a payment method as default")
    public ResponseEntity<ApiResponse<PaymentMethodDto>> setAsDefault(
            @Parameter(description = "Payment method ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Setting payment method {} as default for user: {}", id, userContext.getUserId());
        PaymentMethodDto method = paymentMethodService.setAsDefault(id, userContext.getUserId());
        return ResponseEntity.ok(ApiResponse.success(method, "Payment method set as default"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete payment method", description = "Remove a payment method")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(
            @Parameter(description = "Payment method ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Deleting payment method {} for user: {}", id, userContext.getUserId());
        paymentMethodService.deletePaymentMethod(id, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(null, "Payment method deleted"));
    }
}
