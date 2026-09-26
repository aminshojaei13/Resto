package com.braveboy.calcuapp.data.mock

import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.Expense
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.OrderItem
import com.braveboy.calcuapp.data.model.Organization
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.ProductVariant
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.data.model.Store
import com.braveboy.calcuapp.data.model.Supplier
import com.braveboy.calcuapp.data.model.Warehouse

object MockSaaSDataSource {

    val organizations: List<Organization> = listOf(
        Organization(
            id = "org_apex",
            name = "Apex Retail Group",
            code = "APEX",
            logoUrl = "https://picsum.photos/id/1018/200",
            currencySymbol = "$",
            currencyCode = "USD",
            subscriptionTier = "ENTERPRISE",
            stores = listOf(
                Store(
                    id = "store_apex_1",
                    orgId = "org_apex",
                    name = "Apex Flagship Store (Downtown)",
                    code = "APX-DT",
                    address = "100 Market St, San Francisco, CA",
                    phone = "+1 (555) 019-2834",
                    warehouses = listOf(
                        Warehouse(
                            id = "wh_apex_1a",
                            storeId = "store_apex_1",
                            orgId = "org_apex",
                            name = "Main Warehouse",
                            code = "WH-MAIN",
                            address = "100 Market St B1, San Francisco"
                        ),
                        Warehouse(
                            id = "wh_apex_1b",
                            storeId = "store_apex_1",
                            orgId = "org_apex",
                            name = "Express Storage Hub",
                            code = "WH-EXP",
                            address = "120 Market St, San Francisco"
                        )
                    )
                ),
                Store(
                    id = "store_apex_2",
                    orgId = "org_apex",
                    name = "Apex Outlet Store",
                    code = "APX-OUT",
                    address = "500 Commerce Ave, Oakland, CA",
                    phone = "+1 (555) 012-9842",
                    warehouses = listOf(
                        Warehouse(
                            id = "wh_apex_2a",
                            storeId = "store_apex_2",
                            orgId = "org_apex",
                            name = "Outlet Stockroom",
                            code = "WH-OUTLET",
                            address = "500 Commerce Ave, Oakland"
                        )
                    )
                )
            )
        ),
        Organization(
            id = "org_braveboy",
            name = "BraveBoy Electronics",
            code = "BBE",
            logoUrl = "https://picsum.photos/id/1019/200",
            currencySymbol = "$",
            currencyCode = "USD",
            subscriptionTier = "PRO",
            stores = listOf(
                Store(
                    id = "store_bb_1",
                    orgId = "org_braveboy",
                    name = "Tech Hub Metro",
                    code = "BBE-MTR",
                    address = "742 Evergreen Terrace, Springfield",
                    phone = "+1 (555) 901-2211",
                    warehouses = listOf(
                        Warehouse(
                            id = "wh_bb_1",
                            storeId = "store_bb_1",
                            orgId = "org_braveboy",
                            name = "Metro Depot",
                            code = "WH-BB1",
                            address = "742 Evergreen Terrace Rear, Springfield"
                        )
                    )
                )
            )
        )
    )

    val products: List<Product> = listOf(
        Product(
            id = "prod_1",
            orgId = "org_apex",
            sku = "APX-LAP-001",
            barcode = "880609123401",
            name = "ProBook Ultra 15 M3",
            description = "High-performance laptop featuring 16GB RAM, 512GB SSD.",
            price = 1299.99,
            costPrice = 850.00,
            category = "Electronics",
            imageUrl = "https://picsum.photos/id/0/300/300",
            unit = "pcs",
            variants = listOf(
                ProductVariant(
                    id = "var_1_1",
                    productId = "prod_1",
                    sku = "APX-LAP-001-SLV",
                    barcode = "8806091234011",
                    name = "Silver - 512GB",
                    price = 1299.99,
                    costPrice = 850.00,
                    stockQuantityByWarehouse = mapOf("wh_apex_1a" to 14, "wh_apex_1b" to 5)
                )
            ),
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 22, "wh_apex_1b" to 7)
        ),
        Product(
            id = "prod_2",
            orgId = "org_apex",
            sku = "APX-AUD-002",
            barcode = "880609123402",
            name = "NoiseCancel Studio Headphones",
            description = "Active noise cancelling wireless headphones with 30-hour battery life.",
            price = 249.99,
            costPrice = 120.00,
            category = "Audio",
            imageUrl = "https://picsum.photos/id/1057/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 45, "wh_apex_1b" to 18)
        ),
        Product(
            id = "prod_3",
            orgId = "org_apex",
            sku = "APX-MOB-003",
            barcode = "880609123403",
            name = "Apex Phone 15 Pro",
            description = "Flagship smartphone with triple lens camera and 120Hz AMOLED display.",
            price = 999.00,
            costPrice = 620.00,
            category = "Smartphones",
            imageUrl = "https://picsum.photos/id/160/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 18, "wh_apex_1b" to 9)
        ),
        Product(
            id = "prod_4",
            orgId = "org_apex",
            sku = "APX-ACC-004",
            barcode = "880609123404",
            name = "Ergonomic Mechanical Keyboard",
            description = "Hot-swappable tactile RGB mechanical keyboard with PBT keycaps.",
            price = 119.50,
            costPrice = 55.00,
            category = "Accessories",
            imageUrl = "https://picsum.photos/id/367/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 30, "wh_apex_1b" to 12)
        ),
        Product(
            id = "prod_5",
            orgId = "org_apex",
            sku = "APX-MON-005",
            barcode = "880609123405",
            name = "Curved Gaming Monitor 34\"",
            description = "UltraWide 144Hz 1ms curved IPS gaming monitor with HDR10.",
            price = 549.00,
            costPrice = 340.00,
            category = "Monitors",
            imageUrl = "https://picsum.photos/id/201/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 8, "wh_apex_1b" to 3)
        )
    )

    val customers: List<Customer> = listOf(
        Customer(
            id = "cust_1",
            orgId = "org_apex",
            name = "Sarah Connor",
            email = "sarah.connor@example.com",
            phone = "+1 (555) 234-5678",
            address = "789 Cyberdyne Way, Los Angeles, CA",
            totalPurchases = 3548.50,
            loyaltyPoints = 350
        ),
        Customer(
            id = "cust_2",
            orgId = "org_apex",
            name = "John Smith",
            email = "john.smith@techcorp.io",
            phone = "+1 (555) 987-6543",
            address = "456 Innovation Blvd, San Jose, CA",
            totalPurchases = 1899.90,
            loyaltyPoints = 180
        )
    )

    val suppliers: List<Supplier> = listOf(
        Supplier(
            id = "sup_1",
            orgId = "org_apex",
            name = "TechImport Global Co.",
            email = "sales@techimport.com",
            phone = "+1 (800) 555-0199",
            address = "500 Logistics Way, San Jose, CA"
        ),
        Supplier(
            id = "sup_2",
            orgId = "org_apex",
            name = "ElectroComponents Inc.",
            email = "orders@electrocomponents.com",
            phone = "+1 (800) 555-0288",
            address = "12 Industrial Park, Austin, TX"
        )
    )

    val expenses: List<Expense> = listOf(
        Expense(
            id = "exp_1001",
            orgId = "org_apex",
            storeId = "store_apex_1",
            category = "Store Utilities",
            amount = 450.00,
            paymentMethod = "BANK_TRANSFER",
            date = "2025-01-15",
            notes = "Monthly electricity bill for Apex Flagship Store"
        ),
        Expense(
            id = "exp_1002",
            orgId = "org_apex",
            storeId = "store_apex_1",
            category = "Marketing & Ads",
            amount = 250.00,
            paymentMethod = "CASH",
            date = "2025-01-20",
            notes = "Social media campaign ad spend"
        )
    )

    val salesOrders: List<SalesOrder> = listOf(
        SalesOrder(
            id = "ord_1001",
            orderNumber = "ORD-2025-1001",
            orgId = "org_apex",
            storeId = "store_apex_1",
            warehouseId = "wh_apex_1a",
            customerId = "cust_1",
            customerName = "Sarah Connor",
            items = listOf(
                OrderItem(
                    productId = "prod_1",
                    variantId = null,
                    productName = "ProBook Ultra 15 M3",
                    sku = "APX-LAP-001",
                    unitPrice = 1299.99,
                    quantity = 1,
                    discountPercent = 5.0,
                    taxAmount = 98.80,
                    totalPrice = 1333.79
                )
            ),
            subtotal = 1299.99,
            discountAmount = 65.00,
            taxAmount = 98.80,
            totalAmount = 1333.79,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.PAID,
            fulfillmentStatus = FulfillmentStatus.COMPLETED,
            notes = "Paid via credit card.",
            createdAt = System.currentTimeMillis() - 86400000L
        )
    )

    val ledgerEntries: List<LedgerEntry> = listOf(
        LedgerEntry(
            id = "leg_2001",
            orgId = "org_apex",
            storeId = "store_apex_1",
            entryNumber = "LEDG-2025-001",
            type = LedgerType.CREDIT,
            category = LedgerCategory.SALES,
            amount = 1333.79,
            description = "Sales Order #ORD-2025-1001 payment received",
            referenceId = "ord_1001",
            createdAt = System.currentTimeMillis() - 86400000L
        )
    )
}
