package com.travelplan.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MethodType type;

    @Column(length = 100)
    private String provider;

    @Column(name = "last_four_digits", length = 4)
    private String lastFourDigits;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Column(name = "card_brand", length = 50)
    private String cardBrand;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Column(name = "provider_token", length = 500)
    private String providerToken;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum MethodType {
        CREDIT_CARD,
        DEBIT_CARD,
        PAYPAL,
        STRIPE,
        BANK_TRANSFER
    }
}
