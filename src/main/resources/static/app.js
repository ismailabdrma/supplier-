const API_BASE = "/api";
const bootstrap = window.bootstrap;

document.addEventListener("DOMContentLoaded", () => {
    // Show "Add Product" by default
    showSection('add-product');
    loadProducts();

    // Bind form event only if form exists
    const form = document.getElementById("add-product-form");
    if (form) {
        form.addEventListener("submit", (e) => {
            e.preventDefault();
            addProduct();
        });
    }
});

function showSection(sectionName) {
    document.querySelectorAll(".section").forEach((section) => {
        section.style.display = "none";
    });
    document.getElementById(sectionName + "-section").style.display = "block";
    if (sectionName === "products") loadProducts();
    else if (sectionName === "payments") loadPayments();
}

function loadProducts() {
    fetch(`${API_BASE}/products`)
        .then((response) => response.json())
        .then((products) => displayProducts(products))
        .catch((error) => {
            console.error("Error loading products:", error);
            showAlert("Error loading products", "danger");
        });
}

function displayProducts(products) {
    const container = document.getElementById("products-container");
    if (products.length === 0) {
        container.innerHTML = '<div class="col-12"><div class="alert alert-info">No products found.</div></div>';
        return;
    }

    container.innerHTML = products.map(product => {
        const imageUrl = product.pictureUrl
            ? (product.pictureUrl.startsWith('/')
                ? product.pictureUrl
                : '/uploads/' + product.pictureUrl)
            : null;

        return `
    <div class="col-md-4 mb-4">
      <div class="card product-card h-100 position-relative">
        <span class="badge bg-${product.availableQuantity > 0 ? "success" : "danger"} stock-badge">
          Stock: ${product.availableQuantity}
        </span>
        ${imageUrl ? `
          <img src="${imageUrl}" 
               class="card-img-top" 
               alt="${product.name}" 
               style="height: 200px; object-fit: cover;"
               onerror="this.onerror=null; this.src='https://via.placeholder.com/300?text=Image+Not+Found'">
        ` : `
          <div class="card-img-top bg-light d-flex align-items-center justify-content-center" style="height: 200px;">
            <i class="fas fa-image text-muted" style="font-size: 3rem;"></i>
          </div>
        `}
        <div class="card-body">
          <h5 class="card-title">${product.name}</h5>
          <p class="card-text">${product.description}</p>
          <div class="price-display mb-3">$${product.price.toFixed(2)}</div>
        </div>
        <div class="card-footer">
          <div class="btn-group w-100" role="group">
            <button class="btn btn-outline-primary btn-sm" onclick="openStockModal(${product.id}, ${product.availableQuantity})">
              <i class="fas fa-edit"></i> Update Stock
            </button>
            <button class="btn btn-success btn-sm" onclick="buyProduct(${product.id})" ${product.availableQuantity === 0 ? "disabled" : ""}>
              <i class="fas fa-shopping-cart"></i> Buy Now
            </button>
          </div>
        </div>
      </div>
    </div>
    `;
    }).join("");
}

function addProduct() {
    const formData = new FormData();
    formData.append('name', document.getElementById("productname").value);
    formData.append('description', document.getElementById('description').value);
    formData.append('price', document.getElementById('price').value);
    formData.append('availableQuantity', document.getElementById('availableQuantity').value);

    const imageInput = document.getElementById('productImage');
    if (imageInput.files.length > 0) {
        formData.append('productImage', imageInput.files[0]);
    }

    fetch(`${API_BASE}/products/upload`, {
        method: "POST",
        body: formData,
    })
        .then((response) => {
            if (response.ok) {
                showAlert("Product added successfully!", "success");
                document.getElementById("add-product-form").reset();
                loadProducts();
            } else {
                return response.json().then(error => { throw new Error(error.error || "Failed to add product") });
            }
        })
        .catch((error) => {
            console.error("Error adding product:", error);
            showAlert("Error adding product: " + error.message, "danger");
        });
}

function openStockModal(productId, currentQuantity) {
    document.getElementById("stockProductId").value = productId;
    document.getElementById("newQuantity").value = currentQuantity;
    new bootstrap.Modal(document.getElementById("stockModal")).show();
}

function updateStock() {
    const productId = document.getElementById("stockProductId").value;
    const newQuantity = Number.parseInt(document.getElementById("newQuantity").value);

    fetch(`${API_BASE}/products/stock/${productId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity: newQuantity }),
    })
        .then((response) => {
            if (response.ok) {
                showAlert("Stock updated successfully!", "success");
                bootstrap.Modal.getInstance(document.getElementById("stockModal")).hide();
                loadProducts();
            } else {
                throw new Error("Failed to update stock");
            }
        })
        .catch((error) => {
            console.error("Error updating stock:", error);
            showAlert("Error updating stock", "danger");
        });
}

function buyProduct(productId) {
    const quantity = prompt("Enter quantity to purchase:", "1");
    if (!quantity || quantity <= 0) return;

    fetch(`${API_BASE}/payments/create-checkout-session`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productId: productId, quantity: Number.parseInt(quantity) }),
    })
        .then((response) => response.json())
        .then((data) => {
            if (data.url) window.location.href = data.url;
            else throw new Error(data.error || "Failed to create checkout session");
        })
        .catch((error) => {
            console.error("Error creating checkout session:", error);
            showAlert("Error: " + error.message, "danger");
        });
}

function loadPayments() {
    fetch(`${API_BASE}/payments`)
        .then((response) => response.json())
        .then((payments) => displayPayments(payments))
        .catch((error) => {
            console.error("Error loading payments:", error);
            showAlert("Error loading payments", "danger");
        });
}

function displayPayments(payments) {
    const container = document.getElementById("payments-container");
    if (payments.length === 0) {
        container.innerHTML = '<div class="alert alert-info">No payments found.</div>';
        return;
    }
    container.innerHTML = `
    <div class="table-responsive">
      <table class="table table-striped">
        <thead>
          <tr>
            <th>ID</th>
            <th>Product ID</th>
            <th>Amount</th>
            <th>Quantity</th>
            <th>Status</th>
            <th>Date</th>
            <th>Stripe Session</th>
          </tr>
        </thead>
        <tbody>
          ${payments.map(payment => `
            <tr>
              <td>${payment.id}</td>
              <td>${payment.productId}</td>
              <td>$${payment.amount.toFixed(2)}</td>
              <td>${payment.quantity || 1}</td>
              <td>
                <span class="badge bg-${getStatusColor(payment.status)}">${payment.status}</span>
              </td>
              <td>${new Date(payment.timestamp).toLocaleString()}</td>
              <td><small class="text-muted">${payment.stripeSessionId || "N/A"}</small></td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function getStatusColor(status) {
    switch (status) {
        case "SUCCESS": return "success";
        case "PENDING": return "warning";
        case "FAILED": return "danger";
        default: return "secondary";
    }
}

function showAlert(message, type) {
    const alertDiv = document.createElement("div");
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `${message}
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  `;
    document.querySelector(".container").insertBefore(alertDiv, document.querySelector(".container").firstChild);
    setTimeout(() => {
        if (alertDiv.parentNode) alertDiv.remove();
    }, 5000);
}

function handleImageError(img) {
    img.onerror = null;
    img.src = 'https://via.placeholder.com/300?text=Image+Not+Found';
    return true;
}
