import SwiftUI

struct ContentView: View {
    @StateObject private var posVM = PosViewModel()
    @StateObject private var inventoryVM = InventoryViewModel()
    @StateObject private var dashboardVM = DashboardViewModel()
    @State private var isTenantSwitchPresented = false
    
    var body: some View {
        NavigationView {
            TabView {
                PosView(viewModel: posVM)
                    .tabItem {
                        Label("POS", systemImage: "cart.fill")
                    }
                
                InventoryView(viewModel: inventoryVM)
                    .tabItem {
                        Label("Inventory", systemImage: "shippingbox.fill")
                    }
                
                DashboardView(viewModel: dashboardVM)
                    .tabItem {
                        Label("Dashboard", systemImage: "chart.bar.fill")
                    }
                
                CustomerView()
                    .tabItem {
                        Label("Customers", systemImage: "person.2.fill")
                    }
                
                OrdersView()
                    .tabItem {
                        Label("Orders", systemImage: "doc.plaintext.fill")
                    }
            }
            .navigationTitle("Calcuapp SaaS")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { isTenantSwitchPresented = true }) {
                        Image(systemName: "building.2.crop.circle")
                    }
                }
            }
            .sheet(isPresented: $isTenantSwitchPresented) {
                TenantSwitchSheet()
            }
        }
    }
}

struct TenantSwitchSheet: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Active Tenant")) {
                    Text("Apex Retail Group • Main Store")
                        .bold()
                        .foregroundColor(.blue)
                    Text("BraveBoy Electronics • Metro Store")
                }
            }
            .navigationTitle("Switch Store Context")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
