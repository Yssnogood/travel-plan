package com.travelplan.travel.repository;

import com.travelplan.travel.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByIsActiveTrue();

    @Query("SELECT a FROM Activity a WHERE a.isActive = true AND " +
            "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:category IS NULL OR LOWER(a.category) = LOWER(:category))")
    Page<Activity> findAllWithFilters(@Param("search") String search,
                                       @Param("category") String category,
                                       Pageable pageable);

    @Query("SELECT a FROM Activity a JOIN a.destinations d WHERE d.id = :destinationId AND a.isActive = true")
    List<Activity> findByDestinationId(@Param("destinationId") Long destinationId);
}
