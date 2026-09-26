package com.urbancompany.clone.service;

import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.repository.ServiceProviderRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ServiceProviderService {
    private final ServiceProviderRepository serviceProviderRepository;
    private final PasswordEncoder passwordEncoder;

    public ServiceProviderService(ServiceProviderRepository serviceProviderRepository,
                                  @Lazy PasswordEncoder passwordEncoder) {
        this.serviceProviderRepository = serviceProviderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Cacheable("providers")
    public List<ServiceProvider> getAllProviders() {
        return serviceProviderRepository.findAll();
    }

    @Cacheable("provider")
    public Optional<ServiceProvider> getProviderById(Long id) {
        return serviceProviderRepository.findById(id);
    }

    @Cacheable("providersByService")
    public List<ProviderWithDistance> getNearestProvidersByService(Long serviceId, Double userLat, Double userLng, int limit) {
        List<ServiceProvider> providers = serviceProviderRepository.findAvailableProvidersByService(serviceId);

        return providers.stream()
                .map(provider -> new ProviderWithDistance(provider, calculateDistance(userLat, userLng, provider.getLocation().getLatitude(), provider.getLocation().getLongitude())))
                .sorted((a, b) -> Double.compare(a.getDistance(), b.getDistance()))
                .limit(limit)
                .toList();
    }

    public ProviderWithDistance getNearestProviderByService(Long serviceId, Double userLat, Double userLng) {
        List<ProviderWithDistance> providers = getNearestProvidersByService(serviceId, userLat, userLng, 1);
        return providers.isEmpty() ? null : providers.get(0);
    }

    @Cacheable("nearbyProviders")
    public List<ProviderWithDistance> getNearbyProviders(Double userLat, Double userLng, double radiusKm, List<Long> serviceIds) {
        List<ServiceProvider> providers = serviceProviderRepository.findByIsAvailableTrue();

        return providers.stream()
                .filter(provider -> serviceIds == null || serviceIds.isEmpty() || serviceIds.stream().anyMatch(sid -> provider.getServiceIds().contains(sid)))
                .filter(provider -> provider.getLocation() != null && provider.getLocation().getLatitude() != null && provider.getLocation().getLongitude() != null)
                .map(provider -> new ProviderWithDistance(provider, calculateDistance(userLat, userLng, provider.getLocation().getLatitude(), provider.getLocation().getLongitude())))
                .filter(pd -> pd.getDistance() <= radiusKm)
                .sorted((a, b) -> Double.compare(a.getDistance(), b.getDistance()))
                .toList();
    }

    public ServiceProvider createProvider(ServiceProvider provider) {
        if (provider.getEmail() != null
                && serviceProviderRepository.findByEmail(provider.getEmail()).isPresent()) {
            throw new IllegalArgumentException("A professional with this email already exists");
        }
        provider.setRole("PROVIDER");
        provider.setPassword(encodeIfRaw(provider.getPassword()));
        ServiceProvider saved = serviceProviderRepository.save(provider);
        evictProviderCache();
        return saved;
    }

    @CacheEvict(value = {"providers", "provider", "providersByService", "nearbyProviders"}, allEntries = true)
    public void evictProviderCache() {}

    public ServiceProvider updateProvider(Long id, ServiceProvider providerDetails) {
        ServiceProvider provider = serviceProviderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        provider.setName(providerDetails.getName());
        provider.setEmail(providerDetails.getEmail());
        provider.setPhone(providerDetails.getPhone());
        provider.setLocation(providerDetails.getLocation());
        provider.setRating(providerDetails.getRating());
        provider.setIsAvailable(providerDetails.getIsAvailable());
        provider.setIsVerified(providerDetails.getIsVerified());
        provider.setServiceIds(providerDetails.getServiceIds());
        provider.setExperienceYears(providerDetails.getExperienceYears());
        provider.setBio(providerDetails.getBio());
        provider.setCertifications(providerDetails.getCertifications());
        if (providerDetails.getPassword() != null && !providerDetails.getPassword().isBlank()) {
            provider.setPassword(encodeIfRaw(providerDetails.getPassword()));
        }
        evictProviderCache();
        return serviceProviderRepository.save(provider);
    }
    public void deleteProvider(Long id) {
        ServiceProvider provider = serviceProviderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        serviceProviderRepository.delete(provider);
        evictProviderCache();
    }

    /** BCrypt-hashes raw passwords; leaves already-encoded ones untouched. */
    private String encodeIfRaw(String password) {
        if (password == null || password.isBlank()) {
            return password;
        }
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }

    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
