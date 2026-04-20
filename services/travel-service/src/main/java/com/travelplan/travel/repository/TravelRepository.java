package com.travelplan.travel.repository;

import com.travelplan.travel.entity.Travel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TravelRepository extends JpaRepository<Travel, Long> {

    @Query("SELECT t FROM Travel t WHERE t.createdBy = :userId")
    Page<Travel> findByCreatedBy(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Travel t WHERE " +
            "(:userId IS NULL OR t.createdBy = :userId) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Travel> findAllWithFilters(
            @Param("userId") Long userId,
            @Param("status") Travel.TravelStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT t FROM Travel t LEFT JOIN FETCH t.destinations td " +
            "LEFT JOIN FETCH td.destination WHERE t.id = :id")
    Optional<Travel> findByIdWithDestinations(@Param("id") Long id);

    List<Travel> findByStatusAndStartDateLessThanEqual(Travel.TravelStatus status, LocalDate date);

    @Query("SELECT COUNT(t) FROM Travel t WHERE t.createdBy = :userId")
    long countByCreatedBy(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Travel t WHERE t.status = :status")
    long countByStatus(@Param("status") Travel.TravelStatus status);
}
