import Foundation
import Combine

@MainActor
class PosViewModel: ObservableObject {
    @Published var products: [Product] = []
    @Published var cart: [CartItem] = []
    @Published var searchQuery: String = ""
    @Published var isCheckoutPresented: Bool = false
    
    var subtotal: Double { cart.reduce(0) { $0 + $1.subtotal } }
    var totalAmount: Double { cart.reduce(0) { $0 + $1.total } }
    
    func loadProducts() async {
        do {
            self.products = try await ApiService.shared.fetchProducts()
        } catch {
            print("Failed to load products: \(error)")
        }
    }
    
    func addToCart(product: Product) {
        if let index = cart.firstIndex(where: { $0.product.id == product.id }) {
            cart[index].quantity += 1
        } else {
            cart.append(CartItem(id: UUID().uuidString, product: product, quantity: 1, discountPercent: 0))
        }
    }
    
    func clearCart() {
        cart.removeAll()
    }
}

@MainActor
class InventoryViewModel: ObservableObject {
    @Published var products: [Product] = []
    
    func loadProducts() async {
        do {
            self.products = try await ApiService.shared.fetchProducts()
        } catch {
            print("Failed to load inventory: \(error)")
        }
    }
}

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var ledgerEntries: [LedgerEntry] = []
    @Published var totalRevenue: Double = 2940.82
    @Published var todayRevenue: Double = 1603.78
    
    func loadData() async {
        do {
            self.ledgerEntries = try await ApiService.shared.fetchLedgerEntries()
        } catch {
            print("Failed to load dashboard: \(error)")
        }
    }
}
