package com.urbancompany.clone.service;

import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.model.ServiceRequestItem;
import com.urbancompany.clone.model.ServiceRequestStatus;
import com.urbancompany.clone.model.User;
import com.urbancompany.clone.repository.ServiceProviderRepository;
import com.urbancompany.clone.repository.ServiceRepository;
import com.urbancompany.clone.repository.ServiceRequestRepository;
import com.urbancompany.clone.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ServiceRequestService {
    public static final double VISITING_FEE = 49.0;

    /** Urban Company style time windows offered every day. */
    public static final List<String> DAILY_SLOTS = Arrays.asList(
            "08:00-10:00", "10:00-12:00", "12:00-14:00",
            "14:00-16:00", "16:00-18:00", "18:00-20:00");

    /** Allowed status transitions (UC: booked -> assigned -> in-progress -> done). */
    private static final Map<ServiceRequestStatus, Set<ServiceRequestStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ServiceRequestStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.PENDING,
                EnumSet.of(ServiceRequestStatus.ASSIGNED, ServiceRequestStatus.CANCELLED, ServiceRequestStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.ASSIGNED,
                EnumSet.of(ServiceRequestStatus.IN_PROGRESS, ServiceRequestStatus.CANCELLED, ServiceRequestStatus.REJECTED, ServiceRequestStatus.ASSIGNED));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.IN_PROGRESS,
                EnumSet.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.COMPLETED, EnumSet.noneOf(ServiceRequestStatus.class));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.CANCELLED, EnumSet.noneOf(ServiceRequestStatus.class));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.REJECTED, EnumSet.of(ServiceRequestStatus.ASSIGNED));
    }

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
                                 ServiceProviderRepository serviceProviderRepository,
                                 ServiceRepository serviceRepository,
                                 UserRepository userRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.serviceProviderRepository = serviceProviderRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    /** Privacy-sensitive (scoped per user in controller) — never cached across accounts. */
    public List<ServiceRequest> getAllRequests() {
        return serviceRequestRepository.findAll();
    }

    public Optional<ServiceRequest> getRequestById(Long id) {
        return serviceRequestRepository.findById(id);
    }

    public List<ServiceRequest> getRequestsByUser(Long userId) {
        return serviceRequestRepository.findByUserId(userId);
    }

    public List<ServiceRequest> getRequestsByProvider(Long providerId) {
        return serviceRequestRepository.findByProviderId(providerId);
    }

    public List<ServiceRequest> getRequestsByStatus(ServiceRequestStatus status) {
        return serviceRequestRepository.findByStatus(status);
    }

    public List<String> getAvailableSlots(LocalDate date) {
        if (date != null && date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Slot date cannot be in the past");
        }
        return DAILY_SLOTS;
    }

    public ServiceRequest createRequest(ServiceRequest request) {
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new IllegalArgumentException("Service address is required for booking");
        }
        // The booking is always owned by a real account (no more hardcoded user 1).
        bindBookingUser(request);
        // Normalize the cart: legacy single-service bookings become a 1-item cart,
        // multi-service carts are validated + price-locked line by line.
        normalizeCart(request);

        // Resolve provider if one was picked in the wizard (UC: choose professional or auto-assign).
        if (request.getProvider() != null && request.getProvider().getId() != null) {
            ServiceProvider provider = serviceProviderRepository.findById(request.getProvider().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
            if (Boolean.FALSE.equals(provider.getIsAvailable())) {
                throw new IllegalStateException("Selected provider is currently unavailable");
            }
            request.setProvider(provider);
        } else {
            request.setProvider(null);
        }

        // UC slot defaults: tomorrow if the customer skipped slot selection.
        if (request.getScheduledDate() == null) {
            request.setScheduledDate(LocalDate.now().plusDays(1));
        }
        if (request.getScheduledDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Slot date cannot be in the past");
        }
        if (request.getScheduledSlot() == null || request.getScheduledSlot().isBlank()) {
            request.setScheduledSlot(DAILY_SLOTS.get(1));
        } else if (!DAILY_SLOTS.contains(request.getScheduledSlot())) {
            throw new IllegalArgumentException("Invalid slot. Choose one of " + DAILY_SLOTS);
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            request.setPaymentMethod("UPI");
        }

        // UC price breakup: sum of locked line totals + fixed visiting fee (server-computed, never trusted from client).
        double itemsTotal = request.getItems().stream()
                .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                .sum();
        request.setVisitingFee(VISITING_FEE);
        request.setFinalPrice(itemsTotal + VISITING_FEE);

        request.setRequestedAt(LocalDateTime.now());
        // UC flow: picking a professional confirms assignment, otherwise wait for assignment.
        request.setStatus(request.getProvider() != null
                ? ServiceRequestStatus.ASSIGNED
                : ServiceRequestStatus.PENDING);
        if (request.getProvider() != null) {
            ensureOtp(request);
        }
        ServiceRequest saved = serviceRequestRepository.save(request);
        evictCache();
        return saved;
    }

    /**
     * Binds the booking to the logged-in account (contact details from the
     * wizard refresh the profile). Anonymous callers must reference an
     * existing account explicitly (legacy/back-compat path).
     */
    private void bindBookingUser(ServiceRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            User account = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("Logged-in account not found"));
            if (request.getUser() != null) {
                if (request.getUser().getName() != null && !request.getUser().getName().isBlank()) {
                    account.setName(request.getUser().getName());
                }
                if (request.getUser().getPhone() != null && !request.getUser().getPhone().isBlank()) {
                    account.setPhone(request.getUser().getPhone());
                }
                if (request.getUser().getLocation() != null) {
                    account.setLocation(request.getUser().getLocation());
                }
            }
            request.setUser(account);
            return;
        }
        if (request.getUser() != null && request.getUser().getId() != null) {
            User account = userRepository.findById(request.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            request.setUser(account);
            return;
        }
        throw new IllegalArgumentException("Please log in or sign up before booking");
    }

    /**
     * Validates the booking cart and locks prices from the DB.
     * Legacy payloads with only `service` get a single-qty-1 line so old clients keep working.
     */
    private void normalizeCart(ServiceRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            if (request.getService() == null || request.getService().getId() == null) {
                throw new IllegalArgumentException("Add at least one service to the cart");
            }
            com.urbancompany.clone.model.Service service =
                    serviceRepository.findById(request.getService().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Service not found"));
            assertServiceBookable(service);
            request.setService(service);
            ServiceRequestItem line = new ServiceRequestItem();
            line.setService(service);
            line.setServiceName(service.getName());
            line.setUnitPrice(service.getBasePrice() != null ? service.getBasePrice() : 0.0);
            line.setQuantity(1);
            request.setItems(new java.util.ArrayList<>(List.of(line)));
            return;
        }
        if (request.getItems().size() > 20) {
            throw new IllegalArgumentException("A maximum of 20 lines per booking is allowed");
        }
        for (ServiceRequestItem line : request.getItems()) {
            if (line.getService() == null || line.getService().getId() == null) {
                throw new IllegalArgumentException("Each cart line needs a service");
            }
            int qty = line.getQuantity() != null ? line.getQuantity() : 1;
            if (qty < 1 || qty > 10) {
                throw new IllegalArgumentException("Quantity must be between 1 and 10");
            }
            com.urbancompany.clone.model.Service service =
                    serviceRepository.findById(line.getService().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Service not found"));
            assertServiceBookable(service);
            line.setId(null); // always new lines on create
            line.setService(service);
            line.setServiceName(service.getName());
            line.setUnitPrice(service.getBasePrice() != null ? service.getBasePrice() : 0.0);
            line.setQuantity(qty);
        }
        // Primary service = first cart line (backward compat for old UIs).
        request.setService(request.getItems().get(0).getService());
    }

    private void assertServiceBookable(com.urbancompany.clone.model.Service service) {
        if (Boolean.FALSE.equals(service.getIsActive())) {
            throw new IllegalStateException("Service '" + service.getName() + "' is currently unavailable");
        }
    }

    /** Price preview for the cart drawer / checkout step (no booking created). */
    public Map<String, Object> quoteCart(List<ServiceRequestItem> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        double subtotal = 0.0;
        for (ServiceRequestItem line : lines) {
            if (line.getService() == null || line.getService().getId() == null) {
                throw new IllegalArgumentException("Each cart line needs a service");
            }
            int qty = line.getQuantity() != null ? line.getQuantity() : 1;
            if (qty < 1 || qty > 10) {
                throw new IllegalArgumentException("Quantity must be between 1 and 10");
            }
            com.urbancompany.clone.model.Service service =
                    serviceRepository.findById(line.getService().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Service not found"));
            double unit = service.getBasePrice() != null ? service.getBasePrice() : 0.0;
            double lineTotal = unit * qty;
            subtotal += lineTotal;
            out.add(Map.of(
                    "serviceId", service.getId(),
                    "name", service.getName(),
                    "quantity", qty,
                    "unitPrice", unit,
                    "lineTotal", lineTotal));
        }
        return Map.of(
                "lines", out,
                "subtotal", subtotal,
                "visitingFee", VISITING_FEE,
                "total", subtotal + VISITING_FEE);
    }

    public ServiceRequest assignProvider(Long requestId, Long providerId) {
        return assignProvider(requestId, providerId, null, true);
    }

    /** Owner (re-pick) or admin assignment; generates the customer OTP on attach. */
    public ServiceRequest assignProvider(Long requestId, Long providerId, String requesterEmail, boolean admin) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        requireOwnerOrAdmin(request, requesterEmail, admin);
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found with id: " + providerId));
        if (Boolean.FALSE.equals(provider.getIsAvailable())) {
            throw new IllegalStateException("Selected provider is currently unavailable");
        }
        validateTransition(request.getStatus(), ServiceRequestStatus.ASSIGNED);
        request.setProvider(provider);
        request.setStatus(ServiceRequestStatus.ASSIGNED);
        ensureOtp(request);
        evictCache();
        return serviceRequestRepository.save(request);
    }

    public ServiceRequest updateStatus(Long requestId, ServiceRequestStatus status) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        validateTransition(request.getStatus(), status);
        request.setStatus(status);
        if (status == ServiceRequestStatus.COMPLETED) {
            request.setCompletedAt(LocalDateTime.now());
        }
        evictCache();
        return serviceRequestRepository.save(request);
    }

    public ServiceRequest cancelRequest(Long requestId) {
        return cancelRequest(requestId, null, true);
    }

    public ServiceRequest cancelRequest(Long requestId, String requesterEmail, boolean admin) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        if (!admin) {
            requireOwnerOrJobProvider(request, requesterEmail);
        }
        if (request.getStatus() == ServiceRequestStatus.COMPLETED
                || request.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw new IllegalStateException("Completed or already-cancelled bookings cannot be cancelled");
        }
        request.setStatus(ServiceRequestStatus.CANCELLED);
        // Mock gateway: paid online bookings auto-refund on cancellation.
        if ("PAID".equals(request.getPaymentStatus())) {
            request.setPaymentStatus("REFUNDED");
        }
        evictCache();
        return serviceRequestRepository.save(request);
    }

    public ServiceRequest completeRequest(Long requestId, Double finalPrice) {
        return completeRequest(requestId, finalPrice, null, true);
    }

    public ServiceRequest completeRequest(Long requestId, Double finalPrice, String requesterEmail, boolean admin) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        if (!admin && requesterEmail != null) {
            requireOwnerOrJobProvider(request, requesterEmail);
        }
        validateTransition(request.getStatus(), ServiceRequestStatus.COMPLETED);
        if (finalPrice != null) {
            request.setFinalPrice(finalPrice);
        }
        request.setStatus(ServiceRequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        evictCache();
        return serviceRequestRepository.save(request);
    }

    /** UC reschedule: move visit to another day/slot while booking is still active. */
    public ServiceRequest rescheduleRequest(Long requestId, LocalDate date, String slot) {
        return rescheduleRequest(requestId, date, slot, null, true);
    }

    public ServiceRequest rescheduleRequest(Long requestId, LocalDate date, String slot,
                                            String requesterEmail, boolean admin) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        requireOwnerOrAdmin(request, requesterEmail, admin);
        if (request.getStatus() != ServiceRequestStatus.PENDING
                && request.getStatus() != ServiceRequestStatus.ASSIGNED) {
            throw new IllegalStateException("Only pending or assigned bookings can be rescheduled");
        }
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Rescheduled date cannot be in the past");
        }
        if (slot == null || !DAILY_SLOTS.contains(slot)) {
            throw new IllegalArgumentException("Invalid slot. Choose one of " + DAILY_SLOTS);
        }
        request.setScheduledDate(date);
        request.setScheduledSlot(slot);
        evictCache();
        return serviceRequestRepository.save(request);
    }

    /** UC rate professional: 1-5 stars + review after completion; rolls up to provider rating. */
    public ServiceRequest rateRequest(Long requestId, int rating, String review) {
        return rateRequest(requestId, rating, review, null, true);
    }

    public ServiceRequest rateRequest(Long requestId, int rating, String review,
                                      String requesterEmail, boolean admin) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        requireOwnerOrAdmin(request, requesterEmail, admin);
        if (request.getStatus() != ServiceRequestStatus.COMPLETED) {
            throw new IllegalStateException("Only completed bookings can be rated");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        request.setRating(rating);
        request.setReview(review);
        if (request.getProvider() != null) {
            ServiceProvider provider = request.getProvider();
            int total = provider.getTotalReviews() != null ? provider.getTotalReviews() : 0;
            double current = provider.getRating() != null ? provider.getRating() : 0.0;
            provider.setRating(((current * total) + rating) / (total + 1));
            provider.setTotalReviews(total + 1);
            serviceProviderRepository.save(provider);
        }
        evictCache();
        return serviceRequestRepository.save(request);
    }

    private void validateTransition(ServiceRequestStatus from, ServiceRequestStatus to) {
        if (from == to) {
            return;
        }
        Set<ServiceRequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException("Cannot move booking from " + from + " to " + to);
        }
    }

    // ---------------- ownership ----------------

    private void requireOwnerOrAdmin(ServiceRequest request, String requesterEmail, boolean admin) {
        if (admin || requesterEmail == null) {
            return; // legacy/internal path (service tests, admin ops)
        }
        if (request.getUser() != null && requesterEmail.equalsIgnoreCase(request.getUser().getEmail())) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException("Not your booking");
    }

    private void requireOwnerOrJobProvider(ServiceRequest request, String requesterEmail) {
        if (requesterEmail == null) {
            return; // legacy/internal path
        }
        if (request.getUser() != null && requesterEmail.equalsIgnoreCase(request.getUser().getEmail())) {
            return;
        }
        if (request.getProvider() != null && requesterEmail.equalsIgnoreCase(request.getProvider().getEmail())) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException("Not your booking or job");
    }

    private ServiceProvider requireProviderAccount(String providerEmail) {
        if (providerEmail == null) {
            throw new IllegalArgumentException("Professional login required");
        }
        return serviceProviderRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Professional account not found"));
    }

    private ServiceRequest requireOwnJob(Long requestId, String providerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        if (request.getProvider() == null
                || !providerEmail.equalsIgnoreCase(request.getProvider().getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your job");
        }
        return request;
    }

    private void ensureOtp(ServiceRequest request) {
        if (request.getStartOtp() == null || request.getStartOtp().isBlank()) {
            request.setStartOtp(String.format("%04d", new java.util.Random().nextInt(10000)));
            request.setOtpVerified(false);
        }
    }

    // ---------------- professional job engine (UC partner app) ----------------

    /** Jobs currently with this professional (active + history). */
    public List<ServiceRequest> getProviderJobs(String providerEmail) {
        ServiceProvider me = requireProviderAccount(providerEmail);
        return serviceRequestRepository.findByProviderId(me.getId());
    }

    /** Unassigned bookings matching this professional's services (the open pool). */
    public List<ServiceRequest> getAvailablePool(String providerEmail) {
        ServiceProvider me = requireProviderAccount(providerEmail);
        List<Long> mine = me.getServiceIds() != null ? me.getServiceIds() : List.of();
        return serviceRequestRepository.findByStatus(ServiceRequestStatus.PENDING).stream()
                .filter(r -> {
                    if (r.getService() == null || r.getService().getId() == null) {
                        return false;
                    }
                    if (!mine.contains(r.getService().getId())) {
                        return r.getItems() != null && r.getItems().stream()
                                .anyMatch(i -> i.getService() != null && mine.contains(i.getService().getId()));
                    }
                    return true;
                })
                .toList();
    }

    /** Accept an open job → ASSIGNED to self + OTP issued to the customer. */
    public ServiceRequest acceptJob(Long requestId, String providerEmail) {
        ServiceProvider me = requireProviderAccount(providerEmail);
        if (Boolean.FALSE.equals(me.getIsAvailable())) {
            throw new IllegalStateException("Mark yourself available before accepting jobs");
        }
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        if (request.getStatus() != ServiceRequestStatus.PENDING || request.getProvider() != null) {
            throw new IllegalStateException("Job is no longer open");
        }
        request.setProvider(me);
        request.setStatus(ServiceRequestStatus.ASSIGNED);
        ensureOtp(request);
        evictCache();
        return serviceRequestRepository.save(request);
    }

    /** Turn down a job assigned to self. */
    public ServiceRequest rejectJob(Long requestId, String providerEmail) {
        ServiceRequest request = requireOwnJob(requestId, providerEmail);
        validateTransition(request.getStatus(), ServiceRequestStatus.REJECTED);
        request.setStatus(ServiceRequestStatus.REJECTED);
        evictCache();
        return serviceRequestRepository.save(request);
    }

    /** Start work after the customer shares their OTP (UC presence proof). */
    public ServiceRequest startJob(Long requestId, String providerEmail, String otp) {
        ServiceRequest request = requireOwnJob(requestId, providerEmail);
        validateTransition(request.getStatus(), ServiceRequestStatus.IN_PROGRESS);
        if (request.getStartOtp() == null || otp == null
                || !request.getStartOtp().equals(otp.trim())) {
            throw new IllegalArgumentException("Wrong OTP — ask the customer for the current code");
        }
        request.setOtpVerified(true);
        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        evictCache();
        return serviceRequestRepository.save(request);
    }

    /** Finish the job with the collected amount. */
    public ServiceRequest completeJob(Long requestId, String providerEmail, Double finalPrice) {
        ServiceRequest request = requireOwnJob(requestId, providerEmail);
        validateTransition(request.getStatus(), ServiceRequestStatus.COMPLETED);
        if (finalPrice != null) {
            request.setFinalPrice(finalPrice);
        }
        request.setStatus(ServiceRequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        evictCache();
        return serviceRequestRepository.save(request);
    }

    public ServiceProvider setProviderAvailability(String providerEmail, boolean available) {
        ServiceProvider me = requireProviderAccount(providerEmail);
        me.setIsAvailable(available);
        return serviceProviderRepository.save(me);
    }

    /** Month-to-date earnings from completed jobs + job counts by status. */
    public Map<String, Object> providerEarnings(String providerEmail) {
        ServiceProvider me = requireProviderAccount(providerEmail);
        List<ServiceRequest> jobs = serviceRequestRepository.findByProviderId(me.getId());
        java.time.YearMonth month = java.time.YearMonth.now();
        double monthEarnings = jobs.stream()
                .filter(j -> j.getStatus() == ServiceRequestStatus.COMPLETED
                        && j.getCompletedAt() != null
                        && java.time.YearMonth.from(j.getCompletedAt()).equals(month))
                .mapToDouble(j -> j.getFinalPrice() != null ? j.getFinalPrice() : 0.0)
                .sum();
        long active = jobs.stream()
                .filter(j -> j.getStatus() == ServiceRequestStatus.ASSIGNED
                        || j.getStatus() == ServiceRequestStatus.IN_PROGRESS
                        || j.getStatus() == ServiceRequestStatus.PENDING)
                .count();
        long done = jobs.stream()
                .filter(j -> j.getStatus() == ServiceRequestStatus.COMPLETED)
                .count();
        return Map.of(
                "month", month.toString(),
                "monthEarnings", monthEarnings,
                "activeJobs", active,
                "completedJobs", done,
                "rating", me.getRating() != null ? me.getRating() : 0.0);
    }

    // ---------------- mock payment gateway (UC checkout) ----------------

    /** Creates a mock payment intent for UPI/CARD bookings. CASH needs no gateway. */
    public Map<String, Object> initPayment(Long requestId, String requesterEmail, boolean admin) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        requireOwnerOrAdmin(request, requesterEmail, admin);
        if ("CASH".equalsIgnoreCase(request.getPaymentMethod())) {
            return Map.of("mode", "CASH", "message", "Pay the professional after the service");
        }
        if ("PAID".equals(request.getPaymentStatus())) {
            return Map.of("mode", request.getPaymentMethod(), "message", "Already paid",
                    "ref", request.getPaymentRef() != null ? request.getPaymentRef() : "");
        }
        String ref = "MOCK-" + System.currentTimeMillis() + "-" + requestId;
        request.setPaymentRef(ref);
        request.setPaymentStatus("PENDING");
        serviceRequestRepository.save(request);
        evictCache();
        return Map.of(
                "mode", request.getPaymentMethod(),
                "ref", ref,
                "amount", request.getFinalPrice() != null ? request.getFinalPrice() : 0.0,
                "message", "Mock gateway: confirm to simulate a successful payment");
    }

    /** Simulates the gateway callback (success=true captures, false declines). */
    public ServiceRequest confirmPayment(Long requestId, String ref, boolean success,
                                         String requesterEmail, boolean admin) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        requireOwnerOrAdmin(request, requesterEmail, admin);
        if (request.getPaymentRef() == null || !request.getPaymentRef().equals(ref)) {
            throw new IllegalArgumentException("Unknown or expired payment reference");
        }
        request.setPaymentStatus(success ? "PAID" : "FAILED");
        evictCache();
        return serviceRequestRepository.save(request);
    }

    @CacheEvict(value = {"requests"}, allEntries = true)
    public void evictCache() {}
}
