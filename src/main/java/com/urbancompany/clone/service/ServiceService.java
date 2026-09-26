package com.urbancompany.clone.service;

import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.repository.ServiceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@Transactional
public class ServiceService {
    private final ServiceRepository serviceRepository;

    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Cacheable("services")
    public List<Service> getAllActiveServices() {
        return serviceRepository.findByIsActiveTrue();
    }

    @Cacheable("service")
    public Optional<Service> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    @Cacheable("serviceSearch")
    public List<Service> searchServices(String name) {
        return serviceRepository.findByNameContainingIgnoreCase(name);
    }

    public Service createService(Service service) {
        Service saved = serviceRepository.save(service);
        evictServiceCache();
        return saved;
    }

    @CacheEvict(value = {"services", "service", "serviceSearch"}, allEntries = true)
    public void evictServiceCache() {}

    public Service updateService(Long id, Service serviceDetails) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        service.setName(serviceDetails.getName());
        service.setDescription(serviceDetails.getDescription());
        service.setBasePrice(serviceDetails.getBasePrice());
        service.setIconUrl(serviceDetails.getIconUrl());
        service.setIsActive(serviceDetails.getIsActive());
        evictServiceCache();
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        serviceRepository.delete(service);
        evictServiceCache();
    }
}
