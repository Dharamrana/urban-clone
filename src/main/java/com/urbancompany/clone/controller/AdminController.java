package com.urbancompany.clone.controller;

import com.urbancompany.clone.repository.ServiceProviderRepository;
import com.urbancompany.clone.repository.ServiceRequestRepository;
import com.urbancompany.clone.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    private final UserRepository userRepository;
    private final ServiceProviderRepository providerRepository;
    private final ServiceRequestRepository requestRepository;

    public AdminController(UserRepository userRepository,
                           ServiceProviderRepository providerRepository,
                           ServiceRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.requestRepository = requestRepository;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        var requests = requestRepository.findAll();
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("providers", providerRepository.findAll());
        model.addAttribute("requests", requests);
        model.addAttribute("pendingVerification", providerRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsVerified())).count());
        model.addAttribute("paidBookings", requests.stream()
                .filter(r -> "PAID".equalsIgnoreCase(r.getPaymentStatus())).count());
        model.addAttribute("totalRevenue", requests.stream()
                .filter(r -> "PAID".equalsIgnoreCase(r.getPaymentStatus()))
                .map(r -> r.getFinalPrice() == null ? 0.0 : r.getFinalPrice())
                .reduce(0.0, Double::sum));
        return "admin-dashboard";
    }
}
