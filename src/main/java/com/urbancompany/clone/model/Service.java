package com.urbancompany.clone.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Service name is required")
    @Size(min = 3, max = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double basePrice;

    private String iconUrl;

    private String photoUrl;

    private Boolean isActive = true;
}
