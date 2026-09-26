package com.urbancompany.clone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_request_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    /** Snapshot of the service name at booking time. */
    private String serviceName;

    /** Price per unit captured at booking time (UC price lock). */
    private Double unitPrice;

    /** Number of units (e.g. 2 bathrooms, 3 rooms). */
    private Integer quantity = 1;
}
