package com.urbancompany.clone.controller;

import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.service.ServiceProviderService;
import com.urbancompany.clone.service.ServiceRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Professional (partner) API — every call is scoped to the logged-in
 * professional's own jobs. No one can touch another pro's work.
 */
@RestController
@RequestMapping("/api/provider")
public class ProviderController {
    private final ServiceRequestService requestService;
    private final ServiceProviderService providerService;

    public ProviderController(ServiceRequestService requestService,
                              ServiceProviderService providerService) {
        this.requestService = requestService;
        this.providerService = providerService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return providerService.getAllProviders().stream()
                .filter(p -> authentication.getName().equalsIgnoreCase(p.getEmail()))
                .findFirst()
                .map(p -> ResponseEntity.ok(Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "email", p.getEmail(),
                        "rating", p.getRating() != null ? p.getRating() : 0.0,
                        "isAvailable", Boolean.TRUE.equals(p.getIsAvailable()))))
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Professional account not found")));
    }

    @GetMapping("/jobs")
    public List<ServiceRequest> myJobs(Authentication authentication) {
        return requestService.getProviderJobs(authentication.getName());
    }

    @GetMapping("/pool")
    public List<ServiceRequest> openPool(Authentication authentication) {
        return requestService.getAvailablePool(authentication.getName());
    }

    @GetMapping("/earnings")
    public Map<String, Object> earnings(Authentication authentication) {
        return requestService.providerEarnings(authentication.getName());
    }

    @PutMapping("/availability")
    public ServiceProvider availability(Authentication authentication,
                                        @RequestParam boolean available) {
        return requestService.setProviderAvailability(authentication.getName(), available);
    }

    @PutMapping("/jobs/{id}/accept")
    public ServiceRequest accept(@PathVariable Long id, Authentication authentication) {
        return requestService.acceptJob(id, authentication.getName());
    }

    @PutMapping("/jobs/{id}/reject")
    public ServiceRequest reject(@PathVariable Long id, Authentication authentication) {
        return requestService.rejectJob(id, authentication.getName());
    }

    @PutMapping("/jobs/{id}/start")
    public ResponseEntity<?> start(@PathVariable Long id,
                                   @RequestParam String otp,
                                   Authentication authentication) {
        try {
            return ResponseEntity.ok(requestService.startJob(id, authentication.getName(), otp));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/jobs/{id}/complete")
    public ServiceRequest complete(@PathVariable Long id,
                                   @RequestParam(required = false) Double finalPrice,
                                   Authentication authentication) {
        return requestService.completeJob(id, authentication.getName(), finalPrice);
    }
}
