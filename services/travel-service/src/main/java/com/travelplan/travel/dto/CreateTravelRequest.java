package com.travelplan.travel.dto;

import com.travelplan.travel.entity.Travel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTravelRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @DecimalMin(value = "0.0", message = "Budget must be positive")
    private BigDecimal totalBudget;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency = "EUR";

    private List<CreateDestinationRequest> destinations;
    private List<CreateAccommodationRequest> accommodations;
    private List<CreateTransportationRequest> transportations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateDestinationRequest {
        @NotNull(message = "Destination ID is required")
        private Long destinationId;
        private LocalDate arrivalDate;
        private LocalDate departureDate;
        private Integer visitOrder;
        private String notes;
        private List<CreateActivityRequest> activities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateActivityRequest {
        @NotNull(message = "Activity ID is required")
        private Long activityId;
        private LocalDate plannedDate;
        private String plannedTime;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateAccommodationRequest {
        @NotBlank(message = "Accommodation name is required")
        private String name;
        private String type;
        private String address;
        @NotNull(message = "Check-in date is required")
        private LocalDate checkInDate;
        @NotNull(message = "Check-out date is required")
        private LocalDate checkOutDate;
        private BigDecimal pricePerNight;
        private String currency;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateTransportationRequest {
        @NotBlank(message = "Transport type is required")
        private String type;
        private String departureLocation;
        private String arrivalLocation;
        @NotNull(message = "Departure time is required")
        private LocalDateTime departureTime;
        @NotNull(message = "Arrival time is required")
        private LocalDateTime arrivalTime;
        private String carrier;
        private BigDecimal price;
        private String currency;
        private String notes;
    }
}
