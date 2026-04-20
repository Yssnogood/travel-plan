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
public class UpdateTravelRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Travel.TravelStatus status;

    @DecimalMin(value = "0.0", message = "Budget must be positive")
    private BigDecimal totalBudget;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    private List<UpdateDestinationRequest> destinations;
    private List<UpdateAccommodationRequest> accommodations;
    private List<UpdateTransportationRequest> transportations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateDestinationRequest {
        private Long id;
        private Long destinationId;
        private LocalDate arrivalDate;
        private LocalDate departureDate;
        private Integer visitOrder;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateAccommodationRequest {
        private Long id;
        private String name;
        private String type;
        private String address;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private BigDecimal pricePerNight;
        private BigDecimal totalPrice;
        private String currency;
        private String confirmationNumber;
        private String status;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateTransportationRequest {
        private Long id;
        private String type;
        private String departureLocation;
        private String arrivalLocation;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String carrier;
        private String bookingReference;
        private BigDecimal price;
        private String currency;
        private String status;
        private String notes;
    }
}
