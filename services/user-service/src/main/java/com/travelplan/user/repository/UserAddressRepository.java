package com.travelplan.user.repository;

import com.travelplan.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserAddress a SET a.isPrimary = false WHERE a.userId = :userId")
    void unsetPrimaryAddresses(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserAddress a WHERE a.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
