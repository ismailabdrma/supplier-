package com.supplier.controller;

import com.supplier.entity.Product;
import com.supplier.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ProductController {

    private final ProductService productService;

    @Value("${upload.dir}")
    private String uploadDir;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Fetching all products");
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        log.info("Fetching product with id: {}", id);
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        log.info("Creating new product: {}", product.getName());
        if (product.getPictureUrl() == null || product.getPictureUrl().trim().isEmpty()) {
            product.setPictureUrl("https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=400");
        }
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.ok(createdProduct);
    }

    @PutMapping("/stock/{productId}")
    public ResponseEntity<Product> updateStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> request) {
        Integer newQuantity = request.get("quantity");
        log.info("Updating stock for product {} to {}", productId, newQuantity);
        Optional<Product> updatedProduct = productService.updateStock(productId, newQuantity);
        return updatedProduct.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<Map<String, Integer>> getStock(@PathVariable Long productId) {
        Optional<Integer> stock = productService.getAvailableStock(productId);
        return stock.map(s -> ResponseEntity.ok(Map.of("quantity", s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/with-stock")
    public ResponseEntity<List<Product>> getAllProductsWithStock() {
        log.info("Fetching all products with real-time stock information");
        List<Product> products = productService.getAllProductsWithStock();
        return ResponseEntity.ok(products);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam("availableQuantity") Integer availableQuantity,
            @RequestParam("productImage") MultipartFile productImage
    ) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String filename = UUID.randomUUID() + "_" + Objects.requireNonNull(productImage.getOriginalFilename());
            Path filePath = uploadPath.resolve(filename);
            productImage.transferTo(filePath.toFile());
            String pictureUrl = "/uploads/" + filename;

            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price); // Only one price
            product.setAvailableQuantity(availableQuantity);
            product.setPictureUrl(pictureUrl);

            Product createdProduct = productService.createProduct(product);
            return ResponseEntity.ok(createdProduct);

        } catch (IOException e) {
            log.error("Image upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

}