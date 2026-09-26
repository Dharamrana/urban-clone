package com.urbancompany.clone.controller;

import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.service.ProviderWithDistance;
import com.urbancompany.clone.service.ServiceProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ServiceProviderController {
    private final ServiceProviderService serviceProviderService;

    public ServiceProviderController(ServiceProviderService serviceProviderService) {
        this.serviceProviderService = serviceProviderService;
    }

    @GetMapping
    public List<ServiceProvider> getAllProviders() {
        return serviceProviderService.getAllProviders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceProvider> getProviderById(@PathVariable Long id) {
        return serviceProviderService.getProviderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nearest")
    public ResponseEntity<List<ProviderWithDistance>> getNearestProviders(
            @RequestParam Long serviceId,
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10") int limit) {
        List<ProviderWithDistance> providers = serviceProviderService.getNearestProvidersByService(serviceId, lat, lng, limit);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/nearest-one")
    public ResponseEntity<ProviderWithDistance> getNearestProvider(
            @RequestParam Long serviceId,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        ProviderWithDistance provider = serviceProviderService.getNearestProviderByService(serviceId, lat, lng);
        return provider != null ? ResponseEntity.ok(provider) : ResponseEntity.notFound().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ProviderWithDistance>> getNearbyProviders(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(required = false) List<Long> serviceIds) {
        List<ProviderWithDistance> providers = serviceProviderService.getNearbyProviders(lat, lng, radiusKm, serviceIds);
        return ResponseEntity.ok(providers);
    }

    @PostMapping
    public ServiceProvider createProvider(@Valid @RequestBody ServiceProvider provider) {
        return serviceProviderService.createProvider(provider);
    }

    @PutMapping("/{id}")
    public ServiceProvider updateProvider(@PathVariable Long id, @Valid @RequestBody ServiceProvider providerDetails) {
        return serviceProviderService.updateProvider(id, providerDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        serviceProviderService.deleteProvider(id);
        return ResponseEntity.noContent().build();
    }
}
