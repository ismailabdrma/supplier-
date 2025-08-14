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
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            log.info("Creating checkout session for product {} with quantity {}", productId, quantity);

            String sessionUrl = paymentService.createCheckoutSession(productId, quantity);
            return ResponseEntity.ok(Map.of("url", sessionUrl));
        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating checkout session: {}", e.getMessage());
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
}
