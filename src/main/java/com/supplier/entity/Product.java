package com.supplier.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;


    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(length = 500)
    private String pictureUrl;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Stock stock;

    // Helper method to get real-time stock
    @Transient
    public Integer getRealTimeStock() {
        if (this.stock != null && this.stock.getQuantity() != null) {
            return this.stock.getQuantity();
        }
        return this.availableQuantity;
    }

    // Helper method to get last stock update
    @Transient
    public java.time.LocalDateTime getLastStockUpdate() {
        if (this.stock != null && this.stock.getLastUpdated() != null) {
            return this.stock.getLastUpdated();
        }
        return null;
    }
}
