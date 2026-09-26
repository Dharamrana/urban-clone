package com.urbancompany.clone.controller;

import com.urbancompany.clone.model.User;
import com.urbancompany.clone.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * JSON auth API used by the signup page and booking wizard.
 * Browser form login is handled by Spring Security itself (POST /login).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** Sign up + immediate auto-login (UC-style: one step into booking). */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody User signup, HttpServletRequest request) {
        if (signup.getPassword() == null || signup.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must be at least 6 characters"));
        }
        try {
            signup.setRole("CUSTOMER"); // never trust role from client
            User saved = userService.createUser(signup);
            // Auto-login so the user lands straight in the booking flow.
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    saved.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + (saved.getRole() != null ? saved.getRole() : "CUSTOMER"))));
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return ResponseEntity.ok(publicProfile(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /** Current session profile; 401 when logged out (used to prefill booking + header). */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "Not logged in"));
        }
        return userService.getUserByEmail(authentication.getName())
                .map(u -> ResponseEntity.ok(publicProfile(u)))
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Account not found")));
    }

    private Map<String, Object> publicProfile(User u) {
        return Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "phone", u.getPhone() != null ? u.getPhone() : "");
    }
}
