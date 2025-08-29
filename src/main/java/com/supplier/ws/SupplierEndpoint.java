package com.supplier.ws;

import com.supplier.entity.Product;
import com.supplier.entity.Payment;
import com.supplier.service.PaymentService;
import com.supplier.service.ProductService;
import com.supplier.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Import all generated JAXB classes
import com.supplier.ws.GetProductByIdRequest;
import com.supplier.ws.GetProductByIdResponse;
import com.supplier.ws.GetAvailableStockRequest;
import com.supplier.ws.GetAvailableStockResponse;
import com.supplier.ws.NotifyPaymentStatusRequest;
import com.supplier.ws.NotifyPaymentStatusResponse;
import com.supplier.ws.GetAllProductsRequest;
import com.supplier.ws.GetAllProductsResponse;
import com.supplier.ws.ProcessPaymentRequest;
import com.supplier.ws.ProcessPaymentResponse;

@Endpoint
@Slf4j
public class SupplierEndpoint {

    private static final String NAMESPACE_URI = "http://supplier.com/ws";
    private final ProductService productService;
    private final PaymentService paymentService;
    private final ImageService imageService;

    public SupplierEndpoint(ProductService productService, PaymentService paymentService, ImageService imageService) {
        this.productService = productService;
        this.paymentService = paymentService;
        this.imageService = imageService;
    }

    // 1. Get Product By ID
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getProductByIdRequest")
    @ResponsePayload
    public GetProductByIdResponse getProductById(@RequestPayload GetProductByIdRequest request) {
        log.info("SOAP request: getProductById for id {}", request.getId());
        GetProductByIdResponse response = new GetProductByIdResponse();
        Optional<Product> productOpt = productService.getProductByIdWithStock(request.getId());
        productOpt.ifPresent(product -> response.setProduct(convertToWsProduct(product)));
        return response;
    }

    // 2. Get Available Stock
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getAvailableStockRequest")
    @ResponsePayload
    public GetAvailableStockResponse getAvailableStock(@RequestPayload GetAvailableStockRequest request) {
        log.info("SOAP request: getAvailableStock for product {}", request.getProductId());
        GetAvailableStockResponse response = new GetAvailableStockResponse();
        Optional<Integer> stock = productService.getAvailableStock(request.getProductId());
        response.setQuantity(stock.orElse(0));
        return response;
    }

    // 3. Notify Payment Status
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "notifyPaymentStatusRequest")
    @ResponsePayload
    public NotifyPaymentStatusResponse notifyPaymentStatus(@RequestPayload NotifyPaymentStatusRequest request) {
        log.info("SOAP request: notifyPaymentStatus for payment {} with status {}", request.getPaymentId(), request.getStatus());
        NotifyPaymentStatusResponse response = new NotifyPaymentStatusResponse();
        try {
            paymentService.updatePaymentStatus(request.getPaymentId(), request.getStatus());
            response.setSuccess(true);
            response.setMessage("Payment status updated successfully");
        } catch (Exception e) {
            log.error("Error updating payment status: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("Error updating payment status: " + e.getMessage());
        }
        return response;
    }

    // 4. Get All Products
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getAllProductsRequest")
    @ResponsePayload
    public GetAllProductsResponse getAllProducts(@RequestPayload GetAllProductsRequest request) {
        log.info("SOAP request: getAllProducts");
        GetAllProductsResponse response = new GetAllProductsResponse();
        List<Product> products = productService.getAllProducts();
        List<com.supplier.ws.Product> wsProducts = products.stream()
                .map(this::convertToWsProduct)
                .collect(Collectors.toList());
        response.getProducts().addAll(wsProducts);
        return response;
    }

    // 5. Process Payment
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "processPaymentRequest")
    @ResponsePayload
    public ProcessPaymentResponse processPayment(@RequestPayload ProcessPaymentRequest request) {
        log.info("SOAP processPayment for product {} x {} with amount {} {}", 
                request.getProductId(), request.getQuantity(), request.getAmount(), request.getCurrency());
        ProcessPaymentResponse response = new ProcessPaymentResponse();
        try {
            String sessionUrl = paymentService.createCheckoutSession(
                request.getProductId(), 
                request.getQuantity(), 
                request.getAmount(), 
                request.getCurrency(), 
                request.getOrderId()
            );
            Optional<Payment> paymentOpt = paymentService.getPaymentByProductAndQuantity(request.getProductId(), request.getQuantity());
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                response.setPaymentId(payment.getId());
                response.setStatus(payment.getStatus().name());
                response.setStripeSessionId(payment.getStripeSessionId());
                response.setUrl(sessionUrl);
                response.setMessage("Checkout session created successfully");
            } else {
                response.setStatus("PENDING");
                response.setMessage("Checkout session created, payment pending");
            }
        } catch (Exception e) {
            log.error("SOAP processPayment error: {}", e.getMessage());
            response.setStatus("FAILED");
            response.setMessage("Failed to create payment: " + e.getMessage());
        }
        return response;
    }

    // Helper method to convert entity to JAXB object
    private com.supplier.ws.Product convertToWsProduct(Product entityProduct) {
        com.supplier.ws.Product wsProduct = new com.supplier.ws.Product();
        wsProduct.setId(entityProduct.getId());
        wsProduct.setName(entityProduct.getName());
        wsProduct.setDescription(entityProduct.getDescription());
        wsProduct.setSupplierPrice(entityProduct.getPrice());
        wsProduct.setDisplayedPrice(entityProduct.getPrice());
        wsProduct.setAvailableQuantity(entityProduct.getAvailableQuantity());

        // Handle optional picture URL and data
        if(entityProduct.getPictureUrl() != null) {
            wsProduct.setPictureUrl(entityProduct.getPictureUrl());
            
            // Get raw image bytes for SOAP clients (JAXB will handle base64 encoding automatically)
            byte[] imageBytes = imageService.getImageBytes(entityProduct.getPictureUrl());
            if (imageBytes != null) {
                wsProduct.setPictureData(imageBytes);
                
                // Set the image format
                String format = imageService.getImageFormat(entityProduct.getPictureUrl());
                wsProduct.setPictureFormat(format);
            }
        }

        wsProduct.setRealTimeStock(entityProduct.getRealTimeStock());

        // Convert timestamp to string
        if(entityProduct.getLastStockUpdate() != null) {
            wsProduct.setLastStockUpdate(entityProduct.getLastStockUpdate().toString());
        }

        return wsProduct;
    }
}