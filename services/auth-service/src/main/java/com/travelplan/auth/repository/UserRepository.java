package com.travelplan.auth.repository;

import com.travelplan.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role r JOIN FETCH r.permissions WHERE u.email = :email")
    Optional<User> findByEmailWithRoleAndPermissions(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role r JOIN FETCH r.permissions WHERE u.id = :id")
    Optional<User> findByIdWithRoleAndPermissions(Long id);
}
