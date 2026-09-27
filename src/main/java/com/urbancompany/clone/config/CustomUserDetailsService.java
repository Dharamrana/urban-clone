package com.urbancompany.clone.config;

import com.urbancompany.clone.model.User;
import com.urbancompany.clone.repository.ServiceProviderRepository;
import com.urbancompany.clone.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ServiceProviderRepository providerRepository;

    public CustomUserDetailsService(UserRepository userRepository, ServiceProviderRepository providerRepository) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            String role = user.getRole() == null ? "CUSTOMER" : user.getRole();
            return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword() == null ? "" : user.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        return providerRepository.findByEmail(email)
                .map(p -> new org.springframework.security.core.userdetails.User(p.getEmail(), p.getPassword() == null ? "" : p.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_PROVIDER"))))
                .orElseThrow(() -> new UsernameNotFoundException("No account with email: " + email));
    }
}
