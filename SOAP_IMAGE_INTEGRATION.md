# SOAP API Image Integration

## Overview

This document describes the implementation of image data transmission through SOAP API responses. The system now sends both image URLs and base64-encoded image data to SOAP clients.

## Changes Made

### 1. Updated SOAP Schema (`supplier.xsd`)

Added new optional fields to the product complex type:
- `pictureData` (xs:base64Binary) - Raw image bytes encoded as base64
- `pictureFormat` (xs:string) - Image file format (jpg, png, gif, etc.)

The existing `pictureUrl` field is preserved for backward compatibility.

### 2. Created ImageService

New service class `com.supplier.service.ImageService` provides:
- `getImageBytes(String imagePath)` - Returns raw image bytes
- `convertImageToBase64(String imagePath)` - Returns base64-encoded string
- `getImageFormat(String imagePath)` - Extracts file format from path

### 3. Enhanced SupplierEndpoint

Modified the `convertToWsProduct` method to:
- Include image bytes in SOAP responses when local images are available
- Set the image format for proper client-side handling
- Handle both local files and external URLs gracefully

## Usage for SOAP Clients

When consuming the SOAP API, clients will now receive:

```xml
<product>
    <id>1</id>
    <name>Product Name</name>
    <!-- ... other fields ... -->
    <pictureUrl>/uploads/image.jpg</pictureUrl>
    <pictureData>iVBORw0KGgoAAAANSUhEUgAA...</pictureData>
    <pictureFormat>jpg</pictureFormat>
</product>
```

### Client Implementation Examples

#### Java Client
```java
Product product = response.getProduct();
if (product.getPictureData() != null) {
    byte[] imageBytes = product.getPictureData();
    String format = product.getPictureFormat();
    // Save or display the image
}
```

#### .NET Client
```csharp
var product = response.Product;
if (product.PictureData != null) {
    byte[] imageBytes = product.PictureData;
    string format = product.PictureFormat;
    // Process the image
}
```

## Benefits

1. **Self-contained responses** - No need for separate HTTP requests to fetch images
2. **Reliability** - Images are guaranteed to be available with the product data
3. **Security** - No dependency on external image hosting
4. **Offline capability** - Clients can work without internet access to image URLs
5. **Backward compatibility** - Existing clients still receive image URLs

## Technical Notes

- Images are loaded from the local `uploads/` directory
- External URLs (starting with "http") are handled gracefully but don't include image data
- JAXB automatically handles base64 encoding/decoding for the `xs:base64Binary` type
- Large images may increase SOAP response size - consider image optimization if needed

## Configuration

The image directory is configured via:
```properties
upload.dir=${user.dir}/uploads/
```

## Testing

Run the test suite to verify image processing functionality:
```bash
mvn test -Dtest=ImageSoapTest
```

