//
// Ce fichier a été généré par Eclipse Implementation of JAXB, v3.0.0 
// Voir https://eclipse-ee4j.github.io/jaxb-ri 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2025.08.17 à 04:54:19 PM GMT+01:00 
//


package com.supplier.ws;

import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.supplier.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.supplier.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ProcessPaymentRequest }
     * 
     */
    public ProcessPaymentRequest createProcessPaymentRequest() {
        return new ProcessPaymentRequest();
    }

    /**
     * Create an instance of {@link ProcessPaymentResponse }
     * 
     */
    public ProcessPaymentResponse createProcessPaymentResponse() {
        return new ProcessPaymentResponse();
    }

    /**
     * Create an instance of {@link GetProductByIdRequest }
     * 
     */
    public GetProductByIdRequest createGetProductByIdRequest() {
        return new GetProductByIdRequest();
    }

    /**
     * Create an instance of {@link GetProductByIdResponse }
     * 
     */
    public GetProductByIdResponse createGetProductByIdResponse() {
        return new GetProductByIdResponse();
    }

    /**
     * Create an instance of {@link Product }
     * 
     */
    public Product createProduct() {
        return new Product();
    }

    /**
     * Create an instance of {@link GetAvailableStockRequest }
     * 
     */
    public GetAvailableStockRequest createGetAvailableStockRequest() {
        return new GetAvailableStockRequest();
    }

    /**
     * Create an instance of {@link GetAvailableStockResponse }
     * 
     */
    public GetAvailableStockResponse createGetAvailableStockResponse() {
        return new GetAvailableStockResponse();
    }

    /**
     * Create an instance of {@link NotifyPaymentStatusRequest }
     * 
     */
    public NotifyPaymentStatusRequest createNotifyPaymentStatusRequest() {
        return new NotifyPaymentStatusRequest();
    }

    /**
     * Create an instance of {@link NotifyPaymentStatusResponse }
     * 
     */
    public NotifyPaymentStatusResponse createNotifyPaymentStatusResponse() {
        return new NotifyPaymentStatusResponse();
    }

    /**
     * Create an instance of {@link GetAllProductsRequest }
     * 
     */
    public GetAllProductsRequest createGetAllProductsRequest() {
        return new GetAllProductsRequest();
    }

    /**
     * Create an instance of {@link GetAllProductsResponse }
     * 
     */
    public GetAllProductsResponse createGetAllProductsResponse() {
        return new GetAllProductsResponse();
    }

}
