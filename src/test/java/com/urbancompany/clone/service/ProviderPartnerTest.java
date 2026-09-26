package com.urbancompany.clone.service;

import com.urbancompany.clone.config.CustomUserDetailsService;
import com.urbancompany.clone.model.Location;
import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.model.ServiceRequestStatus;
import com.urbancompany.clone.model.User;
import com.urbancompany.clone.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProviderPartnerTest {

    @Autowired
    private ServiceRequestService requestService;

    @Autowired
    private ServiceProviderService providerService;

    @Autowired
    private UserService userService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private ServiceProvider pro(String email) {
        ServiceProvider p = new ServiceProvider();
        p.setName("Pro " + email);
        p.setEmail(email);
        p.setPhone("9888888888");
        p.setPassword("propass123");
        p.setLocation(new Location(28.61, 77.20, "Delhi"));
        p.setIsAvailable(true);
        p.setIsVerified(true);
        return providerService.createProvider(p);
    }

    private User customer(String email) {
        return userService.createUser(
                new User(null, "Cust " + email, email, "9777777777", "custpass", "CUSTOMER", null));
    }

    private ServiceRequest openBooking(User owner, Service svc) {
        ServiceRequest request = new ServiceRequest();
        User ref = new User();
        ref.setId(owner.getId());
        request.setUser(ref);
        request.setService(svc);
        request.setAddress("Partner test street");
        request.setScheduledDate(LocalDate.now().plusDays(1));
        request.setScheduledSlot("10:00-12:00");
        request.setPaymentMethod("CARD");
        return requestService.createRequest(request);
    }

    @Test
    void providerLogsInWithRoleAndPasswordHashed() {
        ServiceProvider saved = pro("plumber@test.com");
        assertNotEquals("propass123", saved.getPassword());
        UserDetails details = userDetailsService.loadUserByUsername("plumber@test.com");
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER")));
    }

    @Test
    void acceptStartCompleteWithOtp() {
        Service svc = serviceRepository.save(
                new Service(null, "Partner Cleaning", "desc", 499.0, null, null, true));
        ServiceProvider me = pro(" cleaner@test.com".trim());
        me.setServiceIds(new java.util.ArrayList<>(List.of(svc.getId())));
        providerService.updateProvider(me.getId(), me);

        User owner = customer("owner@test.com");
        ServiceRequest booking = openBooking(owner, svc);
        assertEquals(ServiceRequestStatus.PENDING, booking.getStatus());

        // Open pool shows it to matching pros.
        assertTrue(requestService.getAvailablePool("cleaner@test.com").stream()
                .anyMatch(r -> r.getId().equals(booking.getId())));

        ServiceRequest accepted = requestService.acceptJob(booking.getId(), "cleaner@test.com");
        assertEquals(ServiceRequestStatus.ASSIGNED, accepted.getStatus());
        assertNotNull(accepted.getStartOtp());
        assertEquals(4, accepted.getStartOtp().length());

        assertThrows(IllegalArgumentException.class, () ->
                requestService.startJob(booking.getId(), "cleaner@test.com", "0000"));
        ServiceRequest started = requestService.startJob(
                booking.getId(), "cleaner@test.com", accepted.getStartOtp());
        assertEquals(ServiceRequestStatus.IN_PROGRESS, started.getStatus());
        assertTrue(started.getOtpVerified());

        ServiceRequest done = requestService.completeJob(booking.getId(), "cleaner@test.com", 548.0);
        assertEquals(ServiceRequestStatus.COMPLETED, done.getStatus());
        assertEquals(548.0, done.getFinalPrice());

        Map<String, Object> earnings = requestService.providerEarnings("cleaner@test.com");
        assertEquals(1L, earnings.get("completedJobs"));
        assertTrue((Double) earnings.get("monthEarnings") >= 548.0);
    }

    @Test
    void ownershipEnforcedAcrossRoles() {
        Service svc = serviceRepository.save(
                new Service(null, "Owned Repair", "desc", 299.0, null, null, true));
        ServiceProvider me = pro("owned@test.com");
        me.setServiceIds(new java.util.ArrayList<>(List.of(svc.getId())));
        providerService.updateProvider(me.getId(), me);
        ServiceProvider stranger = pro("stranger@test.com");

        User owner = customer("owned-owner@test.com");
        User intruder = customer("intruder@test.com");
        ServiceRequest booking = openBooking(owner, svc);
        ServiceRequest accepted = requestService.acceptJob(booking.getId(), "owned@test.com");

        // Stranger pro cannot start someone else's job.
        assertThrows(AccessDeniedException.class, () ->
                requestService.startJob(booking.getId(), "stranger@test.com", accepted.getStartOtp()));
        // Other customer cannot reschedule / rate / cancel it.
        assertThrows(AccessDeniedException.class, () ->
                requestService.rescheduleRequest(booking.getId(),
                        LocalDate.now().plusDays(3), "14:00-16:00", "intruder@test.com", false));
        // Owner can.
        ServiceRequest moved = requestService.rescheduleRequest(booking.getId(),
                LocalDate.now().plusDays(3), "14:00-16:00", "owned-owner@test.com", false);
        assertEquals("14:00-16:00", moved.getScheduledSlot());
    }

    @Test
    void mockPaymentThenRefundOnCancel() {
        Service svc = serviceRepository.save(
                new Service(null, "Payable Fix", "desc", 399.0, null, null, true));
        User owner = customer("payer@test.com");
        ServiceRequest booking = openBooking(owner, svc);

        Map<String, Object> intent = requestService.initPayment(booking.getId(), "payer@test.com", false);
        assertTrue(intent.get("ref").toString().startsWith("MOCK-"));

        ServiceRequest paid = requestService.confirmPayment(
                booking.getId(), intent.get("ref").toString(), true, "payer@test.com", false);
        assertEquals("PAID", paid.getPaymentStatus());

        ServiceRequest cancelled = requestService.cancelRequest(booking.getId(), "payer@test.com", false);
        assertEquals("REFUNDED", cancelled.getPaymentStatus());
    }
}
