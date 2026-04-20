package com.travelplan.travel.repository;

import com.travelplan.travel.entity.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {

    List<Destination> findByIsActiveTrue();

    @Query("SELECT d FROM Destination d WHERE d.isActive = true AND " +
            "(:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.country) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Destination> findAllWithSearch(@Param("search") String search, Pageable pageable);

    List<Destination> findByCountryIgnoreCase(String country);
}
