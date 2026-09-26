package com.urbancompany.clone.service;

import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.model.ServiceRequestStatus;
import com.urbancompany.clone.model.User;
import com.urbancompany.clone.repository.ServiceRepository;
import com.urbancompany.clone.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingWorkflowTest {

    @Autowired
    private ServiceRequestService requestService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceRequestRepository requestRepository;

    @Autowired
    private UserService userService;

    private User testAccount() {
        return userService.createUser(
                new User(null, "Workflow User", "workflow@test.com", "9000000001", "secret123", "CUSTOMER", null));
    }

    private void bindAs(User account, ServiceRequest request) {
        User ref = new User();
        ref.setId(account.getId());
        request.setUser(ref);
    }

    private ServiceRequest newBooking() {
        User account = testAccount();
        Service service = serviceRepository.save(
                new Service(null, "Test Cleaning", "desc", 499.0, null, null, true));
        ServiceRequest request = new ServiceRequest();
        bindAs(account, request);
        request.setService(service);
        request.setAddress("Test address, Delhi");
        request.setScheduledDate(LocalDate.now().plusDays(1));
        request.setScheduledSlot("10:00-12:00");
        request.setPaymentMethod("UPI");
        return requestService.createRequest(request);
    }

    @Test
    void bookingGetsSlotPriceAndPendingStatus() {
        ServiceRequest saved = newBooking();
        assertNotNull(saved.getId());
        assertEquals(ServiceRequestStatus.PENDING, saved.getStatus());
        assertEquals(LocalDate.now().plusDays(1), saved.getScheduledDate());
        assertEquals("10:00-12:00", saved.getScheduledSlot());
        assertEquals(499.0 + ServiceRequestService.VISITING_FEE, saved.getFinalPrice());
        assertEquals(ServiceRequestService.VISITING_FEE, saved.getVisitingFee());
    }

    @Test
    void statusMachineRejectsInvalidJump() {
        ServiceRequest saved = newBooking();
        assertThrows(IllegalStateException.class,
                () -> requestService.updateStatus(saved.getId(), ServiceRequestStatus.COMPLETED));
    }

    @Test
    void rescheduleRateAndSlots() {
        ServiceRequest saved = newBooking();
        LocalDate newDate = LocalDate.now().plusDays(2);
        ServiceRequest moved = requestService.rescheduleRequest(saved.getId(), newDate, "14:00-16:00");
        assertEquals(newDate, moved.getScheduledDate());
        assertEquals("14:00-16:00", moved.getScheduledSlot());

        requestService.updateStatus(saved.getId(), ServiceRequestStatus.ASSIGNED);
        requestService.updateStatus(saved.getId(), ServiceRequestStatus.IN_PROGRESS);
        ServiceRequest done = requestService.completeRequest(saved.getId(), 548.0);
        assertEquals(ServiceRequestStatus.COMPLETED, done.getStatus());
        assertEquals(548.0, done.getFinalPrice());

        ServiceRequest rated = requestService.rateRequest(saved.getId(), 5, "Great work");
        assertEquals(5, rated.getRating());

        List<String> slots = requestService.getAvailableSlots(LocalDate.now().plusDays(1));
        assertEquals(ServiceRequestService.DAILY_SLOTS, slots);
    }

    @Test
    void bookingRequiresAnAccount() {
        ServiceRequest anonymous = new ServiceRequest();
        anonymous.setAddress("addr");
        anonymous.setScheduledDate(LocalDate.now().plusDays(1));
        anonymous.setScheduledSlot("10:00-12:00");
        assertThrows(IllegalArgumentException.class, () -> requestService.createRequest(anonymous));
    }

    @Test
    void pastSlotIsRejected() {
        Service service = serviceRepository.save(
                new Service(null, "Test Repair", "desc", 299.0, null, null, true));
        ServiceRequest request = new ServiceRequest();
        bindAs(testAccount(), request);
        request.setService(service);
        request.setAddress("addr");
        request.setScheduledDate(LocalDate.now().minusDays(1));
        request.setScheduledSlot("10:00-12:00");
        assertThrows(IllegalArgumentException.class, () -> requestService.createRequest(request));
    }
}
