package com.urbancompany.clone.controller;

import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.model.ServiceRequestStatus;
import com.urbancompany.clone.model.User;
import com.urbancompany.clone.service.ServiceRequestService;
import com.urbancompany.clone.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {
    private final ServiceRequestService serviceRequestService;
    private final UserService userService;

    public ServiceRequestController(ServiceRequestService serviceRequestService, UserService userService) {
        this.serviceRequestService = serviceRequestService;
        this.userService = userService;
    }

    @GetMapping
    public List<ServiceRequest> getAllRequests(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            User me = userService.getUserByEmail(authentication.getName()).orElse(null);
            if (me != null) {
                return serviceRequestService.getRequestsByUser(me.getId());
            }
        }
        return serviceRequestService.getAllRequests();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Long id,
                                            Authentication authentication) {
        return serviceRequestService.getRequestById(id)
                .map(r -> {
                    String me = email(authentication);
                    boolean mine = me != null && r.getUser() != null
                            && me.equalsIgnoreCase(r.getUser().getEmail());
                    boolean myJob = me != null && r.getProvider() != null
                            && me.equalsIgnoreCase(r.getProvider().getEmail());
                    return (mine || myJob) ? ResponseEntity.<ServiceRequest>ok(r)
                            : ResponseEntity.<ServiceRequest>status(403).build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ServiceRequest>> getRequestsByUser(@PathVariable Long userId,
                                                                  Authentication authentication) {
        User me = authentication != null
                ? userService.getUserByEmail(authentication.getName()).orElse(null) : null;
        if (me == null || !me.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(serviceRequestService.getRequestsByUser(userId));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ServiceRequest>> getRequestsByProvider(@PathVariable Long providerId,
                                                                      Authentication authentication) {
        if (authentication == null || !"PROVIDER".equals(roleOf(authentication))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(serviceRequestService.getRequestsByProvider(providerId));
    }

    @PostMapping
    public ServiceRequest createRequest(@RequestBody ServiceRequest request) {
        return serviceRequestService.createRequest(request);
    }

    @PutMapping("/{id}/assign/{providerId}")
    public ServiceRequest assignProvider(@PathVariable Long id, @PathVariable Long providerId,
                                         Authentication authentication) {
        return serviceRequestService.assignProvider(id, providerId, email(authentication), false);
    }

    @PutMapping("/{id}/complete")
    public ServiceRequest completeRequest(@PathVariable Long id, @RequestParam Double finalPrice,
                                          Authentication authentication) {
        return serviceRequestService.completeRequest(id, finalPrice, email(authentication), false);
    }

    @GetMapping("/slots")
    public List<String> getSlots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return serviceRequestService.getAvailableSlots(date);
    }

    @PostMapping("/quote")
    public java.util.Map<String, Object> quoteCart(@RequestBody ServiceRequest draft) {
        return serviceRequestService.quoteCart(draft.getItems());
    }

    @PutMapping("/{id}/reschedule")
    public ServiceRequest rescheduleRequest(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String slot,
            Authentication authentication) {
        return serviceRequestService.rescheduleRequest(id, date, slot,
                email(authentication), false);
    }

    @PutMapping("/{id}/rate")
    public ServiceRequest rateRequest(
            @PathVariable Long id,
            @RequestParam int rating,
            @RequestParam(required = false) String review,
            Authentication authentication) {
        return serviceRequestService.rateRequest(id, rating, review,
                email(authentication), false);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ServiceRequest> cancelBooking(@PathVariable Long id,
                                                        Authentication authentication) {
        return ResponseEntity.ok(serviceRequestService.cancelRequest(id,
                email(authentication), false));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ServiceRequest> cancelRequest(@PathVariable Long id,
                                                        Authentication authentication) {
        ServiceRequest cancelled = serviceRequestService.cancelRequest(id,
                email(authentication), false);
        return ResponseEntity.ok(cancelled);
    }

    @PostMapping("/{id}/pay/init")
    public java.util.Map<String, Object> initPayment(@PathVariable Long id,
                                                     Authentication authentication) {
        return serviceRequestService.initPayment(id, email(authentication), false);
    }

    @PostMapping("/{id}/pay/confirm")
    public ServiceRequest confirmPayment(@PathVariable Long id,
                                         @RequestParam String ref,
                                         @RequestParam(defaultValue = "true") boolean success,
                                         Authentication authentication) {
        return serviceRequestService.confirmPayment(id, ref, success,
                email(authentication), false);
    }

    private String email(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return authentication.getName();
    }

    private String roleOf(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("");
    }
}
