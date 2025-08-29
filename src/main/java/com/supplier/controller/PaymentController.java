package com.supplier.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.supplier.entity.Payment;
import com.supplier.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // Inject from application.properties
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received checkout session request: {}", request);
            
            // Validate required parameters
            if (request == null) {
                log.error("Request body is null");
                return ResponseEntity.badRequest().body(Map.of("error", "Request body cannot be null"));
            }
            
            if (!request.containsKey("productId")) {
                log.error("Missing productId in request: {}", request);
                return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
            }
            
            if (!request.containsKey("quantity")) {
                log.error("Missing quantity in request: {}", request);
                return ResponseEntity.badRequest().body(Map.of("error", "quantity is required"));
            }
            
            // Extract required parameters
            Long productId;
            Integer quantity;
            
            try {
                productId = Long.valueOf(request.get("productId").toString());
                log.info("Extracted productId: {}", productId);
            } catch (NumberFormatException e) {
                log.error("Invalid productId format: {}", request.get("productId"));
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid productId format"));
            }
            
            try {
                quantity = Integer.valueOf(request.get("quantity").toString());
                log.info("Extracted quantity: {}", quantity);
            } catch (NumberFormatException e) {
                log.error("Invalid quantity format: {}", request.get("quantity"));
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid quantity format"));
            }
            
            // Validate quantity
            if (quantity <= 0) {
                log.error("Invalid quantity: {}", quantity);
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be greater than 0"));
            }
            
            // Generate order ID if not provided
            String orderId = request.get("orderId") != null ? 
                request.get("orderId").toString() : 
                "ORDER_" + System.currentTimeMillis();
            
            // Use default currency if not provided
            String currency = request.get("currency") != null ? 
                request.get("currency").toString() : 
                "USD";
            
            // Calculate amount from product price and quantity (will be handled by service)
            Double amount = null; // Will be calculated by service based on product price
            
            log.info("Creating checkout session for product {} with quantity {} and orderId {}", 
                    productId, quantity, orderId);

            String sessionUrl = paymentService.createCheckoutSession(productId, quantity, amount, currency, orderId);
            log.info("Checkout session created successfully: {}", sessionUrl);
            return ResponseEntity.ok(Map.of("url", sessionUrl));
        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating checkout session: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // SECURE Stripe webhook endpoint
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Received Stripe webhook event");
        Event event;
        try {
            // Directly use Webhook.constructEvent (not Event.constructFrom!)
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("⚠️  Webhook signature verification failed.", e);
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            log.error("⚠️  Error parsing webhook payload", e);
            return ResponseEntity.status(400).body("Invalid payload");
        }

        // Handle event types you care about
        if ("checkout.session.completed".equals(event.getType())) {
            // Deserialize object to Session
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                log.info("Payment Success! Session ID: {}", session.getId());
                paymentService.handleSuccessfulPayment(session.getId());
            }
        }

        return ResponseEntity.ok("Received");
    }

    @PostMapping("/success/{sessionId}")
    public ResponseEntity<String> handleSuccess(@PathVariable String sessionId) {
        log.info("Handling payment success for session: {}", sessionId);
        paymentService.handleSuccessfulPayment(sessionId);
        return ResponseEntity.ok("Payment processed successfully");
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        try {
            // Try to fetch a simple count to verify database connection
            List<Payment> payments = paymentService.getAllPayments();
            return ResponseEntity.ok(Map.of("status", "healthy", "database", "connected", "payments_count", String.valueOf(payments.size())));
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("status", "unhealthy", "error", e.getMessage()));
        }
    }
}
