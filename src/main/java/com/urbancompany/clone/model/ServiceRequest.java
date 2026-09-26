package com.urbancompany.clone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private ServiceProvider provider;

    @Enumerated(EnumType.STRING)
    private ServiceRequestStatus status = ServiceRequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    private Double finalPrice;

    private String address;

    // --- Urban Company style booking fields ---

    /** Day the professional should visit (UC slot booking). */
    private LocalDate scheduledDate;

    /** Time window e.g. "10:00-12:00" (UC slot booking). */
    private String scheduledSlot;

    /** UPI / CARD / CASH (UC checkout payment choice). */
    private String paymentMethod = "UPI";

    /** Fixed visiting fee added on top of service base price (UC price breakup). */
    private Double visitingFee;

    /** 1-5 star rating given after completion (UC rate professional). */
    private Integer rating;

    /** Written review given after completion. */
    @Column(columnDefinition = "TEXT")
    private String review;

    // --- Urban Company style multi-service cart ---
    // One visit can cover several services with quantities (e.g. 2x Bathroom Cleaning).
    // `service` stays as the primary service for backward compatibility.
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "request_id")
    private List<ServiceRequestItem> items = new ArrayList<>();

    // --- Professional job verification (UC: share OTP to start work) ---
    /** 4-digit code the customer shares with the arriving professional. */
    private String startOtp;

    private Boolean otpVerified = false;

    // --- Mock payment gateway state (UC checkout) ---
    /** PENDING / PAID / FAILED / REFUNDED. CASH stays PENDING until collected. */
    private String paymentStatus = "PENDING";

    /** Gateway reference for the (mock) transaction. */
    private String paymentRef;
}
