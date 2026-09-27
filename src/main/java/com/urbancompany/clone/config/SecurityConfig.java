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

import java.util.LinkedHashMap;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
        var apiEntryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Please log in first\"}");
        };
        var entryPoints = new LinkedHashMap<org.springframework.security.web.util.matcher.RequestMatcher,
                org.springframework.security.web.AuthenticationEntryPoint>();
        entryPoints.put(new AntPathRequestMatcher("/api/**"), apiEntryPoint);
        var delegatingEntryPoint = new org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint(entryPoints);
        delegatingEntryPoint.setDefaultEntryPoint(loginEntryPoint);

        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(e -> e.authenticationEntryPoint(delegatingEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/services", "/providers", "/provider/**", "/about", "/contact",
                                "/login", "/signup", "/css/**", "/js/**", "/h2-console/**", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/services/**", "/api/providers/**", "/api/requests/slots").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/requests/quote").permitAll()
                        .requestMatchers("/admin", "/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/provider-portal", "/provider-portal/**", "/api/provider/**").hasRole("PROVIDER")
                        .requestMatchers("/map").authenticated()
                        .requestMatchers("/request", "/requests", "/api/requests", "/api/requests/**", "/api/users/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/?logout").permitAll());
        return http.build();
    }
}
