package com.travelplan.payment.controller;

import com.travelplan.payment.dto.*;
import com.travelplan.payment.entity.Payment;
import com.travelplan.payment.service.PaymentService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments", description = "Retrieve all payments with filters (Admin only)")
    public ResponseEntity<ApiResponse<Page<PaymentDto>>> getAllPayments(
            @Parameter(description = "Filter by user ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) Payment.PaymentStatus status,
            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching all payments with filters");
        Page<PaymentDto> payments = paymentService.getAllPayments(userId, status, startDate, endDate, pageable);

        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .page(payments.getNumber())
                .size(payments.getSize())
                .totalElements(payments.getTotalElements())
                .totalPages(payments.getTotalPages())
                .hasNext(payments.hasNext())
                .hasPrevious(payments.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success(payments, pageInfo));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my payments", description = "Retrieve all payments for the current user")
    public ResponseEntity<ApiResponse<Page<PaymentDto>>> getMyPayments(
            @AuthenticationPrincipal UserContext userContext,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching payments for user: {}", userContext.getUserId());
        Page<PaymentDto> payments = paymentService.getUserPayments(userContext.getUserId(), pageable);

        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .page(payments.getNumber())
                .size(payments.getSize())
                .totalElements(payments.getTotalElements())
                .totalPages(payments.getTotalPages())
                .hasNext(payments.hasNext())
                .hasPrevious(payments.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success(payments, pageInfo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by ID", description = "Retrieve a specific payment")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentById(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Fetching payment {} for user: {}", id, userContext.getUserId());
        PaymentDto payment = paymentService.getPaymentById(id, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/travel/{travelId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payments by travel", description = "Retrieve all payments for a travel")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getPaymentsByTravelId(
            @Parameter(description = "Travel ID") @PathVariable Long travelId) {
        log.info("Fetching payments for travel: {}", travelId);
        List<PaymentDto> payments = paymentService.getPaymentsByTravelId(travelId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create payment", description = "Create a new payment")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Creating payment for user: {}", userContext.getUserId());
        PaymentDto payment = paymentService.createPayment(request, userContext.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment created successfully"));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Process payment", description = "Process a pending payment")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Processing payment {} for user: {}", id, userContext.getUserId());
        PaymentDto payment = paymentService.processPayment(id, userContext.getUserId());
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment processed successfully"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel payment", description = "Cancel a pending payment")
    public ResponseEntity<ApiResponse<PaymentDto>> cancelPayment(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Cancelling payment {} for user: {}", id, userContext.getUserId());
        PaymentDto payment = paymentService.cancelPayment(id, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment cancelled"));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Refund payment", description = "Create a refund for a payment")
    public ResponseEntity<ApiResponse<PaymentDto.RefundDto>> refundPayment(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Creating refund for payment {} by user: {}", id, userContext.getUserId());
        PaymentDto.RefundDto refund = paymentService.createRefund(id, request, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(refund, "Refund processed successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment statistics", description = "Get payment statistics (Admin only)")
    public ResponseEntity<ApiResponse<PaymentService.PaymentStats>> getPaymentStats() {
        PaymentService.PaymentStats stats = paymentService.getPaymentStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
