package com.supplier.service;

import com.supplier.entity.Product;
import com.supplier.entity.Stock;
import com.supplier.repository.ProductRepository;
import com.supplier.repository.StockRepository;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@WebService(serviceName = "ProductWebService")
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    @WebMethod
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    @WebMethod
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    @WebMethod
    @Transactional
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getName());
        Product savedProduct = productRepository.save(product);

        // Create initial stock entry
        Stock stock = new Stock();
        stock.setProduct(savedProduct);
        stock.setQuantity(savedProduct.getAvailableQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);

        return savedProduct;
    }
    @WebMethod
    @Transactional
    public Optional<Product> updateStock(Long productId, Integer newQuantity) {
        log.info("Updating stock for product {} to quantity {}", productId, newQuantity);

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setAvailableQuantity(newQuantity);
            productRepository.save(product);

            // Update stock table
            Optional<Stock> stockOpt = stockRepository.findByProductId(productId);
            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                stock.setQuantity(newQuantity);
                stock.setLastUpdated(LocalDateTime.now());
                stockRepository.save(stock);
            }

            return Optional.of(product);
        }
        return Optional.empty();
    }

    public Optional<Integer> getAvailableStock(Long productId) {
        Optional<Stock> stock = stockRepository.findByProductId(productId);
        return stock.map(Stock::getQuantity);
    }
    @WebMethod
    @Transactional
    public boolean reduceStock(Long productId, Integer quantity) {
        log.info("Reducing stock for product {} by {}", productId, quantity);

        Optional<Stock> stockOpt = stockRepository.findByProductId(productId);
        if (stockOpt.isPresent()) {
            Stock stock = stockOpt.get();
            if (stock.getQuantity() >= quantity) {
                stock.setQuantity(stock.getQuantity() - quantity);
                stock.setLastUpdated(LocalDateTime.now());
                stockRepository.save(stock);

                // Update product available quantity
                Product product = stock.getProduct();
                product.setAvailableQuantity(stock.getQuantity());
                productRepository.save(product);

                return true;
            }
        }
        return false;
    }
    @WebMethod
    @Transactional(readOnly = true)
    public List<Product> getAllProductsWithStock() {
        log.info("Fetching all products with real-time stock information");
        List<Product> products = productRepository.findAll();

        // Ensure stock information is loaded
        products.forEach(product -> {
            if (product.getStock() != null) {
                product.getStock().getQuantity();
            }
        });

        return products;
    }

    public List<Product> getAllProductsWithStockForAdmin() {
        return productRepository.findAll();
    }

    // Get products pending approval
    public List<Product> getPendingApprovalProducts() {
        return productRepository.findByApprovedFalse();
    }

    // Get inactive products
    public List<Product> getInactiveProducts() {
        return productRepository.findByActiveFalse();
    }

    // Approve a product
    @Transactional
    public boolean approveProduct(Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setApproved(true);
            productRepository.save(product);
            log.info("Product {} approved by admin", productId);
            return true;
        }
        return false;
    }

    // Deactivate a product (soft delete)
    @Transactional
    public boolean deactivateProduct(Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setActive(false);
            productRepository.save(product);
            log.info("Product {} deactivated by admin", productId);
            return true;
        }
        return false;
    }

    // Delete a product permanently (hard delete)
    @Transactional
    public boolean deleteProduct(Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            
            // First deactivate if not already deactivated
            if (product.getActive()) {
                product.setActive(false);
                productRepository.save(product);
                log.info("Product {} deactivated before deletion", productId);
            }
            
            // Now delete permanently
            productRepository.delete(product);
            log.info("Product {} permanently deleted by admin", productId);
            return true;
        }
        return false;
    }
    @WebMethod
    @Transactional(readOnly = true)
    public Optional<Product> getProductByIdWithStock(Long id) {
        log.info("Fetching product {} with real-time stock information", id);
        Optional<Product> productOpt = productRepository.findById(id);

        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (product.getStock() != null) {
                product.getStock().getQuantity();
            }
        }

        return productOpt;
    }
}
