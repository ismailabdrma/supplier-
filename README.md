# Supplier System MVP

A complete supplier management system built with Spring Boot 3, featuring SOAP web services, Stripe payment integration, and a responsive web interface.

## 🚀 Features

### Backend
- **Spring Boot 3** with layered architecture
- **SOAP Web Services** with WSDL generation
- **REST API** for frontend integration
- **Stripe Payment Integration** with checkout sessions
- **Real-time Stock Management**
- **H2 Database** for development (easily switchable to MySQL/PostgreSQL)
- **JPA Entities** with relationships

### Frontend
- **Responsive Web Interface** using Bootstrap 5
- **Product Management** (add, view, update stock)
- **Stripe Checkout Integration**
- **Payment History Tracking**
- **Real-time Updates**

### Integration
- **SOAP Endpoints** for marketplace integration
- **Payment Webhooks** for automatic processing
- **CORS Enabled** for cross-origin requests

## 🛠️ Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Stripe Account (for payment processing)

### Setup

1. **Clone and Navigate**
   \`\`\`bash
   cd supplier-system
   \`\`\`

2. **Configure Stripe**
   Update `src/main/resources/application.properties`:
   ```properties
   stripe.api.key=sk_test_your_stripe_secret_key
   stripe.webhook.secret=whsec_your_webhook_secret
   \`\`\`

3. **Run Application**
   \`\`\`bash
   mvn spring-boot:run
   \`\`\`

4. **Access Application**
   - Web Interface: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
   - SOAP WSDL: http://localhost:8080/ws/supplier.wsdl

## 📡 API Endpoints

### REST API
- `GET /api/products` - List all products
- `POST /api/products` - Create new product
- `PUT /api/stock/{productId}` - Update stock
- `POST /api/payments/create-checkout-session` - Create Stripe session
- `GET /api/payments` - List payments

### SOAP Web Services
- `getProductById(Long id)` - Get product details
- `getAvailableStock(Long productId)` - Get current stock
- `notifyPaymentStatus(Long paymentId, String status)` - Update payment status

## 💳 Stripe Integration

### Test Cards
Use these test card numbers in Stripe checkout:
- **Success**: 4242424242424242
- **Decline**: 4000000000000002
- **3D Secure**: 4000002500003155

### Webhook Setup
1. Create webhook endpoint in Stripe Dashboard
2. Point to: `https://yourdomain.com/api/payments/webhook`
3. Select event: `checkout.session.completed`

## 🏗️ Architecture

\`\`\`
src/main/java/com/supplier/
├── entity/          # JPA Entities
├── repository/      # Data Access Layer
├── service/         # Business Logic
├── controller/      # REST Controllers
├── ws/             # SOAP Endpoints
└── config/         # Configuration Classes

src/main/resources/
├── static/         # Frontend Files
├── xsd/           # SOAP Schema
└── application.properties
\`\`\`

## 🔌 Marketplace Integration

### SOAP Client Example (Java)
\`\`\`java
// Get product information
GetProductByIdRequest request = new GetProductByIdRequest();
request.setId(1L);
GetProductByIdResponse response = supplierClient.getProductById(request);

// Check stock availability
GetAvailableStockRequest stockRequest = new GetAvailableStockRequest();
stockRequest.setProductId(1L);
GetAvailableStockResponse stockResponse = supplierClient.getAvailableStock(stockRequest);

// Notify payment status
NotifyPaymentStatusRequest paymentRequest = new NotifyPaymentStatusRequest();
paymentRequest.setPaymentId(123L);
paymentRequest.setStatus("SUCCESS");
supplierClient.notifyPaymentStatus(paymentRequest);
\`\`\`

## 🗄️ Database Schema

### Products Table
- `id` (Primary Key)
- `name` (Product Name)
- `description` (Product Description)
- `supplier_price` (Cost Price)
- `displayed_price` (Selling Price)
- `available_quantity` (Current Stock)

### Stock Table
- `id` (Primary Key)
- `product_id` (Foreign Key)
- `quantity` (Available Quantity)
- `last_updated` (Timestamp)

### Payments Table
- `id` (Primary Key)
- `product_id` (Product Reference)
- `amount` (Payment Amount)
- `quantity` (Items Purchased)
- `status` (PENDING/SUCCESS/FAILED)
- `stripe_session_id` (Stripe Reference)
- `timestamp` (Creation Time)

## 🚀 Production Deployment

### Database Migration
Replace H2 with production database:

```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/supplierdb
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
