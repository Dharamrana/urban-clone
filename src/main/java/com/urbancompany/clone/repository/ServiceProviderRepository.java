package com.urbancompany.clone.repository;

import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {
    List<ServiceProvider> findByIsAvailableTrue();

    Optional<ServiceProvider> findByEmail(String email);

    @Query("SELECT p FROM ServiceProvider p JOIN p.serviceIds s WHERE s = :serviceId AND p.isAvailable = true AND p.location.latitude IS NOT NULL")
    List<ServiceProvider> findAvailableProvidersByService(@Param("serviceId") Long serviceId);

    @Query("SELECT p FROM ServiceProvider p WHERE p.location.latitude IS NOT NULL ORDER BY p.rating DESC")
    List<ServiceProvider> findAllByOrderByRatingDesc();
}
