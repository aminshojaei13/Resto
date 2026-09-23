import SwiftUI

struct ContentView: View {
    @StateObject private var posVM = PosViewModel()
    @StateObject private var inventoryVM = InventoryViewModel()
    @StateObject private var dashboardVM = DashboardViewModel()
    @State private var isTenantSwitchPresented = false
    @State private var isPersian = true
    
    var body: some View {
        NavigationView {
            TabView {
                PosView(viewModel: posVM, isPersian: isPersian)
                    .tabItem {
                        Label(isPersian ? "فروشگاه" : "POS", systemImage: "cart.fill")
                    }
                
                InventoryView(viewModel: inventoryVM, isPersian: isPersian)
                    .tabItem {
                        Label(isPersian ? "موجودی انبار" : "Inventory", systemImage: "shippingbox.fill")
                    }
                
                DashboardView(viewModel: dashboardVM, isPersian: isPersian)
                    .tabItem {
                        Label(isPersian ? "داشبورد" : "Dashboard", systemImage: "chart.bar.fill")
                    }
                
                CustomerView(isPersian: isPersian)
                    .tabItem {
                        Label(isPersian ? "مشتریان" : "Customers", systemImage: "person.2.fill")
                    }
                
                OrdersView(isPersian: isPersian)
                    .tabItem {
                        Label(isPersian ? "سفارشات" : "Orders", systemImage: "doc.plaintext.fill")
                    }
            }
            .navigationTitle(isPersian ? "کلکو‌اپ (POS و حسابداری)" : "Calcuapp SaaS")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { isTenantSwitchPresented = true }) {
                        Image(systemName: "building.2.crop.circle")
                    }
                }
            }
            .sheet(isPresented: $isTenantSwitchPresented) {
                TenantSwitchSheet(isPersian: $isPersian)
            }
        }
        .environment(\.layoutDirection, isPersian ? .rightToLeft : .leftToRight)
    }
}

struct TenantSwitchSheet: View {
    @Binding var isPersian: Bool
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            List {
                Section(header: Text(isPersian ? "تنظیمات زبان" : "App Language")) {
                    Toggle(isOn: $isPersian) {
                        Text(isPersian ? "فارسی (راست‌چین - RTL)" : "Persian (RTL)")
                            .bold()
                    }
                }
                
                Section(header: Text(isPersian ? "فروشگاه و سازمان فعال" : "Active Tenant")) {
                    Text("گروه بازرگانی اپکس (Apex Retail) • شعبه مرکزی")
                        .bold()
                        .foregroundColor(.blue)
                    Text("الکترونیک براف بوی (BraveBoy) • شعبه مترو")
                }
            }
            .navigationTitle(isPersian ? "تنظیمات شعبه و زبان" : "Switch Store Context")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(isPersian ? "تایید" : "Done") { dismiss() }
                }
            }
        }
        .environment(\.layoutDirection, isPersian ? .rightToLeft : .leftToRight)
    }
}
