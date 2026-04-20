package com.travelplan.travel.controller;

import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.shared.dto.UserContext;
import com.travelplan.travel.dto.*;
import com.travelplan.travel.entity.Travel;
import com.travelplan.travel.service.TravelService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/travels")
@RequiredArgsConstructor
@Tag(name = "Travels", description = "Travel management endpoints")
public class TravelController {

    private final TravelService travelService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all travels", description = "Retrieve travels with filtering and pagination")
    public ResponseEntity<ApiResponse<Page<TravelDto>>> getAllTravels(
            @Parameter(description = "Filter by user ID (admin only)")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) Travel.TravelStatus status,
            @Parameter(description = "Search term")
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserContext userContext,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // Non-admin users can only see their own travels
        Long filterUserId = userContext.isAdmin() ? userId : userContext.getUserId();
        
        log.info("Fetching travels for user: {}, status: {}, search: {}", filterUserId, status, search);
        Page<TravelDto> travels = travelService.getAllTravels(filterUserId, status, search, pageable);

        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .page(travels.getNumber())
                .size(travels.getSize())
                .totalElements(travels.getTotalElements())
                .totalPages(travels.getTotalPages())
                .hasNext(travels.hasNext())
                .hasPrevious(travels.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success(travels, pageInfo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get travel by ID", description = "Retrieve a specific travel plan")
    public ResponseEntity<ApiResponse<TravelDto>> getTravelById(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Fetching travel with id: {} for user: {}", id, userContext.getUserId());
        TravelDto travel = travelService.getTravelById(id);
        
        // Check if user owns this travel or is admin
        if (!userContext.isAdmin() && !travel.getCreatedBy().equals(userContext.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(travel));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's travels", description = "Retrieve all travels for the current user")
    public ResponseEntity<ApiResponse<Page<TravelDto>>> getMyTravels(
            @AuthenticationPrincipal UserContext userContext,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching travels for current user: {}", userContext.getUserId());
        Page<TravelDto> travels = travelService.getUserTravels(userContext.getUserId(), pageable);

        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .page(travels.getNumber())
                .size(travels.getSize())
                .totalElements(travels.getTotalElements())
                .totalPages(travels.getTotalPages())
                .hasNext(travels.hasNext())
                .hasPrevious(travels.hasPrevious())
                .build();

        return ResponseEntity.ok(ApiResponse.success(travels, pageInfo));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create travel", description = "Create a new travel plan")
    public ResponseEntity<ApiResponse<TravelDto>> createTravel(
            @Valid @RequestBody CreateTravelRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Creating travel for user: {}", userContext.getUserId());
        TravelDto travel = travelService.createTravel(request, userContext.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(travel, "Travel created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update travel", description = "Update an existing travel plan")
    public ResponseEntity<ApiResponse<TravelDto>> updateTravel(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTravelRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Updating travel with id: {} by user: {}", id, userContext.getUserId());
        TravelDto travel = travelService.updateTravel(id, request, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(travel, "Travel updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete travel", description = "Delete a travel plan")
    public ResponseEntity<ApiResponse<Void>> deleteTravel(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Deleting travel with id: {} by user: {}", id, userContext.getUserId());
        travelService.deleteTravel(id, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(null, "Travel deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update travel status", description = "Update the status of a travel plan")
    public ResponseEntity<ApiResponse<TravelDto>> updateTravelStatus(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam Travel.TravelStatus status,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Updating status of travel {} to {} by user: {}", id, status, userContext.getUserId());
        TravelDto travel = travelService.updateTravelStatus(id, status, userContext.getUserId(), userContext.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(travel, "Travel status updated"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get travel statistics", description = "Get travel count by status (Admin only)")
    public ResponseEntity<ApiResponse<TravelStats>> getTravelStats() {
        TravelStats stats = TravelStats.builder()
                .draft(travelService.countTravelsByStatus(Travel.TravelStatus.DRAFT))
                .planned(travelService.countTravelsByStatus(Travel.TravelStatus.PLANNED))
                .booked(travelService.countTravelsByStatus(Travel.TravelStatus.BOOKED))
                .inProgress(travelService.countTravelsByStatus(Travel.TravelStatus.IN_PROGRESS))
                .completed(travelService.countTravelsByStatus(Travel.TravelStatus.COMPLETED))
                .cancelled(travelService.countTravelsByStatus(Travel.TravelStatus.CANCELLED))
                .build();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @lombok.Data
    @lombok.Builder
    public static class TravelStats {
        private long draft;
        private long planned;
        private long booked;
        private long inProgress;
        private long completed;
        private long cancelled;
    }
}
