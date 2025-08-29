package com.supplier.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.supplier.entity.Payment;
import com.supplier.entity.Product;
import com.supplier.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProductService productService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Creates a Stripe checkout session, saves the Payment, returns checkout URL.
     */
    @Transactional
    public String createCheckoutSession(Long productId, Integer quantity, Double amount, String currency, String orderId) throws StripeException {
        log.info("Creating checkout session for product {} with quantity {} and amount {} {}", productId, quantity, amount, currency);

        // Check database connection by trying to access the repository
        try {
            long paymentCount = paymentRepository.count();
            log.info("Database connection verified. Total payments in database: {}", paymentCount);
        } catch (Exception e) {
            log.error("Database connection failed: {}", e.getMessage(), e);
            throw new RuntimeException("Database connection failed: " + e.getMessage());
        }

        Optional<Product> productOpt = productService.getProductById(productId);
        if (productOpt.isEmpty()) {
            log.error("Product not found with ID: {}", productId);
            throw new RuntimeException("Product not found");
        }

        Product product = productOpt.get();
        log.info("Found product: {} with price: {}", product.getName(), product.getPrice());

        // Calculate amount from product price if not provided
        if (amount == null) {
            amount = product.getPrice() * quantity;
            log.info("Calculated amount from product price: {} * {} = {}", product.getPrice(), quantity, amount);
        }

        // Check stock availability
        Optional<Integer> availableStock = productService.getAvailableStock(productId);
        if (availableStock.isEmpty() || availableStock.get() < quantity) {
            log.error("Insufficient stock for product {}: requested {}, available {}", productId, quantity, availableStock.orElse(null));
            throw new RuntimeException("Insufficient stock");
        }
        
        log.info("Stock check passed: requested {}, available {}", quantity, availableStock.get());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity((long) quantity)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency.toLowerCase())
                                                .setUnitAmount((long) (amount * 100))
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(product.getName())
                                                                .setDescription(product.getDescription())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("product_id", productId.toString())
                .putMetadata("quantity", quantity.toString())
                .putMetadata("order_id", orderId)
                .build();

        Session session = Session.create(params);
        log.info("Stripe session created successfully with ID: {}", session.getId());

        // Save payment record
        try {
            Payment payment = new Payment();
            payment.setProductId(productId);
            payment.setOrderId(orderId);
            payment.setAmount(amount);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setStripeSessionId(session.getId());
            payment.setQuantity(quantity);
            payment.setCurrency(currency);
            
            log.info("Payment object created: {}", payment);
            
            paymentRepository.save(payment);
            log.info("Payment record saved successfully with ID: {}", payment.getId());
        } catch (Exception e) {
            log.error("Error saving payment record: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save payment record: " + e.getMessage());
        }

        return session.getUrl();
    }

    @Transactional
    public void handleSuccessfulPayment(String sessionId) {
        log.info("Handling successful payment for session: {}", sessionId);

        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(sessionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            // Reduce stock
            productService.reduceStock(payment.getProductId(), payment.getQuantity());

            log.info("Payment completed and stock reduced for product: {}", payment.getProductId());
        }
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    @Transactional
    public void updatePaymentStatus(Long paymentId, String status) {
        log.info("Updating payment {} status to {}", paymentId, status);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            try {
                Payment.PaymentStatus newStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
                payment.setStatus(newStatus);
                paymentRepository.save(payment);

                if (newStatus == Payment.PaymentStatus.SUCCESS) {
                    productService.reduceStock(payment.getProductId(), payment.getQuantity());
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid payment status: {}", status);
                throw new IllegalArgumentException("Invalid payment status: " + status);
            }
        }
    }

    /**
     * Helper for SOAP endpoint: fetch the latest payment by productId and quantity.
     */
    public Optional<Payment> getPaymentByProductAndQuantity(Long productId, Integer quantity) {
        return paymentRepository.findTopByProductIdAndQuantityOrderByCreatedAtDesc(productId, quantity);
    }
}