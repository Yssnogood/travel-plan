package com.travelplan.travel.dto;

import com.travelplan.travel.entity.Travel;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelDto {

    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Travel.TravelStatus status;
    private BigDecimal totalBudget;
    private String currency;
    private Long createdBy;
    private int durationDays;
    private List<DestinationDto> destinations;
    private List<AccommodationDto> accommodations;
    private List<TransportationDto> transportations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DestinationDto {
        private Long id;
        private Long destinationId;
        private String name;
        private String country;
        private String city;
        private LocalDate arrivalDate;
        private LocalDate departureDate;
        private Integer visitOrder;
        private String notes;
        private List<ActivityDto> activities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityDto {
        private Long id;
        private Long activityId;
        private String name;
        private String category;
        private LocalDate plannedDate;
        private String plannedTime;
        private BigDecimal actualCost;
        private String status;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccommodationDto {
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
    public static class TransportationDto {
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
