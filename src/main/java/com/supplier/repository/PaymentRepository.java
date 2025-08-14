package com.supplier.repository;

import com.supplier.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findTopByProductIdAndQuantityOrderByTimestampDesc(Long productId, Integer quantity);
    Optional<Payment> findByStripeSessionId(String sessionId);
    Optional<Payment> findTopByProductIdAndQuantityOrderByCreatedAtDesc(Long productId, Integer quantity);
}
