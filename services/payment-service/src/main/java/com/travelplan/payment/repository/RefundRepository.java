package com.travelplan.payment.repository;

import com.travelplan.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentId(Long paymentId);

    Optional<Refund> findByProviderRefundId(String providerRefundId);

    @Query("SELECT SUM(r.amount) FROM Refund r WHERE r.payment.id = :paymentId AND r.status = 'COMPLETED'")
    BigDecimal sumCompletedRefundsByPaymentId(@Param("paymentId") Long paymentId);
}
