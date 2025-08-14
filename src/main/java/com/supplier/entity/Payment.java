package com.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private Double amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(unique = true)
    private String stripeSessionId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private Integer quantity = 1;
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }
    
    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}
