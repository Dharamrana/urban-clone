package com.urbancompany.clone.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "service_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Column(unique = true)
    private String email;

    private String phone;

    /** BCrypt hash; accepted on create/update, never serialized. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Always PROVIDER for this entity (used for role-based login + portal). */
    private String role = "PROVIDER";

    @Embedded
    private Location location;

    private Double rating = 0.0;

    private Integer totalReviews = 0;

    private Boolean isAvailable = true;

    private Boolean isVerified = false;

    private String profileImageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "provider_services", joinColumns = @JoinColumn(name = "provider_id"))
    @Column(name = "service_id")
    private List<Long> serviceIds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "provider_certifications", joinColumns = @JoinColumn(name = "provider_id"))
    @Column(name = "certification")
    private List<String> certifications;

    private Integer experienceYears;

    @Column(columnDefinition = "TEXT")
    private String bio;
}
