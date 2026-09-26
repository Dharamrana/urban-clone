package com.urbancompany.clone.service;

import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.model.ServiceRequestItem;
import com.urbancompany.clone.model.ServiceRequestStatus;
import com.urbancompany.clone.model.User;
import com.urbancompany.clone.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartBookingTest {

    @Autowired
    private ServiceRequestService requestService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserService userService;

    private void bindTestAccount(ServiceRequest request, String email) {
        User account = userService.createUser(
                new User(null, "Cart User", email, "9000000002", "secret123", "CUSTOMER", null));
        User ref = new User();
        ref.setId(account.getId());
        request.setUser(ref);
    }

    private ServiceRequestItem line(Service service, int qty) {
        ServiceRequestItem item = new ServiceRequestItem();
        Service ref = new Service();
        ref.setId(service.getId());
        item.setService(ref);
        item.setQuantity(qty);
        return item;
    }

    private ServiceRequest cartBooking() {
        Service cleaning = serviceRepository.save(
                new Service(null, "Cart Cleaning", "desc", 499.0, null, null, true));
        Service repair = serviceRepository.save(
                new Service(null, "Cart Repair", "desc", 299.0, null, null, true));
        ServiceRequest request = new ServiceRequest();
        bindTestAccount(request, "cart@test.com");
        request.setItems(new ArrayList<>(List.of(line(cleaning, 2), line(repair, 1))));
        request.setAddress("Cart street, Delhi");
        request.setScheduledDate(LocalDate.now().plusDays(1));
        request.setScheduledSlot("12:00-14:00");
        request.setPaymentMethod("CARD");
        // Tampered total from client must be ignored (server recomputes).
        request.setFinalPrice(1.0);
        return requestService.createRequest(request);
    }

    @Test
    void multiItemBookingTotalsAndLocksPrices() {
        ServiceRequest saved = cartBooking();
        assertEquals(ServiceRequestStatus.PENDING, saved.getStatus());
        assertEquals(2, saved.getItems().size());
        // (499*2 + 299*1) + 49 visiting fee
        assertEquals(499.0 * 2 + 299.0 + ServiceRequestService.VISITING_FEE, saved.getFinalPrice());
        assertEquals("Cart Cleaning", saved.getItems().get(0).getServiceName());
        assertEquals(499.0, saved.getItems().get(0).getUnitPrice());
        // Primary service stays first line for old UIs.
        assertEquals("Cart Cleaning", saved.getService().getName());
    }

    @Test
    void quoteMatchesBookingTotal() {
        Service cleaning = serviceRepository.save(
                new Service(null, "Quote Cleaning", "desc", 400.0, null, null, true));
        Map<String, Object> quote = requestService.quoteCart(List.of(line(cleaning, 3)));
        assertEquals(400.0 * 3, (Double) quote.get("subtotal"));
        assertEquals(400.0 * 3 + ServiceRequestService.VISITING_FEE, (Double) quote.get("total"));
    }

    @Test
    void invalidQuantitiesRejected() {
        Service cleaning = serviceRepository.save(
                new Service(null, "Qty Cleaning", "desc", 400.0, null, null, true));
        ServiceRequest request = new ServiceRequest();
        bindTestAccount(request, "qty@test.com");
        request.setItems(new ArrayList<>(List.of(line(cleaning, 0))));
        request.setAddress("addr");
        request.setScheduledDate(LocalDate.now().plusDays(1));
        request.setScheduledSlot("10:00-12:00");
        assertThrows(IllegalArgumentException.class, () -> requestService.createRequest(request));
        assertThrows(IllegalArgumentException.class,
                () -> requestService.quoteCart(List.of(line(cleaning, 11))));
    }
}
