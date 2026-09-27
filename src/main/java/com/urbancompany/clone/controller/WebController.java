package com.urbancompany.clone.controller;

import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.model.ServiceRequest;
import com.urbancompany.clone.model.User;
import com.urbancompany.clone.service.ProviderWithDistance;
import com.urbancompany.clone.service.ServiceProviderService;
import com.urbancompany.clone.service.ServiceRequestService;
import com.urbancompany.clone.service.ServiceService;
import com.urbancompany.clone.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class WebController {

    private final ServiceService serviceService;
    private final ServiceProviderService serviceProviderService;
    private final ServiceRequestService serviceRequestService;
    private final UserService userService;

    public WebController(ServiceService serviceService,
                         ServiceProviderService serviceProviderService,
                         ServiceRequestService serviceRequestService,
                         UserService userService) {
        this.serviceService = serviceService;
        this.serviceProviderService = serviceProviderService;
        this.serviceRequestService = serviceRequestService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("services", serviceService.getAllActiveServices());
        return "index";
    }

    @GetMapping("/services")
    public String servicesPage(Model model) {
        model.addAttribute("services", serviceService.getAllActiveServices());
        return "services";
    }

    @GetMapping("/providers")
    public String providersPage(@RequestParam Long serviceId,
                                @RequestParam(defaultValue = "28.6139") Double lat,
                                @RequestParam(defaultValue = "77.2090") Double lng,
                                @RequestParam(defaultValue = "10") Integer limit,
                                Model model) {
        List<ProviderWithDistance> providers = serviceProviderService.getNearestProvidersByService(serviceId, lat, lng, limit);
        model.addAttribute("service", serviceService.getServiceById(serviceId).orElse(null));
        model.addAttribute("providers", providers);
        model.addAttribute("lat", lat);
        model.addAttribute("lng", lng);
        return "providers";
    }

    @GetMapping("/provider/{id}")
    public String providerDetail(@PathVariable Long id, @RequestParam(required = false) Long serviceId, Model model) {
        ServiceProvider provider = serviceProviderService.getProviderById(id).orElse(null);
        if (provider == null) return "redirect:/";
        model.addAttribute("provider", provider);
        Long effectiveServiceId = serviceId;
        if (effectiveServiceId == null && provider.getServiceIds() != null && !provider.getServiceIds().isEmpty()) {
            effectiveServiceId = provider.getServiceIds().get(0);
        }
        model.addAttribute("serviceId", effectiveServiceId);
        return "provider-detail";
    }

    @GetMapping("/request")
    public String requestPage(Model model) {
        model.addAttribute("services", serviceService.getAllActiveServices());
        return "request-service";
    }

    @GetMapping("/requests")
    public String requestsPage(Authentication authentication, Model model) {
        List<ServiceRequest> requests = List.of();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            User me = userService.getUserByEmail(authentication.getName()).orElse(null);
            if (me != null) requests = serviceRequestService.getRequestsByUser(me.getId());
        }
        model.addAttribute("requests", requests);
        return "requests";
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/signup")
    public String signupPage() { return "signup"; }

    @GetMapping("/map")
    public String mapPage(Model model) {
        model.addAttribute("providers", serviceProviderService.getAllProviders());
        return "map";
    }

    @GetMapping("/provider-portal")
    public String providerPortal(Authentication authentication, Model model) {
        if (authentication == null || !"PROVIDER".equals(roleOf(authentication))) return "redirect:/login";
        ServiceProvider me = serviceProviderService.getAllProviders().stream()
                .filter(p -> authentication.getName().equalsIgnoreCase(p.getEmail())).findFirst().orElse(null);
        if (me == null) return "redirect:/login";
        model.addAttribute("provider", me);
        model.addAttribute("jobs", serviceRequestService.getRequestsByProvider(me.getId()));
        model.addAttribute("pool", serviceRequestService.getAvailablePool(me.getEmail()));
        model.addAttribute("earnings", serviceRequestService.providerEarnings(me.getEmail()));
        return "provider-portal";
    }

    @GetMapping("/pay/{id}")
    public String payPage(@PathVariable Long id, Authentication authentication, Model model) {
        ServiceRequest request = serviceRequestService.getRequestById(id).orElse(null);
        if (request == null) return "redirect:/requests";
        String me = authentication != null ? authentication.getName() : null;
        if (me == null || request.getUser() == null || !me.equalsIgnoreCase(request.getUser().getEmail())) return "redirect:/requests";
        model.addAttribute("booking", request);
        return "pay";
    }

    private String roleOf(Authentication authentication) {
        if (authentication == null) return "";
        return authentication.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).findFirst().orElse("");
    }

    @GetMapping("/about") public String aboutPage() { return "about"; }
    @GetMapping("/contact") public String contactPage() { return "contact"; }
}
