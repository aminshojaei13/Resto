# API.md - REST API Specifications & Endpoints

## 1. Authentication & Multi-Tenant Headers
All API requests require Bearer token authentication and tenant context headers:

```http
Authorization: Bearer <jwt_token>
X-Tenant-ID: org_apex
X-Store-ID: store_apex_1
X-Warehouse-ID: wh_apex_1a
```

## 2. Endpoint Endpoints Summary

### Auth & Tenant Management
- `POST /api/v1/auth/login` - User authentication
- `GET /api/v1/organizations` - List user organizations
- `GET /api/v1/organizations/{id}/stores` - List stores for tenant

### Products & Inventory
- `GET /api/v1/products` - Search & list products
- `GET /api/v1/products/barcode/{code}` - Lookup by barcode
- `POST /api/v1/inventory/adjust` - Adjust stock level at warehouse

### Sales Orders & POS
- `POST /api/v1/orders/checkout` - Create sales order and process checkout
- `GET /api/v1/orders` - List sales order history
- `GET /api/v1/orders/{id}` - Fetch sales order details & receipt

### Ledger & Financials
- `GET /api/v1/ledger` - Fetch general ledger entries
- `POST /api/v1/ledger/entry` - Record manual financial ledger entry
- `GET /api/v1/metrics/summary` - Fetch executive revenue & profit summary
