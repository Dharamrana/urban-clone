package com.urbancompany.clone.controller;

import com.urbancompany.clone.repository.ServiceProviderRepository;
import com.urbancompany.clone.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/** Exposes the logged-in account (customer or professional) to every Thymeleaf page. */
@ControllerAdvice
public class CurrentUserAdvice {

    private final UserService userService;
    private final ServiceProviderRepository providerRepository;

    public CurrentUserAdvice(UserService userService, ServiceProviderRepository providerRepository) {
        this.userService = userService;
        this.providerRepository = providerRepository;
    }

    @ModelAttribute("currentUser")
    public Map<String, Object> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        boolean isProvider = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"));
        if (isProvider) {
            return providerRepository.findByEmail(auth.getName())
                    .map(p -> Map.<String, Object>of(
                            "id", p.getId(), "name", p.getName(), "email", p.getEmail(), "role", "PROVIDER"))
                    .orElse(null);
        }
        return userService.getUserByEmail(auth.getName())
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", "CUSTOMER"))
                .orElse(null);
    }
}
