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
    public String createCheckoutSession(Long productId, Integer quantity) throws StripeException {
        log.info("Creating checkout session for product {} with quantity {}", productId, quantity);

        Optional<Product> productOpt = productService.getProductById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        Product product = productOpt.get();

        // Check stock availability
        Optional<Integer> availableStock = productService.getAvailableStock(productId);
        if (availableStock.isEmpty() || availableStock.get() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity((long) quantity)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount((long) (product.getPrice() * 100))
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
                .build();

        Session session = Session.create(params);

        // Save payment record
        Payment payment = new Payment();
        payment.setProductId(productId);
        payment.setAmount(product.getPrice() * quantity);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setStripeSessionId(session.getId());
        payment.setQuantity(quantity);
        paymentRepository.save(payment);

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