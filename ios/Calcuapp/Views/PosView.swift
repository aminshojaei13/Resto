import SwiftUI

struct PosView: View {
    @ObservedObject var viewModel: PosViewModel
    var isPersian: Bool = true
    
    var currencySymbol: String { isPersian ? "تومان" : "$" }
    
    var body: some View {
        VStack {
            // Search Bar
            TextField(isPersian ? "جستجوی کالا و بارکد..." : "Search catalog...", text: $viewModel.searchQuery)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)
            
            // Product Catalog Grid
            ScrollView {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 150))], spacing: 16) {
                    ForEach(viewModel.products) { product in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(product.name)
                                .font(.headline)
                                .lineLimit(1)
                            Text("SKU: \(product.sku)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            HStack {
                                Text(isPersian ? "\(Int(product.price * 50000).formatted()) \(currencySymbol)" : String(format: "$%.2f", product.price))
                                    .font(.subheadline)
                                    .bold()
                                    .foregroundColor(.blue)
                                Spacer()
                                Button(action: { viewModel.addToCart(product: product) }) {
                                    Image(systemName: "cart.badge.plus")
                                        .font(.title3)
                                }
                            }
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                }
                .padding()
            }
            
            // Cart Summary Footer
            if !viewModel.cart.isEmpty {
                VStack(spacing: 12) {
                    HStack {
                        Text(isPersian ? "سبد خرید (\(viewModel.cart.reduce(0) { $0 + $1.quantity }))" : "Cart Items (\(viewModel.cart.reduce(0) { $0 + $1.quantity }))")
                            .font(.headline)
                        Spacer()
                        Text(isPersian ? "\(Int(viewModel.totalAmount * 50000).formatted()) \(currencySymbol)" : String(format: "$%.2f", viewModel.totalAmount))
                            .font(.title2)
                            .bold()
                            .foregroundColor(.blue)
                    }
                    
                    Button(action: { viewModel.isCheckoutPresented = true }) {
                        Text(isPersian ? "تسویه و پرداخت" : "Checkout")
                            .font(.headline)
                            .bold()
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .cornerRadius(12)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .shadow(radius: 4)
            }
        }
        .task {
            await viewModel.loadProducts()
        }
        .sheet(isPresented: $viewModel.isCheckoutPresented) {
            CheckoutSheet(viewModel: viewModel, isPersian: isPersian)
        }
    }
}

struct CheckoutSheet: View {
    @ObservedObject var viewModel: PosViewModel
    var isPersian: Bool = true
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            List {
                Section(header: Text(isPersian ? "اقلام سفارش" : "Order Items")) {
                    ForEach(viewModel.cart) { item in
                        HStack {
                            Text(item.product.name)
                            Spacer()
                            Text("\(item.quantity)x \(String(format: "$%.2f", item.product.price))")
                                .bold()
                        }
                    }
                }
                
                Section(header: Text(isPersian ? "مجموع پرداختی" : "Payment Total")) {
                    HStack {
                        Text(isPersian ? "مبلغ قابل پرداخت" : "Grand Total")
                            .bold()
                        Spacer()
                        Text(isPersian ? "\(Int(viewModel.totalAmount * 50000).formatted()) تومان" : String(format: "$%.2f", viewModel.totalAmount))
                            .font(.title3)
                            .bold()
                            .foregroundColor(.blue)
                    }
                }
            }
            .navigationTitle(isPersian ? "تسویه حساب فاکتور" : "POS Checkout")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(isPersian ? "انصراف" : "Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isPersian ? "تکمیل فروش" : "Complete Sale") {
                        viewModel.clearCart()
                        dismiss()
                    }
                    .bold()
                }
            }
        }
        .environment(\.layoutDirection, isPersian ? .rightToLeft : .leftToRight)
    }
}
