import Foundation

struct Organization: Identifiable, Codable {
    let id: String
    let name: String
    let code: String
    let currencySymbol: String
    let currencyCode: String
    let subscriptionTier: String
    var stores: [Store]?
}

struct Store: Identifiable, Codable {
    let id: String
    let organizationId: String
    let name: String
    let code: String
    var warehouses: [Warehouse]?
}

struct Warehouse: Identifiable, Codable {
    let id: String
    let storeId: String
    let organizationId: String
    let name: String
    let code: String
}

struct Product: Identifiable, Codable {
    let id: String
    let organizationId: String
    let sku: String
    let barcode: String
    let name: String
    let description: String?
    let price: Double
    let costPrice: Double
    let category: String
    let unit: String
    let imageUrl: String?
}

struct CartItem: Identifiable {
    let id: String
    let product: Product
    var quantity: Int
    var discountPercent: Double
    
    var subtotal: Double { product.price * Double(quantity) }
    var total: Double { subtotal * (1.0 - (discountPercent / 100.0)) * 1.08 }
}

struct Customer: Identifiable, Codable {
    let id: String
    let organizationId: String
    let name: String
    let email: String?
    let phone: String?
    let totalPurchases: Double
    let loyaltyPoints: Int
}

struct SalesOrder: Identifiable, Codable {
    let id: String
    let orderNumber: String
    let customerName: String
    let subtotal: Double
    let taxAmount: Double
    let totalAmount: Double
    let paymentMethod: String
    let paymentStatus: String
    let fulfillmentStatus: String
}

struct LedgerEntry: Identifiable, Codable {
    let id: String
    let entryNumber: String
    let type: String
    let category: String
    let amount: Double
    let description: String
}
