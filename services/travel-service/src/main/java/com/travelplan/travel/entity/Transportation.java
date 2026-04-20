package com.travelplan.travel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transportations", schema = "travel_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transportation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private TransportType type;

    @Column(name = "departure_location", length = 255)
    private String departureLocation;

    @Column(name = "arrival_location", length = 255)
    private String arrivalLocation;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(length = 100)
    private String carrier;

    @Column(name = "booking_reference", length = 100)
    private String bookingReference;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TransportType {
        FLIGHT,
        TRAIN,
        BUS,
        CAR_RENTAL,
        TAXI,
        FERRY,
        OTHER
    }

    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        CANCELLED
    }
}
