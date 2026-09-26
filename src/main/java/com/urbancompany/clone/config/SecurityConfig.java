package com.urbancompany.clone.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // API clients get JSON 401s; browser pages get the login redirect (UC-style).
        LoginUrlAuthenticationEntryPoint loginEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login");
        var apiEntryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Please log in first\"}");
        };
        var entryPoint = new org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint(
                new java.util.LinkedHashMap<>(Map.of(
                        new AntPathRequestMatcher("/api/**"), apiEntryPoint)));
        entryPoint.setDefaultEntryPoint(loginEntryPoint);

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/services", "/providers", "/provider/**",
                                "/about", "/contact", "/login", "/signup",
                                "/css/**", "/js/**", "/h2-console/**",
                                "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/services/**", "/api/providers/**",
                                "/api/requests/slots").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/requests/quote").permitAll()
                        .requestMatchers("/request", "/requests").authenticated()
                        .requestMatchers("/provider-portal", "/provider-portal/**").hasRole("PROVIDER")
                        .requestMatchers("/api/provider/**").hasRole("PROVIDER")
                        .requestMatchers(HttpMethod.POST, "/api/requests").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/requests/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/requests/**").authenticated()
                        .requestMatchers("/api/requests", "/api/requests/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .permitAll());
        return http.build();
    }
}
