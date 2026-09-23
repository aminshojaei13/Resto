package com.braveboy.calcuapp.data.mock

import com.braveboy.calcuapp.data.model.Customer
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
        ),
        Organization(
            id = "org_omni",
            name = "Omni Supermarket & Organics",
            code = "OMNI",
            logoUrl = "https://picsum.photos/id/1020/200",
            currencySymbol = "€",
            currencyCode = "EUR",
            subscriptionTier = "STARTER",
            stores = listOf(
                Store(
                    id = "store_omni_1",
                    orgId = "org_omni",
                    name = "Omni Central Superstore",
                    code = "OMN-CTR",
                    address = "Via Roma 45, Milan, Italy",
                    phone = "+39 02 8901 2345",
                    warehouses = listOf(
                        Warehouse(
                            id = "wh_omni_1",
                            storeId = "store_omni_1",
                            orgId = "org_omni",
                            name = "Cold Storage & Pantry",
                            code = "WH-OMNI-COLD",
                            address = "Via Roma 45-B, Milan"
                        )
                    )
                )
            )
        )
    )

    val products: List<Product> = listOf(
        // Apex Retail Group Products
        Product(
            id = "prod_1",
            orgId = "org_apex",
            sku = "APX-LAP-001",
            barcode = "880609123401",
            name = "ProBook Ultra 15 M3",
            description = "High-performance laptop featuring 16GB RAM, 512GB SSD, and vibrant M3 retina display.",
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
                    stockQuantityByWarehouse = mapOf("wh_apex_1a" to 14, "wh_apex_1b" to 5, "wh_apex_2a" to 8)
                ),
                ProductVariant(
                    id = "var_1_2",
                    productId = "prod_1",
                    sku = "APX-LAP-001-BLK",
                    barcode = "8806091234012",
                    name = "Space Black - 1TB",
                    price = 1499.99,
                    costPrice = 980.00,
                    stockQuantityByWarehouse = mapOf("wh_apex_1a" to 8, "wh_apex_1b" to 2, "wh_apex_2a" to 4)
                )
            ),
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 22, "wh_apex_1b" to 7, "wh_apex_2a" to 12)
        ),
        Product(
            id = "prod_2",
            orgId = "org_apex",
            sku = "APX-AUD-002",
            barcode = "880609123402",
            name = "NoiseCancel Studio Headphones",
            description = "Active noise cancelling wireless headphones with 30-hour battery life and spatial audio.",
            price = 249.99,
            costPrice = 120.00,
            category = "Audio",
            imageUrl = "https://picsum.photos/id/1057/300/300",
            unit = "pcs",
            variants = emptyList(),
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 45, "wh_apex_1b" to 18, "wh_apex_2a" to 20)
        ),
        Product(
            id = "prod_3",
            orgId = "org_apex",
            sku = "APX-MOB-003",
            barcode = "880609123403",
            name = "Apex Phone 15 Pro",
            description = "Flagship smartphone with triple lens camera, titanium frame, and 120Hz AMOLED display.",
            price = 999.00,
            costPrice = 620.00,
            category = "Smartphones",
            imageUrl = "https://picsum.photos/id/160/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 18, "wh_apex_1b" to 9, "wh_apex_2a" to 15)
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
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 30, "wh_apex_1b" to 12, "wh_apex_2a" to 8)
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
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 8, "wh_apex_1b" to 3, "wh_apex_2a" to 5)
        ),
        Product(
            id = "prod_6",
            orgId = "org_apex",
            sku = "APX-FUR-006",
            barcode = "880609123406",
            name = "Ergonomic Executive Desk Chair",
            description = "Breathable mesh executive office chair with dynamic lumbar support and 3D armrests.",
            price = 299.00,
            costPrice = 160.00,
            category = "Furniture",
            imageUrl = "https://picsum.photos/id/1062/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_apex_1a" to 6, "wh_apex_1b" to 2, "wh_apex_2a" to 3)
        ),

        // BraveBoy Electronics
        Product(
            id = "prod_bb_1",
            orgId = "org_braveboy",
            sku = "BBE-GAM-101",
            barcode = "990123456701",
            name = "CyberPower Gamer Pro PC",
            description = "Ryzen 9, RTX 4080, 32GB DDR5, 2TB NVMe SSD liquid-cooled rig.",
            price = 2199.00,
            costPrice = 1500.00,
            category = "Gaming",
            imageUrl = "https://picsum.photos/id/96/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_bb_1" to 10)
        ),
        Product(
            id = "prod_bb_2",
            orgId = "org_braveboy",
            sku = "BBE-TV-102",
            barcode = "990123456702",
            name = "OLED Cinema TV 65\"",
            description = "4K OLED smart TV with Dolby Vision, Atmos, and 120Hz VRR.",
            price = 1799.00,
            costPrice = 1100.00,
            category = "Home Entertainment",
            imageUrl = "https://picsum.photos/id/1084/300/300",
            unit = "pcs",
            stockQuantityByWarehouse = mapOf("wh_bb_1" to 7)
        ),

        // Omni Supermarket Products
        Product(
            id = "prod_omni_1",
            orgId = "org_omni",
            sku = "OMN-COF-001",
            barcode = "770987654301",
            name = "Organic Arabica Coffee Beans 1kg",
            description = "Single-origin dark roast Arabica whole bean coffee from Colombia.",
            price = 18.50,
            costPrice = 8.20,
            category = "Groceries",
            imageUrl = "https://picsum.photos/id/1060/300/300",
            unit = "bag",
            stockQuantityByWarehouse = mapOf("wh_omni_1" to 120)
        ),
        Product(
            id = "prod_omni_2",
            orgId = "org_omni",
            sku = "OMN-OIL-002",
            barcode = "770987654302",
            name = "Extra Virgin Olive Oil 750ml",
            description = "First cold-pressed Tuscan extra virgin olive oil.",
            price = 14.90,
            costPrice = 6.50,
            category = "Groceries",
            imageUrl = "https://picsum.photos/id/225/300/300",
            unit = "bottle",
            stockQuantityByWarehouse = mapOf("wh_omni_1" to 85)
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
        ),
        Customer(
            id = "cust_3",
            orgId = "org_apex",
            name = "Emily Chen",
            email = "emily.chen@designstudio.com",
            phone = "+1 (555) 345-6789",
            address = "12 Market Plaza, San Francisco, CA",
            totalPurchases = 890.00,
            loyaltyPoints = 85
        ),
        Customer(
            id = "cust_bb_1",
            orgId = "org_braveboy",
            name = "Dwight Schrute",
            email = "dwight@beetfarms.com",
            phone = "+1 (555) 837-2638",
            address = "Schrute Farms, Scranton, PA",
            totalPurchases = 4200.00,
            loyaltyPoints = 420
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
                    variantId = "var_1_1",
                    productName = "ProBook Ultra 15 M3 (Silver - 512GB)",
                    sku = "APX-LAP-001-SLV",
                    unitPrice = 1299.99,
                    quantity = 1,
                    discountPercent = 5.0,
                    taxAmount = 98.80,
                    totalPrice = 1333.79
                ),
                OrderItem(
                    productId = "prod_2",
                    variantId = null,
                    productName = "NoiseCancel Studio Headphones",
                    sku = "APX-AUD-002",
                    unitPrice = 249.99,
                    quantity = 1,
                    discountPercent = 0.0,
                    taxAmount = 20.00,
                    totalPrice = 269.99
                )
            ),
            subtotal = 1549.98,
            discountAmount = 65.00,
            taxAmount = 118.80,
            totalAmount = 1603.78,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.PAID,
            fulfillmentStatus = FulfillmentStatus.COMPLETED,
            notes = "Customer paid with Visa credit card.",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        SalesOrder(
            id = "ord_1002",
            orderNumber = "ORD-2025-1002",
            orgId = "org_apex",
            storeId = "store_apex_1",
            warehouseId = "wh_apex_1a",
            customerId = "cust_2",
            customerName = "John Smith",
            items = listOf(
                OrderItem(
                    productId = "prod_3",
                    variantId = null,
                    productName = "Apex Phone 15 Pro",
                    sku = "APX-MOB-003",
                    unitPrice = 999.00,
                    quantity = 1,
                    discountPercent = 0.0,
                    taxAmount = 79.92,
                    totalPrice = 1078.92
                )
            ),
            subtotal = 999.00,
            discountAmount = 0.0,
            taxAmount = 79.92,
            totalAmount = 1078.92,
            paymentMethod = PaymentMethod.MOBILE_PAYMENT,
            paymentStatus = PaymentStatus.PAID,
            fulfillmentStatus = FulfillmentStatus.COMPLETED,
            notes = "Paid via Apple Pay.",
            createdAt = System.currentTimeMillis() - 86400000L
        ),
        SalesOrder(
            id = "ord_1003",
            orderNumber = "ORD-2025-1003",
            orgId = "org_apex",
            storeId = "store_apex_1",
            warehouseId = "wh_apex_1a",
            customerId = null,
            customerName = "Walk-in Customer",
            items = listOf(
                OrderItem(
                    productId = "prod_4",
                    variantId = null,
                    productName = "Ergonomic Mechanical Keyboard",
                    sku = "APX-ACC-004",
                    unitPrice = 119.50,
                    quantity = 2,
                    discountPercent = 0.0,
                    taxAmount = 19.12,
                    totalPrice = 258.12
                )
            ),
            subtotal = 239.00,
            discountAmount = 0.0,
            taxAmount = 19.12,
            totalAmount = 258.12,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            fulfillmentStatus = FulfillmentStatus.COMPLETED,
            notes = "Walk-in cash sale.",
            createdAt = System.currentTimeMillis() - 3600000L * 4
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
            amount = 1603.78,
            description = "Sales Order #ORD-2025-1001 payment received via Card",
            referenceId = "ord_1001",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        LedgerEntry(
            id = "leg_2002",
            orgId = "org_apex",
            storeId = "store_apex_1",
            entryNumber = "LEDG-2025-002",
            type = LedgerType.CREDIT,
            category = LedgerCategory.SALES,
            amount = 1078.92,
            description = "Sales Order #ORD-2025-1002 payment received via Mobile",
            referenceId = "ord_1002",
            createdAt = System.currentTimeMillis() - 86400000L
        ),
        LedgerEntry(
            id = "leg_2003",
            orgId = "org_apex",
            storeId = "store_apex_1",
            entryNumber = "LEDG-2025-003",
            type = LedgerType.CREDIT,
            category = LedgerCategory.SALES,
            amount = 258.12,
            description = "Sales Order #ORD-2025-1003 payment received via Cash",
            referenceId = "ord_1003",
            createdAt = System.currentTimeMillis() - 3600000L * 4
        ),
        LedgerEntry(
            id = "leg_2004",
            orgId = "org_apex",
            storeId = "store_apex_1",
            entryNumber = "LEDG-2025-004",
            type = LedgerType.DEBIT,
            category = LedgerCategory.EXPENSE,
            amount = 350.00,
            description = "Monthly Store Utility & Internet Bill",
            referenceId = null,
            createdAt = System.currentTimeMillis() - 86400000L * 3
        ),
        LedgerEntry(
            id = "leg_2005",
            orgId = "org_apex",
            storeId = "store_apex_1",
            entryNumber = "LEDG-2025-005",
            type = LedgerType.CREDIT,
            category = LedgerCategory.CASH_IN,
            amount = 500.00,
            description = "Register Drawer Initial Float",
            referenceId = null,
            createdAt = System.currentTimeMillis() - 86400000L * 4
        )
    )
}
