import Foundation

class ApiService {
    static let shared = ApiService()
    private init() {}
    
    private let baseURL = "http://localhost:8000/api/v1"
    private var activeOrgId = "org_apex"
    private var activeStoreId = "store_apex_1"
    
    func setTenantContext(orgId: String, storeId: String) {
        self.activeOrgId = orgId
        self.activeStoreId = storeId
    }
    
    func fetchProducts() async throws -> [Product] {
        guard let url = URL(string: "\(baseURL)/products?org_id=\(activeOrgId)") else { return [] }
        var request = URLRequest(url: url)
        request.addValue(activeOrgId, forHTTPHeaderField: "X-Tenant-ID")
        request.addValue(activeStoreId, forHTTPHeaderField: "X-Store-ID")
        
        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            return try decoder.decode([Product].self, from: data)
        } catch {
            // Fallback mock data
            return [
                Product(id: "prod_1", organizationId: "org_apex", sku: "APX-LAP-001", barcode: "880609123401", name: "ProBook Ultra 15 M3", description: "High performance M3 laptop", price: 1299.99, costPrice: 850.0, category: "Electronics", unit: "pcs", imageUrl: nil),
                Product(id: "prod_2", organizationId: "org_apex", sku: "APX-AUD-002", barcode: "880609123402", name: "NoiseCancel Studio Headphones", description: "Wireless spatial audio", price: 249.99, costPrice: 120.0, category: "Audio", unit: "pcs", imageUrl: nil),
                Product(id: "prod_3", organizationId: "org_apex", sku: "APX-MOB-003", barcode: "880609123403", name: "Apex Phone 15 Pro", description: "Flagship AMOLED phone", price: 999.0, costPrice: 620.0, category: "Smartphones", unit: "pcs", imageUrl: nil)
            ]
        }
    }
    
    func fetchCustomers() async throws -> [Customer] {
        return [
            Customer(id: "cust_1", organizationId: "org_apex", name: "Sarah Connor", email: "sarah@example.com", phone: "+1 555-234-5678", totalPurchases: 3548.5, loyaltyPoints: 350),
            Customer(id: "cust_2", organizationId: "org_apex", name: "John Smith", email: "john@techcorp.io", phone: "+1 555-987-6543", totalPurchases: 1899.9, loyaltyPoints: 180)
        ]
    }
    
    func fetchLedgerEntries() async throws -> [LedgerEntry] {
        return [
            LedgerEntry(id: "leg_1", entryNumber: "LEDG-2025-001", type: "CREDIT", category: "SALES", amount: 1603.78, description: "Sales Order #ORD-2025-1001 payment"),
            LedgerEntry(id: "leg_2", entryNumber: "LEDG-2025-002", type: "DEBIT", category: "EXPENSE", amount: 350.0, description: "Monthly Store Utility Bill")
        ]
    }
}
