package com.travelplan.payment.repository;

import com.travelplan.payment.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUserIdAndIsActiveTrue(Long userId);

    Page<PaymentMethod> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);

    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.userId = :userId AND pm.id != :id")
    void unsetOtherDefaults(@Param("userId") Long userId, @Param("id") Long id);

    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE pm.userId = :userId AND pm.isActive = true")
    long countActiveByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isActive = false WHERE pm.userId = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);
}
