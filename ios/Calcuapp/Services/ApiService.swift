import Foundation

class ApiService {
    static let shared = ApiService()
    private init() {}
    
    private let baseURL = "http://localhost:8000/api/v1"
    private var activeOrgId = ""
    private var activeStoreId = ""
    
    func setTenantContext(orgId: String, storeId: String) {
        self.activeOrgId = orgId
        self.activeStoreId = storeId
    }
    
    func fetchProducts() async throws -> [Product] {
        guard !activeOrgId.isEmpty, let url = URL(string: "\(baseURL)/products?org_id=\(activeOrgId)") else { return [] }
        var request = URLRequest(url: url)
        request.addValue(activeOrgId, forHTTPHeaderField: "X-Tenant-ID")
        request.addValue(activeStoreId, forHTTPHeaderField: "X-Store-ID")
        
        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            return try decoder.decode([Product].self, from: data)
        } catch {
            return []
        }
    }
    
    func fetchCustomers() async throws -> [Customer] {
        guard !activeOrgId.isEmpty, let url = URL(string: "\(baseURL)/customers?org_id=\(activeOrgId)") else { return [] }
        var request = URLRequest(url: url)
        request.addValue(activeOrgId, forHTTPHeaderField: "X-Tenant-ID")
        request.addValue(activeStoreId, forHTTPHeaderField: "X-Store-ID")
        
        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            return try decoder.decode([Customer].self, from: data)
        } catch {
            return []
        }
    }
    
    func fetchLedgerEntries() async throws -> [LedgerEntry] {
        guard !activeOrgId.isEmpty, let url = URL(string: "\(baseURL)/accounting/journal?org_id=\(activeOrgId)") else { return [] }
        var request = URLRequest(url: url)
        request.addValue(activeOrgId, forHTTPHeaderField: "X-Tenant-ID")
        request.addValue(activeStoreId, forHTTPHeaderField: "X-Store-ID")
        
        do {
            let (data, _) = try await URLSession.shared.data(for: request)
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            return try decoder.decode([LedgerEntry].self, from: data)
        } catch {
            return []
        }
    }
}
