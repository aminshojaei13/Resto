import SwiftUI

struct PosView: View {
    @ObservedObject var viewModel: PosViewModel
    
    var body: some View {
        VStack {
            // Search Bar
            TextField("Search catalog...", text: $viewModel.searchQuery)
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
                                Text(String(format: "$%.2f", product.price))
                                    .font(.title3)
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
                        Text("Cart Items (\(viewModel.cart.reduce(0) { $0 + $1.quantity }))")
                            .font(.headline)
                        Spacer()
                        Text(String(format: "$%.2f", viewModel.totalAmount))
                            .font(.title2)
                            .bold()
                            .foregroundColor(.blue)
                    }
                    
                    Button(action: { viewModel.isCheckoutPresented = true }) {
                        Text("Checkout")
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
            CheckoutSheet(viewModel: viewModel)
        }
    }
}

struct CheckoutSheet: View {
    @ObservedObject var viewModel: PosViewModel
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Order Items")) {
                    ForEach(viewModel.cart) { item in
                        HStack {
                            Text(item.product.name)
                            Spacer()
                            Text("\(item.quantity)x \(String(format: "$%.2f", item.product.price))")
                                .bold()
                        }
                    }
                }
                
                Section(header: Text("Payment Total")) {
                    HStack {
                        Text("Grand Total")
                            .bold()
                        Spacer()
                        Text(String(format: "$%.2f", viewModel.totalAmount))
                            .font(.title3)
                            .bold()
                            .foregroundColor(.blue)
                    }
                }
            }
            .navigationTitle("POS Checkout")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Complete Sale") {
                        viewModel.clearCart()
                        dismiss()
                    }
                    .bold()
                }
            }
        }
    }
}
