package com.urbancompany.clone.repository;

import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.model.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByUserId(Long userId);
    List<ServiceRequest> findByProviderId(Long providerId);
    List<ServiceRequest> findByStatus(ServiceRequestStatus status);
}
