package com.travelplan.payment.repository;

import com.travelplan.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    List<Payment> findByTravelId(Long travelId);

    Optional<Payment> findByProviderTransactionId(String providerTransactionId);

    Optional<Payment> findByProviderPaymentIntentId(String providerPaymentIntentId);

    @Query("SELECT p FROM Payment p WHERE " +
            "(:userId IS NULL OR p.userId = :userId) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR p.createdAt <= :endDate)")
    Page<Payment> findAllWithFilters(
            @Param("userId") Long userId,
            @Param("status") Payment.PaymentStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") Payment.PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") Payment.PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedAmountByUserId(@Param("userId") Long userId);
}
